package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.Converter.BookingConverter;
import com.example.hotel_management_project.DTO.CustomException;
import com.example.hotel_management_project.DTO.Request.BookingRequest;
import com.example.hotel_management_project.DTO.Request.CheckInRequest;
import com.example.hotel_management_project.DTO.Response.BookingResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.Models.Entity.Booking;
import com.example.hotel_management_project.Models.Entity.Customer;
import com.example.hotel_management_project.Models.Entity.Room;
import com.example.hotel_management_project.Models.Enums.BookingStatus;
import com.example.hotel_management_project.Models.Enums.ErrorCode;
import com.example.hotel_management_project.Models.Enums.RoomStatus;
import com.example.hotel_management_project.Repository.BookingRepo;
import com.example.hotel_management_project.Repository.CustomerRepo;
import com.example.hotel_management_project.Repository.RoomRepo;
import com.example.hotel_management_project.Services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepo bookingRepo;
    private final CustomerRepo customerRepo;
    private final RoomRepo roomRepo;
    private final BookingConverter bookingConverter;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOMER_NOT_FOUND));

        Room room = roomRepo.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // Phòng đang bảo trì thì không được phép đặt hoặc nhận phòng
        if (room.getStatus() == RoomStatus.DANG_BAO_TRI) {
            throw new CustomException(ErrorCode.ROOM_UNDER_MAINTENANCE);
        }

        // Kiểm tra số lượng người ở không vượt quá số người tối đa của loại phòng
        Integer numGuests = request.getNumberOfGuests() != null ? request.getNumberOfGuests() : 1;
        if (room.getRoomType() != null && room.getRoomType().getMaxOccupancy() != null) {
            if (numGuests > room.getRoomType().getMaxOccupancy()) {
                throw new CustomException(ErrorCode.EXCEED_MAX_GUESTS);
            }
        }

        // Nếu nhận phòng ngay (DANG_O), kiểm tra điều kiện phòng
        if (request.getStatus() == BookingStatus.DANG_O) {
            if (room.getStatus() == RoomStatus.DANG_O) {
                throw new CustomException(ErrorCode.ROOM_ALREADY_OCCUPIED);
            }
            if (room.getStatus() != RoomStatus.TRONG) {
                throw new CustomException(ErrorCode.ROOM_NOT_READY);
            }
            room.setStatus(RoomStatus.DANG_O);
            if (request.getActualCheckInTime() == null) {
                request.setActualCheckInTime(LocalDateTime.now());
            }
            roomRepo.save(room);
        }

        Booking booking = bookingConverter.toBookingEntity(request, customer, room);
        Booking saved = bookingRepo.save(booking);
        return bookingConverter.toBookingResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepo.findById(request.getCustomerId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CUSTOMER_NOT_FOUND));
        }

        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepo.findById(request.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
            // Nếu đổi sang phòng đang bảo trì
            if (room.getStatus() == RoomStatus.DANG_BAO_TRI) {
                throw new CustomException(ErrorCode.ROOM_UNDER_MAINTENANCE);
            }
        }

        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = request.getStatus();

        // Kiểm tra số lượng người ở không vượt quá số người tối đa của loại phòng
        Room checkRoom = room != null ? room : booking.getRoom();
        Integer numGuests = request.getNumberOfGuests() != null ? request.getNumberOfGuests() : booking.getNumberOfGuests();
        if (checkRoom != null && checkRoom.getRoomType() != null && checkRoom.getRoomType().getMaxOccupancy() != null) {
            if (numGuests != null && numGuests > checkRoom.getRoomType().getMaxOccupancy()) {
                throw new CustomException(ErrorCode.EXCEED_MAX_GUESTS);
            }
        }

        // Nếu cập nhật sang trạng thái Đang ở (check-in)
        if (newStatus == BookingStatus.DANG_O && oldStatus != BookingStatus.DANG_O) {
            Room target = room != null ? room : booking.getRoom();
            if (target != null) {
                if (target.getStatus() == RoomStatus.DANG_BAO_TRI) {
                    throw new CustomException(ErrorCode.ROOM_UNDER_MAINTENANCE);
                }
                if (target.getStatus() == RoomStatus.DANG_O) {
                    throw new CustomException(ErrorCode.ROOM_ALREADY_OCCUPIED);
                }
                if (target.getStatus() != RoomStatus.TRONG) {
                    throw new CustomException(ErrorCode.ROOM_NOT_READY);
                }
            }
        }

        bookingConverter.updateEntity(request, booking, customer, room);

        if (newStatus != null && newStatus != oldStatus) {
            Room targetRoom = booking.getRoom();
            if (targetRoom != null) {
                if (newStatus == BookingStatus.DANG_O) {
                    targetRoom.setStatus(RoomStatus.DANG_O);
                    if (booking.getActualCheckInTime() == null) {
                        booking.setActualCheckInTime(LocalDateTime.now());
                    }
                    roomRepo.save(targetRoom);
                } else if (newStatus == BookingStatus.DA_TRA) {
                    targetRoom.setStatus(RoomStatus.DANG_DON);
                    if (booking.getActualCheckOutTime() == null) {
                        booking.setActualCheckOutTime(LocalDateTime.now());
                    }
                    roomRepo.save(targetRoom);
                } else if (newStatus == BookingStatus.DA_HUY) {
                    if (targetRoom.getStatus() == RoomStatus.DANG_O) {
                        targetRoom.setStatus(RoomStatus.TRONG);
                        roomRepo.save(targetRoom);
                    }
                }
            }
        }

        Booking saved = bookingRepo.save(booking);
        return bookingConverter.toBookingResponse(saved);
    }

    @Override
    public PageResponse<BookingResponse> getAllBookings(Pageable pageable) {
        Page<Booking> page = bookingRepo.findAllWithDetails(pageable);
        List<BookingResponse> content = page.getContent().stream()
                .map(bookingConverter::toBookingResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookingResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public PageResponse<BookingResponse> searchBookings(String keyword, String statusStr, Pageable pageable) {
        return searchBookings(keyword, statusStr, null, null, pageable);
    }

    @Override
    public PageResponse<BookingResponse> searchBookings(String keyword, String statusStr, Long roomTypeId, Long roomId, Pageable pageable) {
        BookingStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"Tất cả trạng thái".equalsIgnoreCase(statusStr.trim())) {
            try {
                status = BookingStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                try {
                    status = BookingStatus.fromLabel(statusStr.trim());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Booking> page = bookingRepo.searchBookings(kw, status, roomTypeId, roomId, pageable);
        List<BookingResponse> content = page.getContent().stream()
                .map(bookingConverter::toBookingResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookingResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));
        return bookingConverter.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse checkIn(CheckInRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new CustomException(ErrorCode.BOOKING_NOT_FOUND);
        }
        Booking booking = bookingRepo.findById(request.getBookingId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));

        Room room = booking.getRoom();
        if (room != null) {
            if (room.getStatus() == RoomStatus.DANG_BAO_TRI) {
                throw new CustomException(ErrorCode.ROOM_UNDER_MAINTENANCE);
            }
            if (room.getStatus() == RoomStatus.DANG_O && booking.getStatus() != BookingStatus.DANG_O) {
                throw new CustomException(ErrorCode.ROOM_ALREADY_OCCUPIED);
            }
            if (room.getStatus() != RoomStatus.TRONG && room.getStatus() != RoomStatus.DANG_O) {
                throw new CustomException(ErrorCode.ROOM_NOT_READY);
            }
            room.setStatus(RoomStatus.DANG_O);
            roomRepo.save(room);
        }

        booking.setStatus(BookingStatus.DANG_O);

        if (request.getActualCheckInTime() != null) {
            booking.setActualCheckInTime(request.getActualCheckInTime());
        } else if (booking.getActualCheckInTime() == null) {
            booking.setActualCheckInTime(LocalDateTime.now());
        }

        LocalDate newOutDate = request.getActualCheckOutDate() != null
                ? request.getActualCheckOutDate()
                : request.getExpectedCheckOutDate();
        if (newOutDate != null) {
            booking.setExpectedCheckOutDate(newOutDate);
        }

        if (request.getNote() != null && !request.getNote().isBlank()) {
            booking.setNote(request.getNote());
        }

        Booking saved = bookingRepo.save(booking);
        return bookingConverter.toBookingResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse checkIn(Long id) {
        return checkIn(CheckInRequest.builder()
                .bookingId(id)
                .actualCheckInTime(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public BookingResponse checkOut(Long id) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));

        booking.setStatus(BookingStatus.DA_TRA);
        booking.setActualCheckOutTime(LocalDateTime.now());

        if (booking.getRoom() != null) {
            booking.getRoom().setStatus(RoomStatus.DANG_DON);
            roomRepo.save(booking.getRoom());
        }

        Booking saved = bookingRepo.save(booking);
        return bookingConverter.toBookingResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));

        booking.setStatus(BookingStatus.DA_HUY);

        if (booking.getRoom() != null && booking.getRoom().getStatus() == RoomStatus.DANG_O) {
            booking.getRoom().setStatus(RoomStatus.TRONG);
            roomRepo.save(booking.getRoom());
        }

        Booking saved = bookingRepo.save(booking);
        return bookingConverter.toBookingResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));
        bookingRepo.delete(booking);
    }
}

package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.Models.Entity.Booking;
import com.example.hotel_management_project.Models.Entity.Promotion;
import com.example.hotel_management_project.Models.Entity.Room;
import com.example.hotel_management_project.Models.Enums.BookingStatus;
import com.example.hotel_management_project.Models.Enums.RoomStatus;
import com.example.hotel_management_project.Repository.BookingRepo;
import com.example.hotel_management_project.Repository.PromotionRepo;
import com.example.hotel_management_project.Repository.RoomRepo;
import com.example.hotel_management_project.Services.HotelAutomationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class HotelAutomationServiceImpl implements HotelAutomationService {

    private final BookingRepo bookingRepo;
    private final RoomRepo roomRepo;
    private final PromotionRepo promotionRepo;

    public HotelAutomationServiceImpl(BookingRepo bookingRepo, RoomRepo roomRepo, PromotionRepo promotionRepo) {
        this.bookingRepo = bookingRepo;
        this.roomRepo = roomRepo;
        this.promotionRepo = promotionRepo;
    }

    /**
     * Tác vụ chạy ngầm định kỳ mỗi 60 giây (1 phút)
     */
    @Scheduled(fixedRate = 60000)
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runAutomations() {
        try {
            autoCancelNoShowBookings();
            autoCheckoutAndCleanRooms();
            autoExpirePromotions();
        } catch (Exception e) {
            log.error("Lỗi trong quá trình chạy tác vụ tự động hóa khách sạn: {}", e.getMessage(), e);
        }
    }

    /**
     * 1. Hủy đơn đặt phòng sau 5 tiếng nếu khách không đến nhận phòng:
     *    - Đơn đặt phòng có trạng thái DAT_TRUOC (Đặt trước) và chưa check-in thực tế.
     *    - Giờ nhận phòng tiêu chuẩn là 14:00 ngày checkInDate (hoặc createdAt nếu tạo sau 14:00 trong ngày).
     *    - Sau 5 tiếng không đến: chuyển trạng thái đặt phòng sang DA_HUY, giải phóng phòng về TRONG.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int autoCancelNoShowBookings() {
        List<Booking> bookedList = bookingRepo.findByStatusWithDetails(BookingStatus.DAT_TRUOC);
        if (bookedList.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int count = 0;

        for (Booking booking : bookedList) {
            if (booking.getActualCheckInTime() != null) {
                continue;
            }

            LocalDate checkInDate = booking.getCheckInDate();
            if (checkInDate == null) {
                continue;
            }

            // Mốc nhận phòng tiêu chuẩn khách sạn: 14:00
            LocalDateTime standardCheckInTime = checkInDate.atTime(14, 0);
            LocalDateTime baseTime = standardCheckInTime;

            // Nếu đơn được tạo trong ngày nhận phòng sau 14:00, tính hạn 5 tiếng từ lúc tạo
            if (booking.getCreatedAt() != null
                    && booking.getCreatedAt().toLocalDate().isEqual(checkInDate)
                    && booking.getCreatedAt().isAfter(standardCheckInTime)) {
                baseTime = booking.getCreatedAt();
            }

            LocalDateTime cancelDeadline = baseTime.plusHours(5);

            // Đã quá 5 tiếng nhận phòng hoặc ngày nhận phòng đã trong quá khứ
            if (now.isAfter(cancelDeadline) || checkInDate.isBefore(today)) {
                booking.setStatus(BookingStatus.DA_HUY);

                String cancelNote = "[Hệ thống] Tự động hủy đơn do khách không đến nhận phòng sau 5 tiếng quy định.";
                if (booking.getNote() != null && !booking.getNote().isBlank()) {
                    if (!booking.getNote().contains("[Hệ thống]")) {
                        booking.setNote(booking.getNote() + " | " + cancelNote);
                    }
                } else {
                    booking.setNote(cancelNote);
                }

                Room room = booking.getRoom();
                if (room != null) {
                    List<Booking> activeList = bookingRepo.findByRoomIdAndStatusIn(
                            room.getId(), List.of(BookingStatus.DANG_O));
                    if (activeList.isEmpty() && room.getStatus() != RoomStatus.DANG_BAO_TRI) {
                        room.setStatus(RoomStatus.TRONG);
                        roomRepo.save(room);
                    }
                }

                bookingRepo.save(booking);
                count++;
                log.info("Tự động hủy đơn đặt phòng ID #{} vì quá hạn 5 tiếng không đến.", booking.getId());
            }
        }
        return count;
    }

    /**
     * 2. Đến ngày trả phòng thực tế / dự kiến, hoặc trạng thái đặt phòng là DA_TRA (Đã trả phòng):
     *    - Đơn đặt phòng chuyển sang DA_TRA.
     *    - Trạng thái phòng chuyển sang DANG_DON (Đang dọn) để dọn dẹp vệ sinh.
     *    - Đảm bảo nếu đơn đặt phòng đã trả thì phòng không bao giờ bị kẹt ở trạng thái Đang ở.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int autoCheckoutAndCleanRooms() {
        int count = 0;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Bước 1: Quét các đơn đặt phòng DANG_O đã đến ngày trả thực tế hoặc ngày trả dự kiến
        List<Booking> stayingList = bookingRepo.findByStatusWithDetails(BookingStatus.DANG_O);
        for (Booking booking : stayingList) {
            LocalDate expectedCheckOut = booking.getExpectedCheckOutDate();
            LocalDateTime actualCheckOutTime = booking.getActualCheckOutTime();

            boolean isDueActual = actualCheckOutTime != null && !now.isBefore(actualCheckOutTime);
            boolean isDueExpected = expectedCheckOut != null && !today.isBefore(expectedCheckOut);

            if (isDueActual || isDueExpected) {
                booking.setStatus(BookingStatus.DA_TRA);
                if (booking.getActualCheckOutTime() == null) {
                    booking.setActualCheckOutTime(now);
                }

                String checkoutNote = "[Hệ thống] Đã đến ngày trả phòng (" 
                        + (actualCheckOutTime != null ? actualCheckOutTime.toLocalDate() : expectedCheckOut) 
                        + "), tự động hoàn tất lưu trú và chuyển trạng thái phòng sang Đang dọn.";
                if (booking.getNote() != null && !booking.getNote().isBlank()) {
                    if (!booking.getNote().contains("[Hệ thống]")) {
                        booking.setNote(booking.getNote() + " | " + checkoutNote);
                    }
                } else {
                    booking.setNote(checkoutNote);
                }

                Room room = booking.getRoom();
                if (room != null && room.getStatus() != RoomStatus.DANG_BAO_TRI) {
                    room.setStatus(RoomStatus.DANG_DON);
                    roomRepo.save(room);
                }

                bookingRepo.save(booking);
                count++;
                log.info("Tự động trả phòng đơn đặt ID #{} và chuyển phòng {} sang trạng thái Đang dọn.",
                        booking.getId(), room != null ? room.getRoomNumber() : "N/A");
            }
        }

        // Bước 2: Đồng bộ phòng cho các đơn đặt phòng đã trả (DA_TRA):
        // Nếu đặt phòng là Đã trả phòng mà phòng vẫn đang ở trạng thái 'Đang ở', chuyển ngay sang 'Đang dọn'
        List<Booking> checkedOutList = bookingRepo.findByStatusWithDetails(BookingStatus.DA_TRA);
        for (Booking b : checkedOutList) {
            Room r = b.getRoom();
            if (r != null && r.getStatus() == RoomStatus.DANG_O) {
                List<Booking> activeList = bookingRepo.findByRoomIdAndStatusIn(
                        r.getId(), List.of(BookingStatus.DANG_O));
                if (activeList.isEmpty() && r.getStatus() != RoomStatus.DANG_BAO_TRI) {
                    r.setStatus(RoomStatus.DANG_DON);
                    roomRepo.save(r);
                    log.info("Đồng bộ phòng {}: đơn đặt phòng đã trả -> chuyển trạng thái phòng sang Đang dọn.",
                            r.getRoomNumber());
                }
            }
        }

        // Bước 3: Đảm bảo phòng đang có khách ở (DANG_O) thì trạng thái phòng phải là DANG_O
        List<Booking> currentStaying = bookingRepo.findByStatusWithDetails(BookingStatus.DANG_O);
        for (Booking b : currentStaying) {
            Room r = b.getRoom();
            if (r != null && r.getStatus() != RoomStatus.DANG_O && r.getStatus() != RoomStatus.DANG_BAO_TRI) {
                r.setStatus(RoomStatus.DANG_O);
                roomRepo.save(r);
                log.info("Đồng bộ phòng {}: đơn đặt phòng đang lưu trú -> chuyển trạng thái phòng sang Đang ở.",
                        r.getRoomNumber());
            }
        }

        return count;
    }

    /**
     * 3. Khuyến mãi quá hạn:
     *    Tự động chuyển trạng thái sang Đã hết hạn (active = false) trong CSDL khi endDate < ngày hiện tại.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int autoExpirePromotions() {
        List<Promotion> promotions = promotionRepo.findAll();
        if (promotions.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        int count = 0;

        for (Promotion p : promotions) {
            if (p.getEndDate() != null && p.getEndDate().isBefore(today)) {
                if (Boolean.TRUE.equals(p.getActive())) {
                    p.setActive(false);
                    promotionRepo.save(p);
                    count++;
                    log.info("Tự động chuyển khuyến mãi '{}' ({}) sang Đã hết hạn (ngày kết thúc: {}).",
                            p.getName(), p.getCode(), p.getEndDate());
                }
            }
        }
        return count;
    }
}

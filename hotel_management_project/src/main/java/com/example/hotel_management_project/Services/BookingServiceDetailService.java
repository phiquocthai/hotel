package com.example.hotel_management_project.Services;

import com.example.hotel_management_project.DTO.Request.BookingServiceDetailRequest;
import com.example.hotel_management_project.DTO.Response.BookingServiceDetailResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface BookingServiceDetailService {
    BookingServiceDetailResponse createDetail(BookingServiceDetailRequest request);
    BookingServiceDetailResponse updateDetail(Long id, BookingServiceDetailRequest request);
    BookingServiceDetailResponse getDetailById(Long id);
    List<BookingServiceDetailResponse> getDetailsByBookingId(Long bookingId);
    PageResponse<BookingServiceDetailResponse> getAllDetails(Long bookingId, Pageable pageable);
    PageResponse<BookingServiceDetailResponse> searchDetails(
            String keyword, Long bookingId, Long roomId, Long serviceId,
            BigDecimal minTotal, BigDecimal maxTotal, Pageable pageable);
    void deleteDetail(Long id);
}

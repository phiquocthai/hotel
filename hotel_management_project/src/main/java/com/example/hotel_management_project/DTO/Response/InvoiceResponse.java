package com.example.hotel_management_project.DTO.Response;

import com.example.hotel_management_project.Models.Enums.DiscountType;
import com.example.hotel_management_project.Models.Enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private String code;

    // Thông tin đặt phòng & khách hàng
    private Long bookingId;
    private String bookingCode;
    private Long staffId;
    private String staffName;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerCitizenId;

    // Thông tin phòng
    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private BigDecimal roomBasePrice;

    // Thời gian lưu trú
    private LocalDate checkInDate;
    private LocalDate expectedCheckOutDate;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;
    private Long stayNights;

    // Khuyến mãi
    private Long promotionId;
    private String promotionCode;
    private String promotionName;
    private DiscountType discountType;
    private BigDecimal discountValue;

    // Chi tiết tài chính
    private BigDecimal roomAmount;
    private BigDecimal serviceAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    // Trạng thái thanh toán
    private PaymentStatus paymentStatus;
    private String paymentStatusLabel;

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    @Builder.Default
    private List<PaymentResponse> payments = new ArrayList<>();

    @Builder.Default
    private List<BookingServiceDetailResponse> services = new ArrayList<>();
}

package com.example.hotel_management_project.DTO.Request;

import com.example.hotel_management_project.Models.Enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    @NotNull(message = "Mã đặt phòng không được để trống")
    private Long bookingId;

    private Long staffId;

    private Long promotionId;

    private String promotionCode;

    private BigDecimal roomAmount;

    private BigDecimal serviceAmount;

    private BigDecimal penaltyAmount;

    private BigDecimal discountAmount;

    private BigDecimal totalAmount;

    private PaymentStatus paymentStatus;

    // Các trường thanh toán nếu thực hiện thanh toán luôn khi tạo hóa đơn
    private Long paymentMethodId;

    private BigDecimal paymentAmount;

    private String transactionCode;

    // Tùy chọn tự động chuyển trạng thái phòng sang ĐANG DỌN và đơn đặt sang ĐÃ TRẢ
    private Boolean autoCheckOut;
}

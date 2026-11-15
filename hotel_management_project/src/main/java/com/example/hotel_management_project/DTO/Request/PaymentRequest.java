package com.example.hotel_management_project.DTO.Request;

import com.example.hotel_management_project.Models.Enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    private Long invoiceId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private Long paymentMethodId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    private BigDecimal amount;

    private String transactionCode;

    private TransactionStatus status;

    private LocalDateTime paymentTime;

    private String note;

    private Long staffId;
}

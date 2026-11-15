package com.example.hotel_management_project.DTO.Response;

import com.example.hotel_management_project.Models.Enums.TransactionStatus;
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
public class PaymentResponse {

    private Long id;
    private Long invoiceId;
    private Long paymentMethodId;
    private String paymentMethodName;
    private BigDecimal amount;
    private String transactionCode;
    private TransactionStatus status;
    private String statusLabel;
    private LocalDateTime paymentTime;
    private String note;
    private Long staffId;
    private String staffName;
}

package com.example.hotel_management_project.Converter;

import com.example.hotel_management_project.DTO.Request.PaymentRequest;
import com.example.hotel_management_project.DTO.Response.PaymentResponse;
import com.example.hotel_management_project.Models.Entity.Invoice;
import com.example.hotel_management_project.Models.Entity.Payment;
import com.example.hotel_management_project.Models.Entity.PaymentMethod;
import com.example.hotel_management_project.Models.Enums.TransactionStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentConverter {

    public Payment toEntity(PaymentRequest request, Invoice invoice, PaymentMethod paymentMethod) {
        return Payment.builder()
                .invoice(invoice)
                .paymentMethod(paymentMethod)
                .amount(request.getAmount())
                .transactionCode(request.getTransactionCode())
                .status(request.getStatus() != null ? request.getStatus() : TransactionStatus.THANH_CONG)
                .paymentTime(request.getPaymentTime() != null ? request.getPaymentTime() : LocalDateTime.now())
                .build();
    }

    public PaymentResponse toResponse(Payment entity) {
        PaymentResponse response = PaymentResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .transactionCode(entity.getTransactionCode())
                .status(entity.getStatus())
                .statusLabel(entity.getStatus() != null ? entity.getStatus().getLabel() : "")
                .paymentTime(entity.getPaymentTime())
                .build();

        if (entity.getInvoice() != null) {
            response.setInvoiceId(entity.getInvoice().getId());
        }

        if (entity.getPaymentMethod() != null) {
            response.setPaymentMethodId(entity.getPaymentMethod().getId());
            response.setPaymentMethodName(entity.getPaymentMethod().getName());
        }

        return response;
    }
}

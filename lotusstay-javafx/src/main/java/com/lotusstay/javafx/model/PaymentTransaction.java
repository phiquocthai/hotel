package com.lotusstay.javafx.model;

import java.math.BigDecimal;

public class PaymentTransaction {
    public Long id;
    public Long invoiceId;
    public Long paymentMethodId;
    public String paymentMethodName;
    public BigDecimal amount = BigDecimal.ZERO;
    public String transactionCode;
    public String status;
    public String statusLabel;
    public String paymentTime;
    public String note;
    public Long staffId;
    public String staffName;

    public String statusBadgeClass() {
        if ("THANH_CONG".equalsIgnoreCase(status)) return "badge-trong";
        if ("DANG_XU_LY".equalsIgnoreCase(status)) return "badge-dang_o";
        return "badge-bao_tri";
    }
}

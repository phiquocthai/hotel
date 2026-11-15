package com.lotusstay.javafx.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    public Long id;
    public String code;          // HD001, HD002...
    public Booking booking;      // ManyToOne
    public Customer customer;    // ManyToOne
    public Room room;            // ManyToOne
    public Long staffId;
    public String staffName;
    public Long stayNights = 1L;

    public BigDecimal roomFee = BigDecimal.ZERO;
    public BigDecimal serviceFee = BigDecimal.ZERO;
    public BigDecimal penaltyFee = BigDecimal.ZERO;
    public BigDecimal discountFee = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
    public BigDecimal paidAmount = BigDecimal.ZERO;
    public BigDecimal remainingAmount = BigDecimal.ZERO;

    public String promotionCode;
    public String promotionName;
    public String discountType;
    public BigDecimal discountValue;

    public String paymentStatus = "CHUA_THANH_TOAN"; // CHUA_THANH_TOAN, THANH_TOAN_MOT_PHAN, DA_THANH_TOAN, DA_HUY
    public String issuedDate;
    public String createdAt;
    public String modifiedAt;

    public List<PaymentTransaction> payments = new ArrayList<>();
    public List<BookingServiceDetail> services = new ArrayList<>();

    public String paymentStatusLabel() {
        if ("DA_THANH_TOAN".equalsIgnoreCase(paymentStatus)) return "Đã thanh toán";
        if ("THANH_TOAN_MOT_PHAN".equalsIgnoreCase(paymentStatus)) return "Thanh toán một phần";
        if ("DA_HUY".equalsIgnoreCase(paymentStatus)) return "Đã hủy";
        return "Chưa thanh toán";
    }

    public String badgeClass() {
        if ("DA_THANH_TOAN".equalsIgnoreCase(paymentStatus)) return "badge-trong";
        if ("THANH_TOAN_MOT_PHAN".equalsIgnoreCase(paymentStatus)) return "badge-dang_o";
        if ("DA_HUY".equalsIgnoreCase(paymentStatus)) return "badge-bao_tri";
        return "badge-dang_don";
    }

    public boolean isFullyPaid() {
        if ("DA_HUY".equalsIgnoreCase(paymentStatus)) return false;
        return "DA_THANH_TOAN".equalsIgnoreCase(paymentStatus)
                || (total != null && paidAmount != null && paidAmount.compareTo(total) >= 0);
    }
}

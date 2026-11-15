package com.example.hotel_management_project.Converter;

import com.example.hotel_management_project.DTO.Request.InvoiceRequest;
import com.example.hotel_management_project.DTO.Response.BookingServiceDetailResponse;
import com.example.hotel_management_project.DTO.Response.InvoiceResponse;
import com.example.hotel_management_project.DTO.Response.PaymentResponse;
import com.example.hotel_management_project.Models.Entity.Booking;
import com.example.hotel_management_project.Models.Entity.Invoice;
import com.example.hotel_management_project.Models.Entity.Payment;
import com.example.hotel_management_project.Models.Entity.Promotion;
import com.example.hotel_management_project.Models.Entity.Staff;
import com.example.hotel_management_project.Models.Enums.PaymentStatus;
import com.example.hotel_management_project.Models.Enums.TransactionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceConverter {

    private final PaymentConverter paymentConverter;
    private final BookingServiceDetailConverter bookingServiceDetailConverter;

    public InvoiceConverter(PaymentConverter paymentConverter, BookingServiceDetailConverter bookingServiceDetailConverter) {
        this.paymentConverter = paymentConverter;
        this.bookingServiceDetailConverter = bookingServiceDetailConverter;
    }

    public Invoice toEntity(InvoiceRequest request, Booking booking, Promotion promotion) {
        return toEntity(request, booking, promotion, null);
    }

    public Invoice toEntity(InvoiceRequest request, Booking booking, Promotion promotion, Staff staff) {
        BigDecimal roomAmt = request.getRoomAmount() != null ? request.getRoomAmount() : BigDecimal.ZERO;
        BigDecimal svcAmt = request.getServiceAmount() != null ? request.getServiceAmount() : BigDecimal.ZERO;
        BigDecimal penaltyAmt = request.getPenaltyAmount() != null ? request.getPenaltyAmount() : BigDecimal.ZERO;
        BigDecimal discountAmt = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalAmt = request.getTotalAmount() != null ? request.getTotalAmount()
                : roomAmt.add(svcAmt).add(penaltyAmt).subtract(discountAmt).max(BigDecimal.ZERO);

        PaymentStatus status = request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.CHUA_THANH_TOAN;

        return Invoice.builder()
                .booking(booking)
                .staff(staff)
                .promotion(promotion)
                .roomAmount(roomAmt)
                .serviceAmount(svcAmt)
                .penaltyAmount(penaltyAmt)
                .discountAmount(discountAmt)
                .totalAmount(totalAmt)
                .paymentStatus(status)
                .build();
    }

    public InvoiceResponse toResponse(Invoice entity) {
        return toResponse(entity, true);
    }

    public InvoiceResponse toResponse(Invoice entity, boolean includeDetails) {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(entity.getId())
                .code(entity.getId() != null ? String.format("HD%03d", entity.getId()) : "HD000")
                .roomAmount(entity.getRoomAmount() != null ? entity.getRoomAmount() : BigDecimal.ZERO)
                .serviceAmount(entity.getServiceAmount() != null ? entity.getServiceAmount() : BigDecimal.ZERO)
                .penaltyAmount(entity.getPenaltyAmount() != null ? entity.getPenaltyAmount() : BigDecimal.ZERO)
                .discountAmount(entity.getDiscountAmount() != null ? entity.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO)
                .paymentStatus(entity.getPaymentStatus())
                .paymentStatusLabel(entity.getPaymentStatus() != null ? entity.getPaymentStatus().getLabel() : "")
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .build();

        // Thông tin nhân viên lập hóa đơn
        Staff staff = entity.getStaff();
        if (staff != null) {
            response.setStaffId(staff.getId());
            response.setStaffName(staff.getFullName());
        }

        // Thông tin Đặt phòng & Khách hàng & Phòng
        Booking b = entity.getBooking();
        if (b != null) {
            response.setBookingId(b.getId());
            response.setBookingCode(String.format("DP%03d", b.getId()));
            response.setCheckInDate(b.getCheckInDate());
            response.setExpectedCheckOutDate(b.getExpectedCheckOutDate());
            response.setActualCheckInTime(b.getActualCheckInTime());
            response.setActualCheckOutTime(b.getActualCheckOutTime());

            if (b.getCheckInDate() != null && b.getExpectedCheckOutDate() != null) {
                long nights = ChronoUnit.DAYS.between(b.getCheckInDate(), b.getExpectedCheckOutDate());
                response.setStayNights(Math.max(1, nights));
            } else {
                response.setStayNights(1L);
            }

            if (b.getCustomer() != null) {
                response.setCustomerId(b.getCustomer().getId());
                response.setCustomerName(b.getCustomer().getFullName());
                response.setCustomerPhone(b.getCustomer().getPhoneNumber());
                response.setCustomerCitizenId(b.getCustomer().getCitizenId());
            }

            if (b.getRoom() != null) {
                response.setRoomId(b.getRoom().getId());
                response.setRoomNumber(b.getRoom().getRoomNumber());
                if (b.getRoom().getRoomType() != null) {
                    response.setRoomTypeName(b.getRoom().getRoomType().getName());
                    response.setRoomBasePrice(b.getRoom().getRoomType().getBasePrice());
                }
            }

            if (includeDetails && b.getServiceDetails() != null && !b.getServiceDetails().isEmpty()) {
                List<BookingServiceDetailResponse> svcResponses = b.getServiceDetails().stream()
                        .map(bookingServiceDetailConverter::toResponse)
                        .collect(Collectors.toList());
                response.setServices(svcResponses);
            }
        }

        // Khuyến mãi
        Promotion p = entity.getPromotion();
        if (p != null) {
            response.setPromotionId(p.getId());
            response.setPromotionCode(p.getCode());
            response.setPromotionName(p.getName());
            response.setDiscountType(p.getDiscountType());
            response.setDiscountValue(p.getDiscountValue());
        }

        // Thanh toán & Tiền đã trả / còn lại
        BigDecimal paid = BigDecimal.ZERO;
        List<PaymentResponse> paymentResponses = new ArrayList<>();
        if (entity.getPayments() != null) {
            for (Payment pay : entity.getPayments()) {
                paymentResponses.add(paymentConverter.toResponse(pay));
                if (pay.getStatus() == TransactionStatus.THANH_CONG && pay.getAmount() != null) {
                    paid = paid.add(pay.getAmount());
                }
            }
        }
        response.setPayments(paymentResponses);
        response.setPaidAmount(paid);

        BigDecimal total = response.getTotalAmount() != null ? response.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
        response.setRemainingAmount(remaining);

        return response;
    }

    public void updateEntity(InvoiceRequest request, Invoice entity, Promotion promotion) {
        updateEntity(request, entity, promotion, null);
    }

    public void updateEntity(InvoiceRequest request, Invoice entity, Promotion promotion, Staff staff) {
        if (staff != null) {
            entity.setStaff(staff);
        }
        if (promotion != null) {
            entity.setPromotion(promotion);
        }
        if (request.getRoomAmount() != null) {
            entity.setRoomAmount(request.getRoomAmount());
        }
        if (request.getServiceAmount() != null) {
            entity.setServiceAmount(request.getServiceAmount());
        }
        if (request.getPenaltyAmount() != null) {
            entity.setPenaltyAmount(request.getPenaltyAmount());
        }
        if (request.getDiscountAmount() != null) {
            entity.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getTotalAmount() != null) {
            entity.setTotalAmount(request.getTotalAmount());
        } else {
            BigDecimal roomAmt = entity.getRoomAmount() != null ? entity.getRoomAmount() : BigDecimal.ZERO;
            BigDecimal svcAmt = entity.getServiceAmount() != null ? entity.getServiceAmount() : BigDecimal.ZERO;
            BigDecimal penAmt = entity.getPenaltyAmount() != null ? entity.getPenaltyAmount() : BigDecimal.ZERO;
            BigDecimal discAmt = entity.getDiscountAmount() != null ? entity.getDiscountAmount() : BigDecimal.ZERO;
            entity.setTotalAmount(roomAmt.add(svcAmt).add(penAmt).subtract(discAmt).max(BigDecimal.ZERO));
        }
        if (request.getPaymentStatus() != null) {
            entity.setPaymentStatus(request.getPaymentStatus());
        }
    }
}

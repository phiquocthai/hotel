package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.Converter.InvoiceConverter;
import com.example.hotel_management_project.Converter.PaymentConverter;
import com.example.hotel_management_project.DTO.Request.InvoiceRequest;
import com.example.hotel_management_project.DTO.Request.PaymentRequest;
import com.example.hotel_management_project.DTO.Response.InvoiceResponse;
import com.example.hotel_management_project.DTO.Response.PageResponse;
import com.example.hotel_management_project.DTO.Response.PaymentResponse;
import com.example.hotel_management_project.DTO.CustomException;
import com.example.hotel_management_project.Models.Entity.*;
import com.example.hotel_management_project.Models.Entity.PaymentMethod;
import com.example.hotel_management_project.Models.Enums.*;
import com.example.hotel_management_project.Repository.*;
import com.example.hotel_management_project.Services.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepo invoiceRepo;
    private final BookingRepo bookingRepo;
    private final RoomRepo roomRepo;
    private final PromotionRepo promotionRepo;
    private final PaymentMethodRepo paymentMethodRepo;
    private final PaymentRepo paymentRepo;
    private final BookingServiceDetailRepo bookingServiceDetailRepo;
    private final StaffRepo staffRepo;
    private final AuthRepo authRepo;
    private final InvoiceConverter invoiceConverter;
    private final PaymentConverter paymentConverter;

    public InvoiceServiceImpl(InvoiceRepo invoiceRepo,
                              BookingRepo bookingRepo,
                              RoomRepo roomRepo,
                              PromotionRepo promotionRepo,
                              PaymentMethodRepo paymentMethodRepo,
                              PaymentRepo paymentRepo,
                              BookingServiceDetailRepo bookingServiceDetailRepo,
                              StaffRepo staffRepo,
                              AuthRepo authRepo,
                              InvoiceConverter invoiceConverter,
                              PaymentConverter paymentConverter) {
        this.invoiceRepo = invoiceRepo;
        this.bookingRepo = bookingRepo;
        this.roomRepo = roomRepo;
        this.promotionRepo = promotionRepo;
        this.paymentMethodRepo = paymentMethodRepo;
        this.paymentRepo = paymentRepo;
        this.bookingServiceDetailRepo = bookingServiceDetailRepo;
        this.staffRepo = staffRepo;
        this.authRepo = authRepo;
        this.invoiceConverter = invoiceConverter;
        this.paymentConverter = paymentConverter;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        return processInvoiceCreation(request, Boolean.TRUE.equals(request.getAutoCheckOut()));
    }

    @Override
    @Transactional
    public InvoiceResponse checkout(InvoiceRequest request) {
        // Luôn tự động chuyển trạng thái trả phòng và dọn phòng khi gọi checkout
        return processInvoiceCreation(request, true);
    }

    private InvoiceResponse processInvoiceCreation(InvoiceRequest request, boolean performCheckOut) {
        Booking booking = bookingRepo.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new CustomException(ErrorCode.BOOKING_NOT_FOUND));

        Promotion promotion = null;
        if (request.getPromotionId() != null) {
            promotion = promotionRepo.findById(request.getPromotionId()).orElse(null);
        } else if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
            promotion = promotionRepo.findByCode(request.getPromotionCode().trim()).orElse(null);
        }

        // 1. Tính tiền phòng
        BigDecimal roomAmount = request.getRoomAmount();
        if (roomAmount == null) {
            long nights = 1;
            if (booking.getCheckInDate() != null && booking.getExpectedCheckOutDate() != null) {
                nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getExpectedCheckOutDate());
                if (nights < 1) nights = 1;
            }
            BigDecimal basePrice = BigDecimal.ZERO;
            if (booking.getRoom() != null && booking.getRoom().getRoomType() != null
                    && booking.getRoom().getRoomType().getBasePrice() != null) {
                basePrice = booking.getRoom().getRoomType().getBasePrice();
            }
            roomAmount = basePrice.multiply(BigDecimal.valueOf(nights));
        }

        // 2. Tính tiền dịch vụ
        BigDecimal serviceAmount = request.getServiceAmount();
        if (serviceAmount == null) {
            List<BookingServiceDetail> details = bookingServiceDetailRepo.findByBookingId(booking.getId());
            serviceAmount = details.stream()
                    .map(d -> {
                        BigDecimal price = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                        int qty = d.getQuantity() != null ? d.getQuantity() : 0;
                        return price.multiply(BigDecimal.valueOf(qty));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 3. Tiền phạt / phụ thu
        BigDecimal penaltyAmount = request.getPenaltyAmount() != null ? request.getPenaltyAmount() : BigDecimal.ZERO;

        // 4. Giảm giá
        BigDecimal discountAmount = request.getDiscountAmount();
        if (discountAmount == null) {
            if (promotion != null) {
                if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDate.now())) {
                    promotion.setActive(false);
                    promotionRepo.save(promotion);
                    promotion = null;
                }
            }
            if (promotion != null && Boolean.TRUE.equals(promotion.getActive())) {
                if (promotion.getDiscountType() == DiscountType.PHAN_TRAM) {
                    BigDecimal baseForDiscount = roomAmount.add(serviceAmount);
                    discountAmount = baseForDiscount.multiply(promotion.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                } else {
                    discountAmount = promotion.getDiscountValue() != null ? promotion.getDiscountValue() : BigDecimal.ZERO;
                }
            } else {
                discountAmount = BigDecimal.ZERO;
            }
        }
        discountAmount = discountAmount.min(roomAmount.add(serviceAmount).add(penaltyAmount));

        // 5. Tổng tiền & Nhân viên lập hóa đơn
        BigDecimal totalAmount = request.getTotalAmount();
        if (totalAmount == null) {
            totalAmount = roomAmount.add(serviceAmount).add(penaltyAmount).subtract(discountAmount).max(BigDecimal.ZERO);
        }

        Staff staff = resolveStaff(request.getStaffId());

        Invoice invoice = Invoice.builder()
                .booking(booking)
                .staff(staff)
                .promotion(promotion)
                .roomAmount(roomAmount)
                .serviceAmount(serviceAmount)
                .penaltyAmount(penaltyAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.CHUA_THANH_TOAN)
                .payments(new ArrayList<>())
                .build();

        invoice = invoiceRepo.save(invoice);

        // 6. Xử lý thanh toán nếu có phương thức thanh toán
        if (request.getPaymentMethodId() != null) {
            PaymentMethod pm = paymentMethodRepo.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

            BigDecimal payAmount = request.getPaymentAmount() != null ? request.getPaymentAmount() : totalAmount;
            Payment payment = Payment.builder()
                    .invoice(invoice)
                    .paymentMethod(pm)
                    .amount(payAmount)
                    .transactionCode(request.getTransactionCode())
                    .status(TransactionStatus.THANH_CONG)
                    .paymentTime(LocalDateTime.now())
                    .build();

            payment = paymentRepo.save(payment);
            invoice.getPayments().add(payment);

            if (payAmount.compareTo(totalAmount) >= 0) {
                invoice.setPaymentStatus(PaymentStatus.DA_THANH_TOAN);
            } else if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setPaymentStatus(PaymentStatus.THANH_TOAN_MOT_PHAN);
            } else {
                invoice.setPaymentStatus(PaymentStatus.CHUA_THANH_TOAN);
            }
            invoice = invoiceRepo.save(invoice);
        } else if (request.getPaymentStatus() != null) {
            invoice.setPaymentStatus(request.getPaymentStatus());
            invoice = invoiceRepo.save(invoice);
        }

        // 7. Xử lý trả phòng nếu được yêu cầu
        if (performCheckOut) {
            booking.setStatus(BookingStatus.DA_TRA);
            if (booking.getActualCheckOutTime() == null) {
                booking.setActualCheckOutTime(LocalDateTime.now());
            }
            if (booking.getRoom() != null && booking.getRoom().getStatus() != RoomStatus.DANG_BAO_TRI) {
                booking.getRoom().setStatus(RoomStatus.DANG_DON);
                roomRepo.save(booking.getRoom());
            }
            bookingRepo.save(booking);
        }

        return invoiceConverter.toResponse(invoice, true);
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request) {
        Invoice invoice = invoiceRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getPaymentStatus() == PaymentStatus.DA_HUY) {
            throw new CustomException(ErrorCode.INVOICE_ALREADY_CANCELLED);
        }

        Promotion promotion = invoice.getPromotion();
        if (request.getPromotionId() != null) {
            promotion = promotionRepo.findById(request.getPromotionId()).orElse(null);
        } else if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
            promotion = promotionRepo.findByCode(request.getPromotionCode().trim()).orElse(null);
        }

        Staff staff = null;
        if (request.getStaffId() != null) {
            staff = staffRepo.findById(request.getStaffId()).orElse(null);
        } else if (invoice.getStaff() == null) {
            staff = resolveStaff(null);
        }

        invoiceConverter.updateEntity(request, invoice, promotion, staff);
        Invoice saved = invoiceRepo.save(invoice);
        return invoiceConverter.toResponse(saved, true);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        ensureStaffForInvoice(invoice);

        List<Payment> payments = paymentRepo.findByInvoiceIdWithMethod(id);
        invoice.setPayments(payments);

        return invoiceConverter.toResponse(invoice, true);
    }

    @Override
    public InvoiceResponse getInvoiceByBookingId(Long bookingId) {
        Invoice invoice = invoiceRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        return getInvoiceById(invoice.getId());
    }

    @Override
    public PageResponse<InvoiceResponse> getAllInvoices(Pageable pageable) {
        Page<Invoice> page = invoiceRepo.findAllWithDetails(pageable);
        List<InvoiceResponse> content = page.getContent().stream()
                .map(i -> {
                    ensureStaffForInvoice(i);
                    List<Payment> payments = paymentRepo.findByInvoiceIdWithMethod(i.getId());
                    i.setPayments(payments);
                    return invoiceConverter.toResponse(i, false);
                })
                .collect(Collectors.toList());

        return PageResponse.<InvoiceResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public PageResponse<InvoiceResponse> searchInvoices(String keyword, String statusStr,
                                                        LocalDate fromDate, LocalDate toDate,
                                                        Pageable pageable) {
        PaymentStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)
                && !"Tất cả thanh toán".equalsIgnoreCase(statusStr)) {
            try {
                status = PaymentStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {
                try {
                    status = PaymentStatus.fromLabel(statusStr);
                } catch (Exception ignored2) {}
            }
        }

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        java.time.LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        java.time.LocalDateTime toDateTime = toDate != null ? toDate.atTime(java.time.LocalTime.MAX) : null;

        Page<Invoice> page = invoiceRepo.searchInvoices(kw, status, fromDateTime, toDateTime, pageable);
        List<InvoiceResponse> content = page.getContent().stream()
                .map(i -> {
                    ensureStaffForInvoice(i);
                    List<Payment> payments = paymentRepo.findByInvoiceIdWithMethod(i.getId());
                    i.setPayments(payments);
                    return invoiceConverter.toResponse(i, false);
                })
                .collect(Collectors.toList());

        return PageResponse.<InvoiceResponse>builder()
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .content(content)
                .build();
    }

    @Override
    public List<InvoiceResponse> getAllInvoicesList() {
        return invoiceRepo.findAllWithDetailsList().stream()
                .map(i -> {
                    ensureStaffForInvoice(i);
                    List<Payment> payments = paymentRepo.findByInvoiceIdWithMethod(i.getId());
                    i.setPayments(payments);
                    return invoiceConverter.toResponse(i, false);
                })
                .collect(Collectors.toList());
    }

    private Staff ensureStaffForInvoice(Invoice invoice) {
        if (invoice == null) return null;
        if (invoice.getStaff() != null) {
            return invoice.getStaff();
        }
        Long id = invoice.getId() != null ? invoice.getId() : 1L;
        Long targetStaffId = (id % 3 == 1) ? 3L : (id % 3 == 2) ? 4L : 1L;
        Staff staff = staffRepo.findById(targetStaffId)
                .orElseGet(() -> staffRepo.findAll().stream().findFirst().orElse(null));
        if (staff != null) {
            invoice.setStaff(staff);
            try {
                invoiceRepo.save(invoice);
            } catch (Exception ignored) {}
        }
        return staff;
    }

    @Override
    @Transactional
    public PaymentResponse payInvoice(Long invoiceId, PaymentRequest paymentRequest) {
        Invoice invoice = invoiceRepo.findByIdWithDetails(invoiceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getPaymentStatus() == PaymentStatus.DA_HUY) {
            throw new CustomException(ErrorCode.INVOICE_ALREADY_CANCELLED);
        }

        PaymentMethod pm = paymentMethodRepo.findById(paymentRequest.getPaymentMethodId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        Staff staff = resolveStaff(paymentRequest.getStaffId());
        if (staff == null && invoice.getStaff() != null) {
            staff = invoice.getStaff();
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentMethod(pm)
                .amount(paymentRequest.getAmount())
                .transactionCode(paymentRequest.getTransactionCode())
                .note(paymentRequest.getNote())
                .staff(staff)
                .status(paymentRequest.getStatus() != null ? paymentRequest.getStatus() : TransactionStatus.THANH_CONG)
                .paymentTime(paymentRequest.getPaymentTime() != null ? paymentRequest.getPaymentTime() : LocalDateTime.now())
                .build();

        payment = paymentRepo.save(payment);

        // Tính lại tổng số tiền đã thanh toán thành công
        List<Payment> allPayments = paymentRepo.findByInvoiceIdWithMethod(invoiceId);
        BigDecimal totalPaid = allPayments.stream()
                .filter(p -> p.getStatus() == TransactionStatus.THANH_CONG && p.getAmount() != null)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus(PaymentStatus.DA_THANH_TOAN);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus(PaymentStatus.THANH_TOAN_MOT_PHAN);
        } else {
            invoice.setPaymentStatus(PaymentStatus.CHUA_THANH_TOAN);
        }
        invoiceRepo.save(invoice);

        return paymentConverter.toResponse(payment);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getPaymentStatus() == PaymentStatus.DA_HUY) {
            throw new CustomException(ErrorCode.INVOICE_ALREADY_CANCELLED);
        }

        // Chuyển sang trạng thái đã hủy (Soft Delete), bảo lưu 100% dữ liệu lịch sử thanh toán
        invoice.setPaymentStatus(PaymentStatus.DA_HUY);
        invoice.setModifiedAt(LocalDateTime.now());
        invoiceRepo.save(invoice);
    }

    /**
     * Xác định nhân viên lập hóa đơn:
     * Ưu tiên staffId truyền từ request; nếu không có, tự động lấy tài khoản đang đăng nhập từ JWT.
     */
    private Staff resolveStaff(Long requestStaffId) {
        if (requestStaffId != null) {
            return staffRepo.findById(requestStaffId).orElse(null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String account = auth.getName();
            if (account != null && !account.isBlank()) {
                return authRepo.findByUsernameOrEmailOrPhoneNumber(account).orElse(null);
            }
        }
        return null;
    }
}

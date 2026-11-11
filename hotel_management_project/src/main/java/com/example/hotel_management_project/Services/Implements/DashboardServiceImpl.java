package com.example.hotel_management_project.Services.Implements;

import com.example.hotel_management_project.DTO.Response.DashboardResponse;
import com.example.hotel_management_project.Models.Entity.*;
import com.example.hotel_management_project.Models.Enums.BookingStatus;
import com.example.hotel_management_project.Models.Enums.PaymentStatus;
import com.example.hotel_management_project.Models.Enums.RoomStatus;
import com.example.hotel_management_project.Repository.*;
import com.example.hotel_management_project.Services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final RoomRepo roomRepo;
    private final BookingRepo bookingRepo;
    private final InvoiceRepo invoiceRepo;
    private final PaymentRepo paymentRepo;
    private final BookingServiceDetailRepo bookingServiceDetailRepo;

    private static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public DashboardResponse getDashboardStats() {
        LocalDate today = LocalDate.now();

        // 1. DỮ LIỆU CỐT LÕI
        List<Room> allRooms = roomRepo.findByActiveTrue();
        List<Booking> allBookings = bookingRepo.findAllWithDetails();
        List<Invoice> allInvoices = invoiceRepo.findAllWithDetailsList();
        List<Payment> successfulPayments = paymentRepo.findAllSuccessfulWithMethod();
        List<BookingServiceDetail> allServiceDetails = bookingServiceDetailRepo.findAllWithService();

        // ── 1. KPI 1: DOANH THU HÔM NAY ──────────────────────────────────────
        BigDecimal todayRevenue = BigDecimal.ZERO;
        BigDecimal yesterdayRevenue = BigDecimal.ZERO;
        long todayInvoiceCount = 0;

        for (Payment p : successfulPayments) {
            LocalDate payDate = getPaymentLocalDate(p);
            if (payDate != null) {
                if (payDate.equals(today)) {
                    todayRevenue = todayRevenue.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                    todayInvoiceCount++;
                } else if (payDate.equals(today.minusDays(1))) {
                    yesterdayRevenue = yesterdayRevenue.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                }
            }
        }

        double revenueGrowthPercent = 0.0;
        if (yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowthPercent = todayRevenue.subtract(yesterdayRevenue)
                    .divide(yesterdayRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (todayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowthPercent = 100.0;
        }

        // Sparkline 7 điểm
        List<Double> todaySparkline = buildSparkline(successfulPayments, today);

        // ── 2. KPI 2: CÔNG SUẤT PHÒNG ────────────────────────────────────────
        long occupiedRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.DANG_O).count();
        long availableRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.TRONG).count();
        long cleaningRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.DANG_DON).count();
        long maintenanceRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.DANG_BAO_TRI).count();
        long totalRooms = allRooms.size();
        long usableRooms = Math.max(1, totalRooms - maintenanceRooms);

        int occupancyRate = (int) Math.round(((double) occupiedRooms / (double) usableRooms) * 100.0);
        int readyToWelcome = (int) availableRooms;

        // ── 3. KPI 3: CÒN PHẢI THU (PENDING RECEIVABLES) ──────────────────────
        Map<Long, BigDecimal> invoicePaidMap = new HashMap<>();
        for (Payment p : successfulPayments) {
            if (p.getInvoice() != null && p.getInvoice().getId() != null) {
                Long invId = p.getInvoice().getId();
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                invoicePaidMap.put(invId, invoicePaidMap.getOrDefault(invId, BigDecimal.ZERO).add(amt));
            }
        }

        BigDecimal pendingReceivables = BigDecimal.ZERO;
        int unpaidInvoicesCount = 0;

        for (Invoice inv : allInvoices) {
            BigDecimal total = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = invoicePaidMap.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);

            if (remaining.compareTo(BigDecimal.ZERO) > 0 || inv.getPaymentStatus() != PaymentStatus.DA_THANH_TOAN) {
                pendingReceivables = pendingReceivables.add(remaining);
                unpaidInvoicesCount++;
            }
        }

        // ── 4. KPI 4: TRẢ PHÒNG SẮP TỚI ─────────────────────────────────────
        int todayCheckouts = 0;
        int tomorrowCheckouts = 0;

        for (Booking b : allBookings) {
            if (b.getStatus() == BookingStatus.DANG_O && b.getExpectedCheckOutDate() != null) {
                if (b.getExpectedCheckOutDate().equals(today)) {
                    todayCheckouts++;
                } else if (b.getExpectedCheckOutDate().equals(today.plusDays(1))) {
                    tomorrowCheckouts++;
                }
            }
        }
        int upcomingCheckouts = todayCheckouts + tomorrowCheckouts;

        // ── 5. DOANH THU 7 NGÀY GẦN NHẤT ────────────────────────────────────
        List<DashboardResponse.DailyRevenueItem> dailyRevenues = new ArrayList<>();
        BigDecimal revenue7DaysTotal = BigDecimal.ZERO;

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal dAmount = BigDecimal.ZERO;
            long dInvoices = 0;

            for (Payment p : successfulPayments) {
                LocalDate payDate = getPaymentLocalDate(p);
                if (d.equals(payDate)) {
                    dAmount = dAmount.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                    dInvoices++;
                }
            }
            revenue7DaysTotal = revenue7DaysTotal.add(dAmount);

            boolean isToday = d.equals(today);
            String dayLabel = isToday ? "Hôm nay " + d.format(DateTimeFormatter.ofPattern("dd/MM"))
                    : getDayOfWeekVn(d.getDayOfWeek()) + " " + d.format(DateTimeFormatter.ofPattern("dd/MM"));
            String fullDate = getDayOfWeekVnFull(d.getDayOfWeek()) + ", " + d.format(DATE_VN);

            String labelTop = null;
            if (dAmount.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
                double tr = dAmount.doubleValue() / 1_000_000.0;
                labelTop = String.format(Locale.US, "%.1f tr", tr).replace(".0", "");
            } else if (dAmount.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
                double k = dAmount.doubleValue() / 1_000.0;
                labelTop = String.format(Locale.US, "%.0f k", k);
            }

            dailyRevenues.add(DashboardResponse.DailyRevenueItem.builder()
                    .dayLabel(dayLabel)
                    .amount(dAmount)
                    .labelTop(labelTop)
                    .isToday(isToday)
                    .invoiceCount(dInvoices)
                    .fullDate(fullDate)
                    .build());
        }

        BigDecimal revenue7DaysAverage = revenue7DaysTotal.divide(BigDecimal.valueOf(7), 0, RoundingMode.HALF_UP);

        // ── 6. THU TIỀN THEO PHƯƠNG THỨC ────────────────────────────────────
        Map<String, BigDecimal> paymentMethodTotals = new LinkedHashMap<>();
        BigDecimal totalPaidAllMethods = BigDecimal.ZERO;

        for (Payment p : successfulPayments) {
            String mName = (p.getPaymentMethod() != null && p.getPaymentMethod().getName() != null)
                    ? p.getPaymentMethod().getName() : "Khác";
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            paymentMethodTotals.put(mName, paymentMethodTotals.getOrDefault(mName, BigDecimal.ZERO).add(amt));
            totalPaidAllMethods = totalPaidAllMethods.add(amt);
        }

        List<DashboardResponse.PaymentMethodItem> paymentBreakdowns = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : paymentMethodTotals.entrySet()) {
            int pct = totalPaidAllMethods.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalPaidAllMethods, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            String color = getPaymentMethodColor(entry.getKey());
            paymentBreakdowns.add(DashboardResponse.PaymentMethodItem.builder()
                    .name(entry.getKey())
                    .amount(entry.getValue())
                    .amountText(formatAmountShort(entry.getValue()))
                    .percent(pct)
                    .colorHex(color)
                    .build());
        }

        // ── 7. TIỀN PHÒNG THEO LOẠI PHÒNG ───────────────────────────────────
        Map<String, BigDecimal> roomTypeRevenueMap = new LinkedHashMap<>();
        BigDecimal totalRoomRevenue = BigDecimal.ZERO;

        for (Invoice inv : allInvoices) {
            if (inv.getBooking() != null && inv.getBooking().getRoom() != null
                    && inv.getBooking().getRoom().getRoomType() != null) {
                String rtName = inv.getBooking().getRoom().getRoomType().getName();
                BigDecimal amt = inv.getRoomAmount() != null ? inv.getRoomAmount() : BigDecimal.ZERO;
                roomTypeRevenueMap.put(rtName, roomTypeRevenueMap.getOrDefault(rtName, BigDecimal.ZERO).add(amt));
                totalRoomRevenue = totalRoomRevenue.add(amt);
            }
        }

        List<DashboardResponse.RoomTypeRevenueItem> roomTypeBreakdowns = new ArrayList<>();
        List<Map.Entry<String, BigDecimal>> sortedRoomTypes = new ArrayList<>(roomTypeRevenueMap.entrySet());
        sortedRoomTypes.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, BigDecimal> entry : sortedRoomTypes) {
            int pct = totalRoomRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalRoomRevenue, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            roomTypeBreakdowns.add(DashboardResponse.RoomTypeRevenueItem.builder()
                    .name(entry.getKey())
                    .amount(entry.getValue())
                    .amountText(formatAmountShort(entry.getValue()))
                    .percent(pct)
                    .build());
        }

        // ── 8. DỊCH VỤ BÁN CHẠY ─────────────────────────────────────────────
        Map<String, BigDecimal> serviceSalesMap = new LinkedHashMap<>();
        BigDecimal totalServiceSales = BigDecimal.ZERO;

        for (BookingServiceDetail d : allServiceDetails) {
            if (d.getService() != null && d.getService().getName() != null) {
                String sName = d.getService().getName();
                BigDecimal price = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                int qty = d.getQuantity() != null ? d.getQuantity() : 0;
                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
                serviceSalesMap.put(sName, serviceSalesMap.getOrDefault(sName, BigDecimal.ZERO).add(lineTotal));
                totalServiceSales = totalServiceSales.add(lineTotal);
            }
        }

        List<Map.Entry<String, BigDecimal>> sortedServices = new ArrayList<>(serviceSalesMap.entrySet());
        sortedServices.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<DashboardResponse.ServiceSalesItem> topServices = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sortedServices.size()); i++) {
            Map.Entry<String, BigDecimal> entry = sortedServices.get(i);
            int pct = totalServiceSales.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalServiceSales, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            topServices.add(DashboardResponse.ServiceSalesItem.builder()
                    .name(entry.getKey())
                    .amount(entry.getValue())
                    .amountText(formatAmountShort(entry.getValue()))
                    .percent(pct)
                    .build());
        }

        // ── 9. LỊCH HÔM NAY & NGÀY MAI ─────────────────────────────────────
        List<DashboardResponse.ScheduleEventItem> upcomingEvents = new ArrayList<>();
        LocalDate tomorrow = today.plusDays(1);

        for (Booking b : allBookings) {
            String custName = b.getCustomer() != null ? b.getCustomer().getFullName() : "Khách vãng lai";
            String rInfo = "";
            if (b.getRoom() != null) {
                rInfo = b.getRoom().getRoomNumber() + (b.getRoom().getRoomType() != null ? " · " + b.getRoom().getRoomType().getName() : "");
            }

            // Nhận phòng: Đã đặt trước và ngày nhận là hôm nay / ngày mai
            if (b.getStatus() == BookingStatus.DAT_TRUOC && b.getCheckInDate() != null) {
                if (b.getCheckInDate().equals(today)) {
                    upcomingEvents.add(DashboardResponse.ScheduleEventItem.builder()
                            .type("NHAN_PHONG")
                            .typeLabel("NHẬN PHÒNG")
                            .customerName(custName)
                            .roomInfo(rInfo)
                            .timeLabel("Hôm nay")
                            .build());
                } else if (b.getCheckInDate().equals(tomorrow)) {
                    upcomingEvents.add(DashboardResponse.ScheduleEventItem.builder()
                            .type("NHAN_PHONG")
                            .typeLabel("NHẬN PHÒNG")
                            .customerName(custName)
                            .roomInfo(rInfo)
                            .timeLabel("Ngày mai")
                            .build());
                }
            }

            // Trả phòng: Đang ở và ngày trả dự kiến là hôm nay / ngày mai
            if (b.getStatus() == BookingStatus.DANG_O && b.getExpectedCheckOutDate() != null) {
                if (b.getExpectedCheckOutDate().equals(today)) {
                    upcomingEvents.add(DashboardResponse.ScheduleEventItem.builder()
                            .type("TRA_PHONG")
                            .typeLabel("TRẢ PHÒNG")
                            .customerName(custName)
                            .roomInfo(rInfo)
                            .timeLabel("Hôm nay")
                            .build());
                } else if (b.getExpectedCheckOutDate().equals(tomorrow)) {
                    upcomingEvents.add(DashboardResponse.ScheduleEventItem.builder()
                            .type("TRA_PHONG")
                            .typeLabel("TRẢ PHÒNG")
                            .customerName(custName)
                            .roomInfo(rInfo)
                            .timeLabel("Ngày mai")
                            .build());
                }
            }
        }

        // ── 10. HÓA ĐƠN GẦN ĐÂY ─────────────────────────────────────────────
        List<DashboardResponse.RecentInvoiceItem> recentInvoices = new ArrayList<>();
        int limitInvoices = Math.min(10, allInvoices.size());
        for (int i = 0; i < limitInvoices; i++) {
            Invoice inv = allInvoices.get(i);
            String cName = (inv.getBooking() != null && inv.getBooking().getCustomer() != null)
                    ? inv.getBooking().getCustomer().getFullName() : "Khách vãng lai";
            String rCode = (inv.getBooking() != null && inv.getBooking().getRoom() != null)
                    ? inv.getBooking().getRoom().getRoomNumber() : "—";
            String tText = formatMoney(inv.getTotalAmount());
            String st = inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "CHUA_THANH_TOAN";
            String stLabel = inv.getPaymentStatus() != null ? inv.getPaymentStatus().getLabel() : "Chưa thanh toán";
            String dStr = inv.getCreatedAt() != null ? inv.getCreatedAt().format(DATE_VN) : "";

            recentInvoices.add(DashboardResponse.RecentInvoiceItem.builder()
                    .id(inv.getId())
                    .customerName(cName)
                    .roomCode(rCode)
                    .totalText(tText)
                    .status(st)
                    .statusLabel(stLabel)
                    .date(dStr)
                    .build());
        }

        // ── 11. AI INSIGHTS TỔNG HỢP THÔNG MINH ─────────────────────────────
        List<String> aiBulletPoints = new ArrayList<>();
        if (todayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            aiBulletPoints.add(String.format("Doanh thu hôm nay đạt %s (%s so với hôm qua) với %d giao dịch thành công.",
                    formatMoney(todayRevenue),
                    revenueGrowthPercent >= 0 ? "+" + Math.round(revenueGrowthPercent) + "%" : Math.round(revenueGrowthPercent) + "%",
                    todayInvoiceCount));
        } else {
            aiBulletPoints.add(String.format("Hôm nay chưa ghi nhận doanh thu mới phát sinh. Doanh thu 7 ngày qua đạt %s (trung bình %s/ngày).",
                    formatMoney(revenue7DaysTotal), formatMoney(revenue7DaysAverage)));
        }

        if (pendingReceivables.compareTo(BigDecimal.ZERO) > 0) {
            aiBulletPoints.add(String.format("Có %d hóa đơn còn phải thu với tổng công nợ %s, bộ phận lễ tân cần lưu ý đối soát khi khách trả phòng.",
                    unpaidInvoicesCount, formatMoney(pendingReceivables)));
        } else {
            aiBulletPoints.add("Toàn bộ các hóa đơn đã được thanh toán đầy đủ, không có công nợ tồn đọng.");
        }

        aiBulletPoints.add(String.format("Công suất phòng hiện tại đạt %d%% (%d/%d phòng khả dụng). Có %d phòng trống sẵn sàng đón khách.",
                occupancyRate, occupiedRooms, totalRooms, readyToWelcome));

        if (upcomingCheckouts > 0) {
            aiBulletPoints.add(String.format("Có %d lượt trả phòng sắp tới (%d lượt hôm nay, %d lượt ngày mai), cần phối hợp buồng phòng dọn dẹp vệ sinh.",
                    upcomingCheckouts, todayCheckouts, tomorrowCheckouts));
        }

        return DashboardResponse.builder()
                .todayRevenue(todayRevenue)
                .revenueGrowthPercent(revenueGrowthPercent)
                .todaySparkline(todaySparkline)
                .todayInvoiceCount(todayInvoiceCount)
                .occupancyRate(occupancyRate)
                .occupiedRooms(occupiedRooms)
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .cleaningRooms(cleaningRooms)
                .maintenanceRooms(maintenanceRooms)
                .pendingReceivables(pendingReceivables)
                .unpaidInvoicesCount(unpaidInvoicesCount)
                .upcomingCheckouts(upcomingCheckouts)
                .todayCheckouts(todayCheckouts)
                .tomorrowCheckouts(tomorrowCheckouts)
                .aiInsightTitle("Nhận định hôm nay")
                .aiInsightSub("Trợ lý đọc dữ liệu phòng, hóa đơn và thanh toán để nêu việc cần chú ý")
                .aiInsightContent("Bấm \"Phân tích\" để AI tóm tắt việc cần xử lý, doanh thu, công nợ và cơ hội.")
                .aiInsightBulletPoints(aiBulletPoints)
                .revenue7DaysTotal(revenue7DaysTotal)
                .revenue7DaysAverage(revenue7DaysAverage)
                .dailyRevenues(dailyRevenues)
                .readyToWelcome(readyToWelcome)
                .paymentBreakdowns(paymentBreakdowns)
                .roomTypeBreakdowns(roomTypeBreakdowns)
                .topServices(topServices)
                .upcomingEvents(upcomingEvents)
                .recentInvoices(recentInvoices)
                .build();
    }

    private LocalDate getPaymentLocalDate(Payment p) {
        if (p.getPaymentTime() != null) {
            return p.getPaymentTime().toLocalDate();
        }
        if (p.getCreatedAt() != null) {
            return p.getCreatedAt().toLocalDate();
        }
        return null;
    }

    private List<Double> buildSparkline(List<Payment> payments, LocalDate today) {
        List<Double> points = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            double sum = 0;
            for (Payment p : payments) {
                LocalDate pd = getPaymentLocalDate(p);
                if (d.equals(pd)) {
                    sum += p.getAmount() != null ? p.getAmount().doubleValue() : 0;
                }
            }
            points.add(sum > 0 ? Math.min(100.0, Math.max(15.0, sum / 100_000.0)) : 10.0);
        }
        return points;
    }

    private String getDayOfWeekVn(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private String getDayOfWeekVnFull(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private String getPaymentMethodColor(String name) {
        if (name == null) return "#F59E0B";
        String lower = name.toLowerCase();
        if (lower.contains("momo")) return "#F97316";
        if (lower.contains("tiền mặt") || lower.contains("cash")) return "#10B981";
        if (lower.contains("chuyển khoản") || lower.contains("ngân hàng") || lower.contains("bank")) return "#3B82F6";
        return "#F59E0B";
    }

    private String formatAmountShort(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) == 0) return "0 ₫";
        if (amt.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            double tr = amt.doubleValue() / 1_000_000.0;
            return String.format(Locale.US, "%.1f tr", tr).replace(".0", "");
        } else if (amt.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
            double k = amt.doubleValue() / 1_000.0;
            return String.format(Locale.US, "%.0f k", k);
        }
        return formatMoney(amt);
    }

    private String formatMoney(BigDecimal amt) {
        if (amt == null) return "0 ₫";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amt) + " ₫";
    }
}


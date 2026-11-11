package com.example.hotel_management_project.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // ── 1. KPI Cards ────────────────────────────────────────────────────────
    @Builder.Default
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    @Builder.Default
    private Double revenueGrowthPercent = 0.0;
    @Builder.Default
    private List<Double> todaySparkline = new ArrayList<>();
    @Builder.Default
    private Long todayInvoiceCount = 0L;

    @Builder.Default
    private Integer occupancyRate = 0;
    @Builder.Default
    private Long occupiedRooms = 0L;
    @Builder.Default
    private Long totalRooms = 0L;
    @Builder.Default
    private Long availableRooms = 0L;
    @Builder.Default
    private Long cleaningRooms = 0L;
    @Builder.Default
    private Long maintenanceRooms = 0L;

    @Builder.Default
    private BigDecimal pendingReceivables = BigDecimal.ZERO;
    @Builder.Default
    private Integer unpaidInvoicesCount = 0;

    @Builder.Default
    private Integer upcomingCheckouts = 0;
    @Builder.Default
    private Integer todayCheckouts = 0;
    @Builder.Default
    private Integer tomorrowCheckouts = 0;

    // ── 2. AI Insight (Nhận định hôm nay) ──────────────────────────────────
    @Builder.Default
    private String aiInsightTitle = "Nhận định hôm nay";
    @Builder.Default
    private String aiInsightSub = "Trợ lý đọc dữ liệu phòng, hóa đơn và thanh toán để nêu việc cần chú ý";
    @Builder.Default
    private String aiInsightContent = "Bấm \"Phân tích\" để AI tóm tắt việc cần xử lý, doanh thu, công nợ và cơ hội.";
    @Builder.Default
    private List<String> aiInsightBulletPoints = new ArrayList<>();

    // ── 3. Doanh thu 7 ngày gần nhất ────────────────────────────────────────
    @Builder.Default
    private BigDecimal revenue7DaysTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal revenue7DaysAverage = BigDecimal.ZERO;
    @Builder.Default
    private List<DailyRevenueItem> dailyRevenues = new ArrayList<>();

    // ── 4. Tình trạng phòng ─────────────────────────────────────────────────
    @Builder.Default
    private Integer readyToWelcome = 0;

    // ── 5. Thu tiền theo phương thức ────────────────────────────────────────
    @Builder.Default
    private List<PaymentMethodItem> paymentBreakdowns = new ArrayList<>();

    // ── 6. Tiền phòng theo loại phòng ───────────────────────────────────────
    @Builder.Default
    private List<RoomTypeRevenueItem> roomTypeBreakdowns = new ArrayList<>();

    // ── 7. Dịch vụ bán chạy ────────────────────────────────────────────────
    @Builder.Default
    private List<ServiceSalesItem> topServices = new ArrayList<>();

    // ── 8. Lịch hôm nay & ngày mai ─────────────────────────────────────────
    @Builder.Default
    private List<ScheduleEventItem> upcomingEvents = new ArrayList<>();

    // ── 9. Hóa đơn gần đây ─────────────────────────────────────────────────
    @Builder.Default
    private List<RecentInvoiceItem> recentInvoices = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyRevenueItem {
        private String dayLabel; // e.g. "T5 29/10", "T6 30/10", "Hôm nay 04/11"
        private BigDecimal amount;
        private String labelTop; // e.g. "7.3 tr" or null
        private Boolean isToday;
        private Long invoiceCount;
        private String fullDate; // e.g. "T6, 30/10/2026"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodItem {
        private String name;
        private String amountText;
        private Integer percent;
        private String colorHex;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomTypeRevenueItem {
        private String name;
        private String amountText;
        private Integer percent;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceSalesItem {
        private String name;
        private String amountText;
        private Integer percent;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleEventItem {
        private String type; // "NHAN_PHONG", "TRA_PHONG"
        private String typeLabel; // "NHẬN PHÒNG", "TRẢ PHÒNG"
        private String customerName;
        private String roomInfo;
        private String timeLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentInvoiceItem {
        private Long id;
        private String customerName;
        private String roomCode;
        private String totalText;
        private String status;
        private String statusLabel;
        private String date;
    }
}


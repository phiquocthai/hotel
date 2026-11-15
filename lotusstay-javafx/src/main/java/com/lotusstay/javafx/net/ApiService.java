package com.lotusstay.javafx.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lotusstay.javafx.model.*;
import com.lotusstay.javafx.ui.Util;

import java.io.File;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ApiService kết nối trực tiếp tới Backend Spring Boot qua ApiClient (REST
 * HTTP).
 * Toàn bộ dữ liệu mẫu (mock seed data) đã được xóa bỏ hoàn toàn.
 */
public class ApiService {

    private static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ============== AUTH (ĐĂNG NHẬP) ==============
    public static LoginResponse login(String username, String password) {
        Map<String, Object> req = new HashMap<>();
        req.put("account", username);
        req.put("password", password);
        req.put("rememberMe", false);

        JsonNode root = ApiClient.post("/api/auth/login", req, new TypeReference<JsonNode>() {
        });
        if (root == null) {
            throw new ApiClient.ApiException("Không nhận được phản hồi từ máy chủ.");
        }

        JsonNode resultNode = root.get("result");
        if (resultNode == null || resultNode.isNull()) {
            String msg = root.has("message") ? root.get("message").asText() : "Đăng nhập thất bại.";
            throw new ApiClient.ApiException(msg);
        }

        String token = resultNode.has("token") ? resultNode.get("token").asText() : null;
        JsonNode userNode = resultNode.get("user");

        LoginResponse res = new LoginResponse();
        res.token = token;

        if (userNode != null && !userNode.isNull()) {
            res.userId = userNode.has("id") ? userNode.get("id").asLong() : null;
            res.username = userNode.has("username") ? userNode.get("username").asText() : username;
            res.fullName = userNode.has("fullName") ? userNode.get("fullName").asText() : "";

            if (userNode.has("role")) {
                JsonNode roleNode = userNode.get("role");
                if (roleNode.isObject() && roleNode.has("name")) {
                    res.role = roleNode.get("name").asText();
                } else {
                    res.role = roleNode.asText();
                }
            }
        }

        // Lưu phiên đăng nhập
        Session.set(res.token, res.userId, res.username, res.fullName, res.role);
        return res;
    }

    // ============== DASHBOARD ==============
    public static DashboardStats dashboard() {
        try {
            ApiResponse<JsonNode> resp = ApiClient.get("/api/dashboard",
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });
            if (resp != null && resp.result != null) {
                return mapDashboardStats(resp.result);
            }
        } catch (Exception ex) {
            System.err.println("Dashboard API call failed, falling back to local calculation: " + ex.getMessage());
        }

        return computeDashboardStatsFallback();
    }

    private static DashboardStats mapDashboardStats(JsonNode n) {
        DashboardStats s = new DashboardStats();

        // 1. KPI Cards
        s.todayRevenue = n.hasNonNull("todayRevenue") ? new BigDecimal(n.get("todayRevenue").asText()) : BigDecimal.ZERO;
        s.revenueGrowthPercent = n.hasNonNull("revenueGrowthPercent") ? n.get("revenueGrowthPercent").asDouble() : 0.0;
        s.todayInvoiceCount = n.hasNonNull("todayInvoiceCount") ? n.get("todayInvoiceCount").asLong() : 0L;

        if (n.has("todaySparkline") && n.get("todaySparkline").isArray()) {
            List<Double> spark = new ArrayList<>();
            for (JsonNode pt : n.get("todaySparkline")) {
                spark.add(pt.asDouble());
            }
            if (!spark.isEmpty()) s.todaySparkline = spark;
        }

        s.occupancyRate = n.hasNonNull("occupancyRate") ? n.get("occupancyRate").asInt() : 0;
        s.occupiedRooms = n.hasNonNull("occupiedRooms") ? n.get("occupiedRooms").asLong() : 0L;
        s.totalRooms = n.hasNonNull("totalRooms") ? n.get("totalRooms").asLong() : 0L;
        s.availableRooms = n.hasNonNull("availableRooms") ? n.get("availableRooms").asLong() : 0L;
        s.cleaningRooms = n.hasNonNull("cleaningRooms") ? n.get("cleaningRooms").asLong() : 0L;
        s.maintenanceRooms = n.hasNonNull("maintenanceRooms") ? n.get("maintenanceRooms").asLong() : 0L;

        s.pendingReceivables = n.hasNonNull("pendingReceivables") ? new BigDecimal(n.get("pendingReceivables").asText()) : BigDecimal.ZERO;
        s.unpaidInvoicesCount = n.hasNonNull("unpaidInvoicesCount") ? n.get("unpaidInvoicesCount").asInt() : 0;

        s.upcomingCheckouts = n.hasNonNull("upcomingCheckouts") ? n.get("upcomingCheckouts").asInt() : 0;
        s.todayCheckouts = n.hasNonNull("todayCheckouts") ? n.get("todayCheckouts").asInt() : 0;
        s.tomorrowCheckouts = n.hasNonNull("tomorrowCheckouts") ? n.get("tomorrowCheckouts").asInt() : 0;

        // 2. AI Insight
        s.aiInsightTitle = n.hasNonNull("aiInsightTitle") ? n.get("aiInsightTitle").asText() : "Nhận định hôm nay";
        s.aiInsightSub = n.hasNonNull("aiInsightSub") ? n.get("aiInsightSub").asText() : "Trợ lý đọc dữ liệu phòng, hóa đơn và thanh toán để nêu việc cần chú ý";
        s.aiInsightContent = n.hasNonNull("aiInsightContent") ? n.get("aiInsightContent").asText() : "Bấm \"Phân tích\" để AI tóm tắt việc cần xử lý, doanh thu, công nợ và cơ hội.";

        if (n.has("aiInsightBulletPoints") && n.get("aiInsightBulletPoints").isArray()) {
            List<String> bullets = new ArrayList<>();
            for (JsonNode b : n.get("aiInsightBulletPoints")) {
                bullets.add(b.asText());
            }
            if (!bullets.isEmpty()) s.aiInsightBulletPoints = bullets;
        }

        // 3. Doanh thu 7 ngày
        s.revenue7DaysTotal = n.hasNonNull("revenue7DaysTotal") ? new BigDecimal(n.get("revenue7DaysTotal").asText()) : BigDecimal.ZERO;
        s.revenue7DaysAverage = n.hasNonNull("revenue7DaysAverage") ? new BigDecimal(n.get("revenue7DaysAverage").asText()) : BigDecimal.ZERO;

        if (n.has("dailyRevenues") && n.get("dailyRevenues").isArray()) {
            List<DashboardStats.DailyRevenueItem> daily = new ArrayList<>();
            for (JsonNode d : n.get("dailyRevenues")) {
                DashboardStats.DailyRevenueItem item = new DashboardStats.DailyRevenueItem();
                item.dayLabel = d.hasNonNull("dayLabel") ? d.get("dayLabel").asText() : "";
                item.amount = d.hasNonNull("amount") ? new BigDecimal(d.get("amount").asText()) : BigDecimal.ZERO;
                item.labelTop = d.hasNonNull("labelTop") ? d.get("labelTop").asText() : null;
                item.isToday = d.hasNonNull("isToday") && d.get("isToday").asBoolean();
                item.invoiceCount = d.hasNonNull("invoiceCount") ? d.get("invoiceCount").asLong() : 0L;
                item.fullDate = d.hasNonNull("fullDate") ? d.get("fullDate").asText() : "";
                daily.add(item);
            }
            s.dailyRevenues = daily;
        }

        // 4. Tình trạng phòng
        s.readyToWelcome = n.hasNonNull("readyToWelcome") ? n.get("readyToWelcome").asInt() : (int) s.availableRooms;

        // 5. Thu tiền theo phương thức
        if (n.has("paymentBreakdowns") && n.get("paymentBreakdowns").isArray()) {
            List<DashboardStats.PaymentMethodItem> pms = new ArrayList<>();
            for (JsonNode p : n.get("paymentBreakdowns")) {
                DashboardStats.PaymentMethodItem item = new DashboardStats.PaymentMethodItem();
                item.name = p.hasNonNull("name") ? p.get("name").asText() : "";
                item.amountText = p.hasNonNull("amountText") ? p.get("amountText").asText() : "0 ₫";
                item.percent = p.hasNonNull("percent") ? p.get("percent").asInt() : 0;
                item.colorHex = p.hasNonNull("colorHex") ? p.get("colorHex").asText() : "#F59E0B";
                pms.add(item);
            }
            s.paymentBreakdowns = pms;
        }

        // 6. Tiền phòng theo loại phòng
        if (n.has("roomTypeBreakdowns") && n.get("roomTypeBreakdowns").isArray()) {
            List<DashboardStats.RoomTypeRevenueItem> rts = new ArrayList<>();
            for (JsonNode r : n.get("roomTypeBreakdowns")) {
                DashboardStats.RoomTypeRevenueItem item = new DashboardStats.RoomTypeRevenueItem();
                item.name = r.hasNonNull("name") ? r.get("name").asText() : "";
                item.amountText = r.hasNonNull("amountText") ? r.get("amountText").asText() : "0 ₫";
                item.percent = r.hasNonNull("percent") ? r.get("percent").asInt() : 0;
                rts.add(item);
            }
            s.roomTypeBreakdowns = rts;
        }

        // 7. Dịch vụ bán chạy
        if (n.has("topServices") && n.get("topServices").isArray()) {
            List<DashboardStats.ServiceSalesItem> svcs = new ArrayList<>();
            for (JsonNode sv : n.get("topServices")) {
                DashboardStats.ServiceSalesItem item = new DashboardStats.ServiceSalesItem();
                item.name = sv.hasNonNull("name") ? sv.get("name").asText() : "";
                item.amountText = sv.hasNonNull("amountText") ? sv.get("amountText").asText() : "0 ₫";
                item.percent = sv.hasNonNull("percent") ? sv.get("percent").asInt() : 0;
                svcs.add(item);
            }
            s.topServices = svcs;
        }

        // 8. Lịch hôm nay & ngày mai
        if (n.has("upcomingEvents") && n.get("upcomingEvents").isArray()) {
            List<DashboardStats.ScheduleEventItem> evs = new ArrayList<>();
            for (JsonNode ev : n.get("upcomingEvents")) {
                DashboardStats.ScheduleEventItem item = new DashboardStats.ScheduleEventItem();
                item.type = ev.hasNonNull("type") ? ev.get("type").asText() : "";
                item.typeLabel = ev.hasNonNull("typeLabel") ? ev.get("typeLabel").asText() : "";
                item.customerName = ev.hasNonNull("customerName") ? ev.get("customerName").asText() : "";
                item.roomInfo = ev.hasNonNull("roomInfo") ? ev.get("roomInfo").asText() : "";
                item.timeLabel = ev.hasNonNull("timeLabel") ? ev.get("timeLabel").asText() : "";
                evs.add(item);
            }
            s.upcomingEvents = evs;
        }

        // 9. Hóa đơn gần đây
        if (n.has("recentInvoices") && n.get("recentInvoices").isArray()) {
            List<DashboardStats.RecentInvoiceItem> invs = new ArrayList<>();
            for (JsonNode in : n.get("recentInvoices")) {
                DashboardStats.RecentInvoiceItem item = new DashboardStats.RecentInvoiceItem();
                item.customerName = in.hasNonNull("customerName") ? in.get("customerName").asText() : "";
                item.roomCode = in.hasNonNull("roomCode") ? in.get("roomCode").asText() : "";
                item.totalText = in.hasNonNull("totalText") ? in.get("totalText").asText() : "0 ₫";
                item.status = in.hasNonNull("status") ? in.get("status").asText() : "";
                item.statusLabel = in.hasNonNull("statusLabel") ? in.get("statusLabel").asText() : "";
                item.date = in.hasNonNull("date") ? in.get("date").asText() : "";
                invs.add(item);
            }
            s.recentInvoices = invs;
        }

        return s;
    }

    private static DashboardStats computeDashboardStatsFallback() {
        DashboardStats s = new DashboardStats();
        List<Room> roomList = rooms();
        long occupied = roomList.stream().filter(r -> "DANG_O".equalsIgnoreCase(r.status)).count();
        long available = roomList.stream().filter(r -> "TRONG".equalsIgnoreCase(r.status)).count();
        long cleaning = roomList.stream().filter(r -> "DANG_DON".equalsIgnoreCase(r.status)).count();
        long maintenance = roomList.stream().filter(r -> "BAO_TRI".equalsIgnoreCase(r.status) || "DANG_BAO_TRI".equalsIgnoreCase(r.status)).count();

        s.totalRooms = roomList.size();
        s.occupiedRooms = occupied;
        s.availableRooms = available;
        s.cleaningRooms = cleaning;
        s.maintenanceRooms = maintenance;
        s.occupancyRate = s.totalRooms > 0 ? (int) Math.round((occupied * 100.0) / s.totalRooms) : 0;
        s.readyToWelcome = (int) available;

        List<Invoice> invList = invoices();
        BigDecimal pending = BigDecimal.ZERO;
        int unpaidCount = 0;
        for (Invoice inv : invList) {
            BigDecimal rem = inv.remainingAmount != null ? inv.remainingAmount : BigDecimal.ZERO;
            if (rem.compareTo(BigDecimal.ZERO) > 0 || !"DA_THANH_TOAN".equalsIgnoreCase(inv.paymentStatus)) {
                pending = pending.add(rem);
                unpaidCount++;
            }
        }
        s.pendingReceivables = pending;
        s.unpaidInvoicesCount = unpaidCount;

        List<DashboardStats.RecentInvoiceItem> recents = new ArrayList<>();
        for (int i = 0; i < Math.min(10, invList.size()); i++) {
            Invoice inv = invList.get(i);
            String cName = inv.customer != null && inv.customer.fullName != null ? inv.customer.fullName : "Khách vãng lai";
            String rNum = inv.room != null && inv.room.number != null ? inv.room.number : "—";
            recents.add(new DashboardStats.RecentInvoiceItem(cName, rNum, Util.money(inv.total), inv.paymentStatus, inv.paymentStatusLabel(), inv.createdAt != null ? inv.createdAt : inv.issuedDate));
        }
        s.recentInvoices = recents;

        return s;
    }

    // ============== ROOM TYPES (LOẠI PHÒNG) ==============
    public static List<RoomType> roomTypes() {
        PageResponse<JsonNode> page = ApiClient.get("/api/room-type?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<RoomType> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            RoomType rt = new RoomType();
            rt.id = n.has("id") ? n.get("id").asLong() : null;
            rt.code = rt.id != null ? String.format("LP%02d", rt.id) : "";
            rt.name = n.has("name") ? n.get("name").asText() : "";
            rt.basePrice = n.has("basePrice") ? new BigDecimal(n.get("basePrice").asText()) : BigDecimal.ZERO;
            int g1 = 2;
            if (n.has("maxOccupancy") && !n.get("maxOccupancy").isNull()) {
                g1 = n.get("maxOccupancy").asInt();
            } else if (n.has("maxGuests") && !n.get("maxGuests").isNull()) {
                g1 = n.get("maxGuests").asInt();
            }
            rt.maxGuests = g1;
            rt.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
            rt.active = !n.has("active") || n.get("active").asBoolean();
            list.add(rt);
        }
        return list;
    }

    public static PageResponse<RoomType> roomTypePage(int pageNo, int pageSize, String keyword, Integer maxGuests,
            BigDecimal minPrice, BigDecimal maxPrice, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/room-type/search?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (maxGuests != null) {
            sb.append("&maxGuests=").append(maxGuests);
        }
        if (minPrice != null) {
            sb.append("&minPrice=").append(minPrice);
        }
        if (maxPrice != null) {
            sb.append("&maxPrice=").append(maxPrice);
        }
        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<RoomType> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<RoomType> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                RoomType rt = new RoomType();
                rt.id = n.has("id") ? n.get("id").asLong() : null;
                rt.code = rt.id != null ? String.format("LP%02d", rt.id) : "";
                rt.name = n.has("name") ? n.get("name").asText() : "";
                rt.basePrice = n.has("basePrice") ? new BigDecimal(n.get("basePrice").asText()) : BigDecimal.ZERO;
                int g2 = 2;
                if (n.has("maxOccupancy") && !n.get("maxOccupancy").isNull()) {
                    g2 = n.get("maxOccupancy").asInt();
                } else if (n.has("maxGuests") && !n.get("maxGuests").isNull()) {
                    g2 = n.get("maxGuests").asInt();
                }
                rt.maxGuests = g2;
                rt.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
                rt.active = !n.has("active") || n.get("active").asBoolean();
                list.add(rt);
            }
        }
        res.content = list;
        return res;
    }

    public static RoomType createRoomType(String name, BigDecimal price, Integer maxGuests) {
        return createRoomType(null, name, price, maxGuests);
    }

    public static RoomType createRoomType(String code, String name, BigDecimal price, Integer maxGuests) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("basePrice", price);
        req.put("maxOccupancy", maxGuests == null ? 2 : maxGuests);

        ApiResponse<JsonNode> resp = ApiClient.post("/api/room-type", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        RoomType rt = new RoomType();
        if (resp != null && resp.result != null) {
            JsonNode n = resp.result;
            rt.id = n.has("id") ? n.get("id").asLong() : null;
            rt.code = rt.id != null ? String.format("LP%02d", rt.id) : (code != null ? code : "LP01");
            rt.name = n.has("name") ? n.get("name").asText() : name;
            rt.basePrice = n.has("basePrice") ? new BigDecimal(n.get("basePrice").asText()) : price;
            rt.maxGuests = n.has("maxOccupancy") ? n.get("maxOccupancy").asInt() : maxGuests;
        }
        return rt;
    }

    public static RoomType updateRoomType(Long id, String code, String name, BigDecimal price, Integer maxGuests) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("basePrice", price);
        req.put("maxOccupancy", maxGuests == null ? 2 : maxGuests);

        ApiResponse<JsonNode> resp = ApiClient.put("/api/room-type/" + id, req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        RoomType rt = new RoomType();
        rt.id = id;
        rt.code = code != null && !code.isBlank() ? code : String.format("LP%02d", id);
        rt.name = name;
        rt.basePrice = price;
        rt.maxGuests = maxGuests;
        return rt;
    }

    public static void deleteRoomType(Long id) {
        ApiClient.delete("/api/room-type/" + id);
    }

    // ============== ROOMS (PHÒNG) ==============
    public static List<Room> rooms() {
        PageResponse<JsonNode> page = ApiClient.get("/api/rooms?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<Room> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            list.add(mapRoom(n));
        }
        return list;
    }

    private static Room mapRoom(JsonNode n) {
        Room r = new Room();
        r.id = n.has("id") ? n.get("id").asLong() : null;
        r.number = n.has("roomNumber") ? n.get("roomNumber").asText() : "";
        r.code = "P" + r.number;
        r.floor = n.has("floor") ? n.get("floor").asInt() : 1;
        r.status = mapBackendRoomStatus(n.has("status") ? n.get("status").asText() : "Trống");
        r.notes = n.has("note") ? n.get("note").asText() : "";
        r.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
        r.active = !n.has("active") || n.get("active").asBoolean();

        RoomType rt = new RoomType();
        if (n.has("roomTypeId") && !n.get("roomTypeId").isNull()) {
            rt.id = n.get("roomTypeId").asLong();
        }
        rt.name = n.has("roomTypeName") && !n.get("roomTypeName").isNull() ? n.get("roomTypeName").asText()
                : "Tiêu chuẩn";

        BigDecimal price = null;
        if (n.has("roomTypePrice") && !n.get("roomTypePrice").isNull()) {
            try {
                price = new BigDecimal(n.get("roomTypePrice").asText());
            } catch (Exception ignored) {
            }
        }
        if (price == null && n.has("basePrice") && !n.get("basePrice").isNull()) {
            try {
                price = new BigDecimal(n.get("basePrice").asText());
            } catch (Exception ignored) {
            }
        }
        if (price == null && n.has("price") && !n.get("price").isNull()) {
            try {
                price = new BigDecimal(n.get("price").asText());
            } catch (Exception ignored) {
            }
        }
        rt.basePrice = price;
        r.roomType = rt;
        r.basePrice = price;
        return r;
    }

    public static PageResponse<Room> roomPage(int pageNo, int pageSize, String keyword, Long roomTypeId, String status,
            String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/rooms?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (roomTypeId != null) {
            sb.append("&roomTypeId=").append(roomTypeId);
        }
        String beStatus = mapFeRoomStatusToBackendEnum(status);
        if (beStatus != null) {
            sb.append("&status=").append(beStatus);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Room> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Room> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapRoom(n));
            }
        }
        res.content = list;
        return res;
    }

    public static Room createRoom(String number, Long roomTypeId, Integer floor, String notes) {
        return createRoom(null, number, roomTypeId, floor, "TRONG", notes);
    }

    public static Room createRoom(String code, String number, Long roomTypeId, Integer floor, String status,
            String notes) {
        Map<String, Object> req = new HashMap<>();
        req.put("roomNumber", number);
        req.put("roomTypeId", roomTypeId);
        req.put("floor", floor == null ? 1 : floor);
        String beStatus = mapFeRoomStatusToBackendEnum(status);
        req.put("status", beStatus != null ? beStatus : "TRONG");
        req.put("note", notes != null ? notes : "");

        ApiResponse<JsonNode> resp = ApiClient.post("/api/rooms", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Room r = new Room();
        r.number = number;
        r.code = code != null && !code.isBlank() ? code : "P" + number;
        r.floor = floor != null ? floor : 1;
        r.status = status != null ? status : "TRONG";
        r.notes = notes;
        if (resp != null && resp.result != null && resp.result.has("id")) {
            r.id = resp.result.get("id").asLong();
        }
        return r;
    }

    public static Room updateRoom(Long id, String code, String number, Long roomTypeId, Integer floor, String status,
            String notes) {
        Map<String, Object> req = new HashMap<>();
        req.put("roomNumber", number);
        req.put("roomTypeId", roomTypeId);
        req.put("floor", floor == null ? 1 : floor);
        String beStatus = mapFeRoomStatusToBackendEnum(status);
        req.put("status", beStatus != null ? beStatus : "TRONG");
        req.put("note", notes != null ? notes : "");

        ApiClient.put("/api/rooms/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        Room r = new Room();
        r.id = id;
        r.number = number;
        r.code = code != null && !code.isBlank() ? code : "P" + number;
        r.floor = floor != null ? floor : 1;
        r.status = status;
        r.notes = notes;
        return r;
    }

    public static Room updateRoomStatus(Long roomId, String status) {
        // Lấy thông tin phòng hiện tại
        ApiResponse<JsonNode> current = ApiClient.get("/api/rooms/" + roomId,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        if (current != null && current.result != null) {
            JsonNode n = current.result;
            Map<String, Object> req = new HashMap<>();
            req.put("roomNumber", n.has("roomNumber") ? n.get("roomNumber").asText() : "");
            Long rTypeId = null;
            if (n.hasNonNull("roomTypeId")) {
                rTypeId = n.get("roomTypeId").asLong();
            } else if (n.has("roomType") && n.get("roomType").hasNonNull("id")) {
                rTypeId = n.get("roomType").get("id").asLong();
            }
            req.put("roomTypeId", rTypeId);
            req.put("floor", n.has("floor") ? n.get("floor").asInt() : 1);
            String beStatus = mapFeRoomStatusToBackendEnum(status);
            req.put("status", beStatus != null ? beStatus : "TRONG");
            req.put("note", n.has("note") ? n.get("note").asText() : "");
            ApiClient.put("/api/rooms/" + roomId, req, new TypeReference<ApiResponse<JsonNode>>() {
            });
        }
        Room r = new Room();
        r.id = roomId;
        r.status = status;
        return r;
    }

    public static void deleteRoom(Long id) {
        ApiClient.delete("/api/rooms/" + id);
    }

    // ============== CUSTOMERS (KHÁCH HÀNG) ==============
    public static PageResponse<Customer> customerPage(int pageNo, int pageSize, String query) {
        return customerPage(pageNo, pageSize, query, "asc");
    }

    public static PageResponse<Customer> customerPage(int pageNo, int pageSize, String query, String sortDir) {
        String path;
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        if (query == null || query.isBlank()) {
            path = "/api/customer?pageNo=" + safePage + "&pageSize=" + safeSize + "&sortBy=id&sortDir=" + sDir;
        } else {
            String q = query.trim();
            String enc = java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
            if (q.contains("@")) {
                path = "/api/customer/search?pageNo=" + safePage + "&pageSize=" + safeSize
                        + "&sortBy=id&sortDir=" + sDir + "&email=" + enc;
            } else if (q.matches("\\d{10,12}")) {
                if (q.length() == 12) {
                    path = "/api/customer/search?pageNo=" + safePage + "&pageSize=" + safeSize
                            + "&sortBy=id&sortDir=" + sDir + "&citizenId=" + enc;
                } else {
                    path = "/api/customer/search?pageNo=" + safePage + "&pageSize=" + safeSize
                            + "&sortBy=id&sortDir=" + sDir + "&phoneNumber=" + enc;
                }
            } else {
                path = "/api/customer/search?pageNo=" + safePage + "&pageSize=" + safeSize
                        + "&sortBy=id&sortDir=" + sDir + "&fullName=" + enc;
            }
        }
        PageResponse<JsonNode> page = ApiClient.get(path, new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Customer> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }

        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Customer> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                Customer c = new Customer();
                c.id = n.has("id") ? n.get("id").asLong() : null;
                c.fullName = n.has("fullName") ? n.get("fullName").asText() : "";
                c.citizenId = n.has("citizenId") ? n.get("citizenId").asText() : "";
                c.email = n.has("email") ? n.get("email").asText() : "";
                c.phone = n.has("phoneNumber") ? n.get("phoneNumber").asText() : "";
                c.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
                c.active = !n.has("active") || n.get("active").asBoolean();
                list.add(c);
            }
        }
        res.content = list;
        return res;
    }

    public static List<Customer> customers(String query) {
        PageResponse<Customer> page = customerPage(1, 1000, query);
        List<Customer> list = page.getContent();
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            return list.stream()
                    .filter(c -> (c.fullName != null && c.fullName.toLowerCase().contains(q))
                            || (c.code != null && c.code.toLowerCase().contains(q))
                            || (c.citizenId != null && c.citizenId.toLowerCase().contains(q))
                            || (c.email != null && c.email.toLowerCase().contains(q))
                            || (c.phone != null && c.phone.contains(q)))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public static Customer createCustomer(String fullName, String phone, String email, String idNumber) {
        return createCustomer(null, fullName, phone, email, idNumber);
    }

    public static Customer createCustomer(String code, String fullName, String phone, String email, String idNumber) {
        Map<String, Object> req = new HashMap<>();
        req.put("fullName", fullName);
        req.put("citizenId", idNumber != null && !idNumber.isBlank() ? idNumber : "000000000000");
        req.put("email", email != null && !email.isBlank() ? email : "khachhang@hotel.vn");
        req.put("phoneNumber", phone != null && !phone.isBlank() ? phone : "0900000000");

        ApiResponse<JsonNode> resp = ApiClient.post("/api/customer", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Customer c = new Customer();
        c.fullName = fullName;
        c.phone = phone;
        c.email = email;
        c.citizenId = idNumber;
        if (resp != null && resp.result != null && resp.result.has("id")) {
            c.id = resp.result.get("id").asLong();
            c.code = String.format("KH%03d", c.id);
        }
        return c;
    }

    public static Customer updateCustomer(Long id, String code, String fullName, String phone, String email,
            String idNumber) {
        Map<String, Object> req = new HashMap<>();
        req.put("fullName", fullName);
        req.put("citizenId", idNumber != null && !idNumber.isBlank() ? idNumber : "000000000000");
        req.put("email", email != null && !email.isBlank() ? email : "khachhang@hotel.vn");
        req.put("phoneNumber", phone != null && !phone.isBlank() ? phone : "0900000000");

        ApiClient.put("/api/customer/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        Customer c = new Customer();
        c.id = id;
        c.code = code != null && !code.isBlank() ? code : String.format("KH%03d", id);
        c.fullName = fullName;
        c.phone = phone;
        c.email = email;
        c.citizenId = idNumber;
        return c;
    }

    public static void deleteCustomer(Long id) {
        ApiClient.delete("/api/customer/" + id);
    }

    // ============== SERVICES (DỊCH VỤ) ==============
    public static List<ServiceItem> services() {
        PageResponse<JsonNode> page = ApiClient.get("/api/services?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<ServiceItem> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            ServiceItem s = new ServiceItem();
            s.id = n.has("id") ? n.get("id").asLong() : null;
            s.code = s.id != null ? String.format("DV%03d", s.id) : "";
            s.name = n.has("name") ? n.get("name").asText() : "";
            s.price = n.has("price") ? new BigDecimal(n.get("price").asText()) : BigDecimal.ZERO;
            s.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
            s.active = !n.has("active") || n.get("active").asBoolean();
            list.add(s);
        }
        return list;
    }

    public static PageResponse<ServiceItem> servicePage(int pageNo, int pageSize, String keyword, BigDecimal minPrice,
            BigDecimal maxPrice, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/services/search?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (minPrice != null) {
            sb.append("&minPrice=").append(minPrice);
        }
        if (maxPrice != null) {
            sb.append("&maxPrice=").append(maxPrice);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<ServiceItem> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<ServiceItem> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                ServiceItem s = new ServiceItem();
                s.id = n.has("id") ? n.get("id").asLong() : null;
                s.code = s.id != null ? String.format("DV%03d", s.id) : "";
                s.name = n.has("name") ? n.get("name").asText() : "";
                s.price = n.has("price") ? new BigDecimal(n.get("price").asText()) : BigDecimal.ZERO;
                s.imagePath = n.has("imagePath") ? n.get("imagePath").asText() : null;
                s.active = !n.has("active") || n.get("active").asBoolean();
                list.add(s);
            }
        }
        res.content = list;
        return res;
    }

    public static ServiceItem createService(String name, BigDecimal price) {
        return createService(null, name, price);
    }

    public static ServiceItem createService(String code, String name, BigDecimal price) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("price", price);
        req.put("active", true);

        ApiResponse<JsonNode> resp = ApiClient.post("/api/services", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        ServiceItem s = new ServiceItem();
        s.name = name;
        s.price = price;
        if (resp != null && resp.result != null && resp.result.has("id")) {
            s.id = resp.result.get("id").asLong();
            s.code = String.format("DV%03d", s.id);
        }
        return s;
    }

    public static ServiceItem updateService(Long id, String code, String name, BigDecimal price) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("price", price);
        req.put("active", true);

        ApiClient.put("/api/services/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        ServiceItem s = new ServiceItem();
        s.id = id;
        s.code = code != null && !code.isBlank() ? code : String.format("DV%03d", id);
        s.name = name;
        s.price = price;
        return s;
    }

    public static void deleteService(Long id) {
        ApiClient.delete("/api/services/" + id);
    }

    // ============== PROMOTIONS (KHUYẾN MÃI) ==============
    public static List<Promotion> promotions() {
        PageResponse<JsonNode> page = ApiClient.get("/api/promotions?pageNo=1&pageSize=1000&sortBy=id&sortDir=asc",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<Promotion> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            Promotion p = new Promotion();
            p.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
            p.code = n.has("code") && !n.get("code").isNull() ? n.get("code").asText() : "";
            p.name = n.has("name") && !n.get("name").isNull() ? n.get("name").asText() : "";
            p.discountType = n.has("discountType") && !n.get("discountType").isNull() ? n.get("discountType").asText()
                    : "PHAN_TRAM";
            p.discountValue = n.has("discountValue") && !n.get("discountValue").isNull()
                    ? new BigDecimal(n.get("discountValue").asText())
                    : BigDecimal.ZERO;
            if (n.has("startDate") && !n.get("startDate").isNull()) {
                try {
                    p.startDate = LocalDate.parse(n.get("startDate").asText());
                } catch (Exception ignored) {
                }
            }
            if (n.has("endDate") && !n.get("endDate").isNull()) {
                try {
                    p.endDate = LocalDate.parse(n.get("endDate").asText());
                } catch (Exception ignored) {
                }
            }
            p.active = !n.has("active") || n.get("active").asBoolean();
            p.status = n.has("status") && !n.get("status").isNull() ? n.get("status").asText() : null;
            list.add(p);
        }
        return list;
    }

    public static PageResponse<Promotion> promotionsPage(int pageNo, int pageSize, String keyword, String discountType,
            Boolean active, String sortDir) {
        return promotionsPage(pageNo, pageSize, keyword, discountType, active, null, sortDir);
    }

    public static PageResponse<Promotion> promotionsPage(int pageNo, int pageSize, String keyword, String discountType,
            Boolean active, String status, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/promotions/search?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (discountType != null && !discountType.isBlank() && !"Tất cả hình thức".equalsIgnoreCase(discountType)
                && !"ALL".equalsIgnoreCase(discountType)) {
            String dt = "Phần trăm (%)".equalsIgnoreCase(discountType) ? "PHAN_TRAM" : "SO_TIEN";
            sb.append("&discountType=").append(dt);
        }
        if (active != null) {
            sb.append("&active=").append(active);
        }
        if (status != null && !status.isBlank() && !"Tất cả trạng thái".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
            sb.append("&status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8));
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Promotion> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Promotion> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                Promotion p = new Promotion();
                p.id = n.has("id") ? n.get("id").asLong() : null;
                p.code = n.has("code") ? n.get("code").asText() : "";
                p.name = n.has("name") ? n.get("name").asText() : "";
                p.discountType = n.has("discountType") ? n.get("discountType").asText() : "PHAN_TRAM";
                p.discountValue = n.has("discountValue") ? new BigDecimal(n.get("discountValue").asText())
                        : BigDecimal.ZERO;
                p.startDate = n.has("startDate") && !n.get("startDate").isNull()
                        ? LocalDate.parse(n.get("startDate").asText())
                        : null;
                p.endDate = n.has("endDate") && !n.get("endDate").isNull() ? LocalDate.parse(n.get("endDate").asText())
                        : null;
                p.active = !n.has("active") || n.get("active").asBoolean();
                p.status = n.has("status") && !n.get("status").isNull() ? n.get("status").asText() : null;
                list.add(p);
            }
        }
        res.content = list;
        return res;
    }

    public static Promotion createPromotion(String code, String name, String discountType, BigDecimal discountValue,
            LocalDate startDate, LocalDate endDate, Boolean active) {
        Map<String, Object> req = new HashMap<>();
        req.put("code", code);
        req.put("name", name);
        req.put("discountType", discountType != null ? discountType : "PHAN_TRAM");
        req.put("discountValue", discountValue != null ? discountValue : BigDecimal.ZERO);
        req.put("startDate", startDate != null ? startDate.toString() : LocalDate.now().toString());
        req.put("endDate", endDate != null ? endDate.toString() : LocalDate.now().plusMonths(1).toString());
        req.put("active", active != null ? active : true);

        ApiResponse<JsonNode> resp = ApiClient.post("/api/promotions", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Promotion p = new Promotion();
        p.code = code;
        p.name = name;
        p.discountType = discountType;
        p.discountValue = discountValue;
        p.startDate = startDate;
        p.endDate = endDate;
        p.active = active;
        if (resp != null && resp.result != null) {
            JsonNode rNode = resp.result.has("promotion") ? resp.result.get("promotion") : resp.result;
            if (rNode.has("id")) {
                p.id = rNode.get("id").asLong();
            }
        }
        return p;
    }

    public static Promotion updatePromotion(Long id, String code, String name, String discountType,
            BigDecimal discountValue,
            LocalDate startDate, LocalDate endDate, Boolean active) {
        Map<String, Object> req = new HashMap<>();
        req.put("code", code);
        req.put("name", name);
        req.put("discountType", discountType != null ? discountType : "PHAN_TRAM");
        req.put("discountValue", discountValue != null ? discountValue : BigDecimal.ZERO);
        req.put("startDate", startDate != null ? startDate.toString() : LocalDate.now().toString());
        req.put("endDate", endDate != null ? endDate.toString() : LocalDate.now().plusMonths(1).toString());
        req.put("active", active != null ? active : true);

        ApiClient.put("/api/promotions/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        Promotion p = new Promotion();
        p.id = id;
        p.code = code;
        p.name = name;
        p.discountType = discountType;
        p.discountValue = discountValue;
        p.startDate = startDate;
        p.endDate = endDate;
        p.active = active;
        return p;
    }

    public static void deletePromotion(Long id) {
        ApiClient.delete("/api/promotions/" + id);
    }

    // ============== PAYMENT METHODS (PHƯƠNG THỨC THANH TOÁN) ==============
    public static List<PaymentMethodItem> paymentMethods() {
        PageResponse<JsonNode> page = ApiClient.get("/api/payment-methods?pageNo=1&pageSize=1000&sortBy=id&sortDir=asc",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<PaymentMethodItem> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            PaymentMethodItem pm = new PaymentMethodItem();
            pm.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
            pm.name = n.has("name") && !n.get("name").isNull() ? n.get("name").asText() : "";
            pm.qrImage = n.has("qrImage") && !n.get("qrImage").isNull() ? n.get("qrImage").asText() : null;
            pm.bankName = n.has("bankName") && !n.get("bankName").isNull() ? n.get("bankName").asText() : null;
            pm.accountNumber = n.has("accountNumber") && !n.get("accountNumber").isNull()
                    ? n.get("accountNumber").asText()
                    : null;
            pm.accountHolderName = n.has("accountHolderName") && !n.get("accountHolderName").isNull()
                    ? n.get("accountHolderName").asText()
                    : null;
            pm.active = !n.has("active") || n.get("active").asBoolean();
            list.add(pm);
        }
        return list;
    }

    public static PageResponse<PaymentMethodItem> paymentMethodsPage(int pageNo, int pageSize, String keyword,
            Boolean active, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/payment-methods/search?pageNo=").append(safePage)
                .append("&pageSize=").append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (active != null) {
            sb.append("&active=").append(active);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<PaymentMethodItem> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<PaymentMethodItem> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                PaymentMethodItem pm = new PaymentMethodItem();
                pm.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
                pm.name = n.has("name") && !n.get("name").isNull() ? n.get("name").asText() : "";
                pm.qrImage = n.has("qrImage") && !n.get("qrImage").isNull() ? n.get("qrImage").asText() : null;
                pm.bankName = n.has("bankName") && !n.get("bankName").isNull() ? n.get("bankName").asText() : null;
                pm.accountNumber = n.has("accountNumber") && !n.get("accountNumber").isNull()
                        ? n.get("accountNumber").asText()
                        : null;
                pm.accountHolderName = n.has("accountHolderName") && !n.get("accountHolderName").isNull()
                        ? n.get("accountHolderName").asText()
                        : null;
                pm.active = !n.has("active") || n.get("active").asBoolean();
                list.add(pm);
            }
        }
        res.content = list;
        return res;
    }

    public static PaymentMethodItem createPaymentMethod(String name, String bankName, String accountNumber,
            String accountHolderName, String qrImage, Boolean active) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("bankName", bankName != null ? bankName : "");
        req.put("accountNumber", accountNumber != null ? accountNumber : "");
        req.put("accountHolderName", accountHolderName != null ? accountHolderName : "");
        req.put("qrImage", qrImage != null ? qrImage : "");
        req.put("active", active != null ? active : true);

        ApiResponse<JsonNode> resp = ApiClient.post("/api/payment-methods", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        PaymentMethodItem pm = new PaymentMethodItem();
        pm.name = name;
        pm.bankName = bankName;
        pm.accountNumber = accountNumber;
        pm.accountHolderName = accountHolderName;
        pm.qrImage = qrImage;
        pm.active = active;
        if (resp != null && resp.result != null) {
            JsonNode rNode = resp.result.has("paymentMethod") ? resp.result.get("paymentMethod") : resp.result;
            if (rNode.has("id")) {
                pm.id = rNode.get("id").asLong();
            }
        }
        return pm;
    }

    public static PaymentMethodItem updatePaymentMethod(Long id, String name, String bankName, String accountNumber,
            String accountHolderName, String qrImage, Boolean active) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", name);
        req.put("bankName", bankName != null ? bankName : "");
        req.put("accountNumber", accountNumber != null ? accountNumber : "");
        req.put("accountHolderName", accountHolderName != null ? accountHolderName : "");
        req.put("qrImage", qrImage != null ? qrImage : "");
        req.put("active", active != null ? active : true);

        ApiClient.put("/api/payment-methods/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        PaymentMethodItem pm = new PaymentMethodItem();
        pm.id = id;
        pm.name = name;
        pm.bankName = bankName;
        pm.accountNumber = accountNumber;
        pm.accountHolderName = accountHolderName;
        pm.qrImage = qrImage;
        pm.active = active;
        return pm;
    }

    public static void deletePaymentMethod(Long id) {
        ApiClient.delete("/api/payment-methods/" + id);
    }

    // ============== STAFF (NHÂN SỰ) ==============
    public static List<Staff> staff() {
        PageResponse<JsonNode> page = ApiClient.get("/api/staff?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<Staff> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            list.add(mapStaff(n));
        }
        return list;
    }

    private static Staff mapStaff(JsonNode n) {
        Staff s = new Staff();
        s.id = n.hasNonNull("id") ? n.get("id").asLong() : null;
        s.code = s.id != null ? String.format("TK%03d", s.id) : "";
        s.username = n.hasNonNull("username") ? n.get("username").asText() : "";
        s.fullName = n.hasNonNull("fullName") ? n.get("fullName").asText() : "";
        s.email = n.hasNonNull("email") ? n.get("email").asText() : "";
        s.phone = n.hasNonNull("phoneNumber") ? n.get("phoneNumber").asText() : "";
        s.jobTitle = n.hasNonNull("jobTitle") ? n.get("jobTitle").asText() : "";

        if (n.hasNonNull("role")) {
            JsonNode rNode = n.get("role");
            String rText = rNode.isObject() && rNode.hasNonNull("name") ? rNode.get("name").asText() : rNode.asText();
            s.role = (rText != null && !rText.isBlank() && !"null".equalsIgnoreCase(rText)) ? rText : null;
        }
        s.role = Staff.resolveRole(s.role, s.username);

        boolean active = !n.has("active") || n.get("active").asBoolean();
        s.status = active ? "ACTIVE" : "INACTIVE";
        s.shift = null;
        s.createdAt = n.has("createdAt")
                ? n.get("createdAt").asText().substring(0, Math.min(10, n.get("createdAt").asText().length()))
                : "";
        s.profileImage = n.hasNonNull("profileImage") ? n.get("profileImage").asText() : null;

        UserAccount u = new UserAccount();
        u.id = s.id;
        u.username = s.username;
        u.fullName = s.fullName;
        u.email = s.email;
        u.phone = s.phone;
        u.role = s.role;
        u.status = s.status;
        s.user = u;
        return s;
    }

    public static PageResponse<Staff> staffPage(int pageNo, int pageSize, String keyword, String role, Boolean active,
            String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/staff/search?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (role != null && !role.isBlank() && !"Tất cả vai trò".equalsIgnoreCase(role)
                && !"ALL".equalsIgnoreCase(role)) {
            String beRole = mapStaffRoleToBackendEnum(role);
            if (beRole != null) {
                sb.append("&role=").append(beRole);
            }
        }
        if (active != null) {
            sb.append("&active=").append(active);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Staff> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Staff> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapStaff(n));
            }
        }
        res.content = list;
        return res;
    }

    public static String uploadStaffImage(File file) {
        if (file == null || !file.exists())
            return null;
        try {
            ApiResponse<Map<String, String>> resp = ApiClient.uploadFile(
                    "/api/staff/upload-image", file, "file",
                    new TypeReference<ApiResponse<Map<String, String>>>() {
                    });
            if (resp != null && resp.result != null && resp.result.containsKey("imageUrl")) {
                return resp.result.get("imageUrl");
            }
        } catch (Exception e) {
            System.err.println("Upload staff image failed: " + e.getMessage());
        }
        return null;
    }

    public static Staff uploadStaffImage(Long staffId, File file) {
        if (staffId == null || file == null || !file.exists())
            return null;
        try {
            ApiResponse<JsonNode> resp = ApiClient.uploadFile(
                    "/api/staff/" + staffId + "/image", file, "file",
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });
            if (resp != null && resp.result != null) {
                return mapStaff(resp.result);
            }
        } catch (Exception e) {
            System.err.println("Upload staff image failed: " + e.getMessage());
        }
        return null;
    }

    public static Staff createStaff(String username, String password, String fullName,
            String email, String phone, String jobTitle,
            String role, String shift) {
        return createStaff(username, password, fullName, email, phone, jobTitle, role, shift, null);
    }

    public static Staff createStaff(String username, String password, String fullName,
            String email, String phone, String jobTitle,
            String role, String shift, String profileImage) {
        Map<String, Object> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password != null && !password.isBlank() ? password : "password123");
        req.put("fullName", fullName);
        req.put("email", email != null && !email.isBlank() ? email : username + "@hotel.vn");
        req.put("phoneNumber", phone != null && !phone.isBlank() ? phone : "0900000000");
        req.put("jobTitle", jobTitle != null && !jobTitle.isBlank() ? jobTitle : "Nhân viên");
        req.put("role", role != null ? role.toUpperCase() : "RECEPTION");
        req.put("active", true);
        if (profileImage != null && !profileImage.isBlank()) {
            req.put("profileImage", profileImage);
        }

        ApiResponse<JsonNode> resp = ApiClient.post("/api/staff", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Staff s = new Staff();
        s.username = username;
        s.fullName = fullName;
        s.email = email;
        s.phone = phone;
        s.jobTitle = jobTitle;
        s.role = role;
        s.shift = shift;
        s.profileImage = profileImage;
        s.status = "ACTIVE";
        if (resp != null && resp.result != null && resp.result.has("id")) {
            s.id = resp.result.get("id").asLong();
            s.code = String.format("TK%03d", s.id);
        }
        return s;
    }

    public static Staff updateStaff(Long id, String username, String fullName,
            String email, String phone, String jobTitle,
            String role, String shift) {
        return updateStaff(id, username, fullName, email, phone, jobTitle, role, shift, null);
    }

    public static Staff updateStaff(Long id, String username, String fullName,
            String email, String phone, String jobTitle,
            String role, String shift, String profileImage) {
        Map<String, Object> req = new HashMap<>();
        req.put("username", username);
        req.put("fullName", fullName);
        req.put("email", email != null && !email.isBlank() ? email : username + "@hotel.vn");
        req.put("phoneNumber", phone != null && !phone.isBlank() ? phone : "0900000000");
        req.put("jobTitle", jobTitle != null && !jobTitle.isBlank() ? jobTitle : "Nhân viên");
        req.put("role", role != null ? role.toUpperCase() : "RECEPTION");
        req.put("active", true);
        if (profileImage != null && !profileImage.isBlank()) {
            req.put("profileImage", profileImage);
        }

        ApiClient.put("/api/staff/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        Staff s = new Staff();
        s.id = id;
        s.code = String.format("TK%03d", id);
        s.username = username;
        s.fullName = fullName;
        s.email = email;
        s.phone = phone;
        s.jobTitle = jobTitle;
        s.role = role;
        s.shift = shift;
        s.profileImage = profileImage;
        s.status = "ACTIVE";
        return s;
    }

    public static Staff toggleStaffStatus(Long id) {
        ApiResponse<JsonNode> current = ApiClient.get("/api/staff/" + id, new TypeReference<ApiResponse<JsonNode>>() {
        });
        if (current != null && current.result != null) {
            JsonNode n = current.result;
            boolean active = !n.has("active") || n.get("active").asBoolean();
            Map<String, Object> req = new HashMap<>();
            req.put("username", n.has("username") ? n.get("username").asText() : "");
            req.put("fullName", n.has("fullName") ? n.get("fullName").asText() : "");
            req.put("email", n.has("email") ? n.get("email").asText() : "");
            req.put("phoneNumber", n.has("phoneNumber") ? n.get("phoneNumber").asText() : "0900000000");
            req.put("jobTitle", n.has("jobTitle") ? n.get("jobTitle").asText() : "Nhân viên");
            req.put("role",
                    n.has("role")
                            ? (n.get("role").isObject() ? n.get("role").get("name").asText() : n.get("role").asText())
                            : "RECEPTION");
            req.put("active", !active);

            ApiClient.put("/api/staff/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
            });

            Staff s = new Staff();
            s.id = id;
            s.status = !active ? "ACTIVE" : "INACTIVE";
            return s;
        }
        return null;
    }

    public static void deleteStaff(Long id) {
        ApiClient.delete("/api/staff/" + id);
    }

    public static Staff createStaff(String fullName, String jobTitle, String shift, String phone, Long userId) {
        return createStaff("user" + System.currentTimeMillis() % 10000, "123456", fullName, "", phone, jobTitle,
                "RECEPTION", shift);
    }

    // ============== WORK SHIFTS (CA LÀM VIỆC) ==============
    public static List<WorkShift> workShifts() {
        PageResponse<JsonNode> page = ApiClient.get("/api/workshift?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<WorkShift> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            list.add(mapWorkShift(n));
        }
        return list;
    }

    private static WorkShift mapWorkShift(JsonNode n) {
        WorkShift ws = new WorkShift();
        ws.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;

        if (n.hasNonNull("shiftName") && !n.get("shiftName").asText().trim().isEmpty()) {
            ws.shiftName = n.get("shiftName").asText().trim();
        } else if (n.hasNonNull("name") && !n.get("name").asText().trim().isEmpty()) {
            ws.shiftName = n.get("name").asText().trim();
        } else if (n.hasNonNull("tenCa") && !n.get("tenCa").asText().trim().isEmpty()) {
            ws.shiftName = n.get("tenCa").asText().trim();
        } else {
            ws.shiftName = "";
        }

        ws.startTime = n.has("startTime") ? formatTimeNode(n.get("startTime")) : "06:00";
        ws.endTime = n.has("endTime") ? formatTimeNode(n.get("endTime")) : "14:00";
        ws.active = !n.has("active") || n.get("active").asBoolean();
        return ws;
    }

    public static PageResponse<WorkShift> workShiftPage(int pageNo, int pageSize, String keyword, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/workshift?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<WorkShift> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<WorkShift> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapWorkShift(n));
            }
        }
        res.content = list;
        return res;
    }

    public static WorkShift createWorkShift(String shiftName, String startTime, String endTime) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", shiftName);
        req.put("shiftName", shiftName);
        req.put("startTime", ensureTimeSeconds(startTime));
        req.put("endTime", ensureTimeSeconds(endTime));

        ApiResponse<JsonNode> resp = ApiClient.post("/api/workshift", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        WorkShift ws = new WorkShift();
        ws.shiftName = shiftName;
        ws.startTime = startTime;
        ws.endTime = endTime;
        ws.active = true;
        if (resp != null && resp.result != null) {
            JsonNode r = resp.result;
            if (r.has("Ca làm việc:")) {
                r = r.get("Ca làm việc:");
            }
            if (r.has("id")) {
                ws.id = r.get("id").asLong();
            }
        }
        return ws;
    }

    public static WorkShift updateWorkShift(Long id, String shiftName, String startTime, String endTime) {
        Map<String, Object> req = new HashMap<>();
        req.put("name", shiftName);
        req.put("shiftName", shiftName);
        req.put("startTime", ensureTimeSeconds(startTime));
        req.put("endTime", ensureTimeSeconds(endTime));

        ApiClient.put("/api/workshift/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        WorkShift ws = new WorkShift();
        ws.id = id;
        ws.shiftName = shiftName;
        ws.startTime = startTime;
        ws.endTime = endTime;
        ws.active = true;
        return ws;
    }

    public static void deleteWorkShift(Long id) {
        ApiClient.delete("/api/workshift/" + id);
    }

    // ============== SHIFT ASSIGNMENTS (PHÂN CA) ==============
    public static PageResponse<ShiftAssignment> shiftAssignmentPage(int pageNo, int pageSize, String staffName,
            Long workShiftId, String workDate) {
        return shiftAssignmentPage(pageNo, pageSize, staffName, workShiftId, workDate, "asc");
    }

    public static PageResponse<ShiftAssignment> shiftAssignmentPage(int pageNo, int pageSize, String staffName,
            Long workShiftId, String workDate, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        boolean hasFilter = (staffName != null && !staffName.isBlank())
                || workShiftId != null
                || (workDate != null && !workDate.isBlank());

        String basePath = hasFilter ? "/api/shift-assignments/search" : "/api/shift-assignments";
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder(basePath).append("?pageNo=").append(safePage)
                .append("&pageSize=").append(safeSize)
                .append("&sortBy=id&sortDir=").append(sDir);

        if (staffName != null && !staffName.isBlank()) {
            sb.append("&staffName=")
                    .append(java.net.URLEncoder.encode(staffName.trim(), java.nio.charset.StandardCharsets.UTF_8));
        }
        if (workShiftId != null) {
            sb.append("&workShiftId=").append(workShiftId);
        }
        if (workDate != null && !workDate.isBlank()) {
            sb.append("&workDate=").append(formatToIsoDate(workDate.trim()));
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(),
                new TypeReference<PageResponse<JsonNode>>() {
                });

        PageResponse<ShiftAssignment> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }

        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Attendance> attendanceList = attendances();
        List<ShiftAssignment> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                ShiftAssignment sa = new ShiftAssignment();
                sa.id = n.has("id") ? n.get("id").asLong() : null;

                Staff s = new Staff();
                if (n.has("staffId") && !n.get("staffId").isNull()) {
                    s.id = n.get("staffId").asLong();
                }
                s.fullName = n.has("staffName") ? n.get("staffName").asText() : "";
                sa.staff = s;

                WorkShift ws = new WorkShift();
                if (n.has("workShiftId") && !n.get("workShiftId").isNull()) {
                    ws.id = n.get("workShiftId").asLong();
                }
                ws.shiftName = n.has("shiftName") ? n.get("shiftName").asText() : "";
                ws.startTime = n.has("startTime") ? formatTimeNode(n.get("startTime")) : "";
                ws.endTime = n.has("endTime") ? formatTimeNode(n.get("endTime")) : "";
                sa.workShift = ws;

                String dateStr = n.has("workDate") ? n.get("workDate").asText() : "";
                sa.workDate = formatToVnDate(dateStr);

                sa.attendance = attendanceList.stream()
                        .filter(a -> a.shiftAssignment != null && Objects.equals(a.shiftAssignment.id, sa.id))
                        .findFirst()
                        .orElse(null);

                list.add(sa);
            }
        }
        res.content = list;
        return res;
    }

    public static List<ShiftAssignment> shiftAssignments() {
        PageResponse<ShiftAssignment> p = shiftAssignmentPage(1, 1000, null, null, null);
        return p != null && p.getContent() != null ? p.getContent() : new ArrayList<>();
    }

    public static ShiftAssignment createShiftAssignment(Long staffId, Long workShiftId, String workDate) {
        Map<String, Object> req = new HashMap<>();
        req.put("staffId", staffId);
        req.put("workShiftId", workShiftId);
        req.put("workDate", formatToIsoDate(workDate));

        ApiResponse<JsonNode> resp = ApiClient.post("/api/shift-assignments", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        ShiftAssignment sa = new ShiftAssignment();
        sa.workDate = workDate;
        if (resp != null && resp.result != null) {
            JsonNode n = resp.result.has("shiftAssignment") ? resp.result.get("shiftAssignment") : resp.result;
            if (n.has("id"))
                sa.id = n.get("id").asLong();
        }
        return sa;
    }

    public static ShiftAssignment updateShiftAssignment(Long id, Long staffId, Long workShiftId, String workDate) {
        Map<String, Object> req = new HashMap<>();
        req.put("staffId", staffId);
        req.put("workShiftId", workShiftId);
        req.put("workDate", formatToIsoDate(workDate));

        ApiResponse<JsonNode> resp = ApiClient.put("/api/shift-assignments/" + id, req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        ShiftAssignment sa = new ShiftAssignment();
        sa.id = id;
        sa.workDate = workDate;
        if (resp != null && resp.result != null) {
            JsonNode n = resp.result.has("shiftAssignment") ? resp.result.get("shiftAssignment") : resp.result;
            if (n.has("id"))
                sa.id = n.get("id").asLong();
        }
        return sa;
    }

    public static void deleteShiftAssignment(Long id) {
        ApiClient.delete("/api/shift-assignments/" + id);
    }

    // ============== ATTENDANCES (CHẤM CÔNG) ==============
    public static List<Attendance> attendances() {
        PageResponse<JsonNode> page = ApiClient.get("/api/attendances?pageNo=1&pageSize=1000",
                new TypeReference<PageResponse<JsonNode>>() {
                });
        if (page == null || page.getContent() == null)
            return new ArrayList<>();

        List<Attendance> list = new ArrayList<>();
        for (JsonNode n : page.getContent()) {
            list.add(parseAttendanceNode(n));
        }
        return list;
    }

    public static PageResponse<Attendance> attendancePage(
            int pageNo, int pageSize, String staffName, Long workShiftId, String workDate, String status) {
        return attendancePage(pageNo, pageSize, staffName, workShiftId, workDate, status, "asc");
    }

    public static PageResponse<Attendance> attendancePage(
            int pageNo, int pageSize, String staffName, Long workShiftId, String workDate, String status,
            String sortDir) {
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder url = new StringBuilder(
                "/api/attendances/search?pageNo=" + pageNo + "&pageSize=" + pageSize + "&sortBy=id&sortDir=" + sDir);
        if (staffName != null && !staffName.isBlank()) {
            url.append("&staffName=").append(URLEncoder.encode(staffName.trim(), StandardCharsets.UTF_8));
        }
        if (workShiftId != null) {
            url.append("&workShiftId=").append(workShiftId);
        }
        if (workDate != null && !workDate.isBlank()) {
            url.append("&workDate=").append(workDate.trim());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            url.append("&status=")
                    .append(URLEncoder.encode(mapFeAttendanceStatusToVietnamese(status), StandardCharsets.UTF_8));
        }

        PageResponse<JsonNode> page = ApiClient.get(url.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        if (page == null)
            return new PageResponse<>();

        PageResponse<Attendance> res = new PageResponse<>();
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = page.totalPages;
        res.last = page.last;

        List<Attendance> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(parseAttendanceNode(n));
            }
        }
        res.content = list;
        return res;
    }

    private static Attendance parseAttendanceNode(JsonNode n) {
        Attendance a = new Attendance();
        a.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
        a.checkInTime = n.has("checkInTime") && !n.get("checkInTime").isNull()
                ? formatTime(n.get("checkInTime").asText())
                : null;
        a.checkOutTime = n.has("checkOutTime") && !n.get("checkOutTime").isNull()
                ? formatTime(n.get("checkOutTime").asText())
                : null;
        a.status = n.has("status") && !n.get("status").isNull() ? mapBackendAttendanceStatus(n.get("status").asText())
                : "DU_CONG";
        a.note = n.has("note") && !n.get("note").isNull() ? n.get("note").asText() : "";

        ShiftAssignment sa = new ShiftAssignment();
        if (n.has("shiftAssignmentId") && !n.get("shiftAssignmentId").isNull()) {
            sa.id = n.get("shiftAssignmentId").asLong();
        }
        Staff st = new Staff();
        st.fullName = n.has("staffName") && !n.get("staffName").isNull() ? n.get("staffName").asText() : "";
        sa.staff = st;

        WorkShift ws = new WorkShift();
        if (n.has("workShiftName") && !n.get("workShiftName").isNull()) {
            ws.shiftName = n.get("workShiftName").asText();
        } else if (n.has("shiftName") && !n.get("shiftName").isNull()) {
            ws.shiftName = n.get("shiftName").asText();
        }
        sa.workShift = ws;

        if (n.has("workDate") && !n.get("workDate").isNull()) {
            sa.workDate = n.get("workDate").asText();
        }
        a.shiftAssignment = sa;
        return a;
    }

    public static Attendance createAttendance(Long shiftAssignmentId, String checkIn, String checkOut, String status,
            String note) {
        Map<String, Object> req = new HashMap<>();
        req.put("shiftAssignmentId", shiftAssignmentId);
        if (checkIn != null && !checkIn.isBlank())
            req.put("checkInTime", ensureTimeSeconds(checkIn));
        if (checkOut != null && !checkOut.isBlank())
            req.put("checkOutTime", ensureTimeSeconds(checkOut));
        req.put("status", mapFeAttendanceStatusToVietnamese(status));
        req.put("note", note != null ? note : "");

        ApiResponse<JsonNode> resp = ApiClient.post("/api/attendances", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Attendance a = new Attendance();
        a.checkInTime = checkIn;
        a.checkOutTime = checkOut;
        a.status = status;
        a.note = note;
        if (resp != null && resp.result != null) {
            JsonNode n = resp.result.has("attendance") ? resp.result.get("attendance") : resp.result;
            if (n.has("id"))
                a.id = n.get("id").asLong();
        }
        return a;
    }

    public static Attendance updateAttendance(Long id, String checkIn, String checkOut, String status, String note) {
        Map<String, Object> req = new HashMap<>();
        if (checkIn != null && !checkIn.isBlank())
            req.put("checkInTime", ensureTimeSeconds(checkIn));
        if (checkOut != null && !checkOut.isBlank())
            req.put("checkOutTime", ensureTimeSeconds(checkOut));
        req.put("status", mapFeAttendanceStatusToVietnamese(status));
        req.put("note", note != null ? note : "");

        ApiClient.put("/api/attendances/" + id, req, new TypeReference<ApiResponse<JsonNode>>() {
        });

        Attendance a = new Attendance();
        a.id = id;
        a.checkInTime = checkIn;
        a.checkOutTime = checkOut;
        a.status = status;
        a.note = note;
        return a;
    }

    public static void deleteAttendance(Long id) {
        ApiClient.delete("/api/attendances/" + id);
    }

    // ============== USERS (TÀI KHOẢN HỆ THỐNG) ==============
    public static List<UserAccount> users() {
        return staff().stream().map(s -> s.user).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static UserAccount createUser(String username, String password, String email, String phone, String role) {
        Staff s = createStaff(username, password, username, email, phone, "Nhân viên", role, "Toàn thời gian");
        return s.user;
    }

    public static UserAccount toggleUserStatus(Long id) {
        Staff s = toggleStaffStatus(id);
        return s != null ? s.user : null;
    }

    // ============== BOOKINGS (ĐẶT PHÒNG & LƯU TRÚ) ==============
    public static List<Booking> bookings() {
        try {
            PageResponse<JsonNode> page = ApiClient.get("/api/booking?pageNo=1&pageSize=1000&sortBy=id&sortDir=desc",
                    new TypeReference<PageResponse<JsonNode>>() {
                    });
            if (page == null || page.getContent() == null)
                return new ArrayList<>();

            List<Booking> list = new ArrayList<>();
            for (JsonNode n : page.getContent()) {
                list.add(mapBooking(n));
            }
            return list;
        } catch (Exception ex) {
            System.err.println("Lỗi khi tải danh sách đặt phòng: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private static Booking mapBooking(JsonNode n) {
        Booking b = new Booking();
        b.id = n.hasNonNull("id") ? n.get("id").asLong() : null;
        b.code = b.id != null ? String.format("DP%03d", b.id) : "DP000";

        Customer c = new Customer();
        c.id = n.hasNonNull("customerId") ? n.get("customerId").asLong() : null;
        c.fullName = n.hasNonNull("customerName") ? n.get("customerName").asText() : "";
        c.phone = n.hasNonNull("customerPhone") ? n.get("customerPhone").asText() : "";
        c.code = c.id != null ? String.format("KH%03d", c.id) : "";
        b.customer = c;

        Room r = new Room();
        r.id = n.hasNonNull("roomId") ? n.get("roomId").asLong() : null;
        r.number = n.hasNonNull("roomNumber") ? n.get("roomNumber").asText() : "";
        r.code = "P" + r.number;
        RoomType rt = new RoomType();
        rt.name = n.hasNonNull("roomTypeName") ? n.get("roomTypeName").asText() : "Tiêu chuẩn";
        r.roomType = rt;
        b.room = r;

        b.checkIn = n.hasNonNull("checkInDate") ? formatToVnDate(n.get("checkInDate").asText()) : "—";
        b.checkOutPlanned = n.hasNonNull("expectedCheckOutDate")
                ? formatToVnDate(n.get("expectedCheckOutDate").asText())
                : "—";
        b.actualCheckIn = n.hasNonNull("actualCheckInTime") ? formatDateTimeNode(n.get("actualCheckInTime")) : null;
        b.actualCheckOut = n.hasNonNull("actualCheckOutTime") ? formatDateTimeNode(n.get("actualCheckOutTime")) : null;
        b.numberOfGuests = n.hasNonNull("numberOfGuests") ? n.get("numberOfGuests").asInt() : 1;
        b.status = n.hasNonNull("status") ? n.get("status").asText() : "DAT_TRUOC";
        b.notes = n.hasNonNull("note") ? n.get("note").asText() : "";
        return b;
    }

    public static PageResponse<Booking> bookingPage(int pageNo, int pageSize, String keyword, String status,
            String sortDir) {
        return bookingPage(pageNo, pageSize, keyword, status, sortDir, null, null);
    }

    public static PageResponse<Booking> bookingPage(int pageNo, int pageSize, String keyword, String status,
            String sortDir, Long roomTypeId, Long roomId) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("asc")) ? "asc" : "desc";
        StringBuilder sb = new StringBuilder("/api/booking/search?pageNo=").append(safePage).append("&pageSize=")
                .append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (status != null && !status.isBlank()) {
            sb.append("&status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8));
        }
        if (roomTypeId != null) {
            sb.append("&roomTypeId=").append(roomTypeId);
        }
        if (roomId != null) {
            sb.append("&roomId=").append(roomId);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Booking> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Booking> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapBooking(n));
            }
        }
        res.content = list;
        return res;
    }

    public static Booking updateBooking(Long id, Long customerId, Long roomId,
            String inDate, String outDate, int guests,
            String notes, String status) {
        Map<String, Object> req = new HashMap<>();
        req.put("customerId", customerId);
        req.put("roomId", roomId);
        req.put("checkInDate", inDate);
        req.put("expectedCheckOutDate", outDate);
        req.put("numberOfGuests", guests);
        req.put("note", notes != null ? notes : "");
        if (status != null && !status.isBlank()) {
            req.put("status", status);
        }

        ApiResponse<JsonNode> resp = ApiClient.put("/api/booking/" + id, req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        if (resp != null && resp.result != null) {
            return mapBooking(resp.result);
        }
        return null;
    }

    public static Booking createBooking(Long customerId, String newCustomerName, String newCustomerPhone,
            Long roomId, String checkIn, String checkOutPlanned, boolean checkInNow) {
        return createBooking(customerId, newCustomerName, newCustomerPhone, null, roomId, checkIn, checkOutPlanned, 1,
                "", checkInNow);
    }

    public static Booking createBooking(Long customerId, String newCustomerName, String newCustomerPhone,
            String newCustomerCitizenId,
            Long roomId, String checkIn, String checkOutPlanned, Integer guests, String notes, boolean checkInNow) {
        Long effectiveCustomerId = customerId;
        if (effectiveCustomerId == null && newCustomerName != null && !newCustomerName.isBlank()) {
            Customer c = createCustomer(newCustomerName.trim(),
                    newCustomerPhone != null ? newCustomerPhone.trim() : "",
                    "guest." + System.currentTimeMillis() % 100000 + "@hotel.vn",
                    newCustomerCitizenId != null && !newCustomerCitizenId.isBlank() ? newCustomerCitizenId.trim()
                            : "000000000000");
            effectiveCustomerId = c.id;
        }

        if (effectiveCustomerId == null) {
            throw new ApiClient.ApiException("Vui lòng chọn hoặc nhập thông tin khách hàng.");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("customerId", effectiveCustomerId);
        req.put("roomId", roomId);
        req.put("checkInDate", formatToIsoDate(checkIn));
        req.put("expectedCheckOutDate", formatToIsoDate(checkOutPlanned));
        req.put("numberOfGuests", guests != null && guests > 0 ? guests : 1);
        req.put("note", notes != null ? notes : "");
        req.put("status", checkInNow ? "DANG_O" : "DAT_TRUOC");

        ApiResponse<JsonNode> resp = ApiClient.post("/api/booking", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        Booking b = new Booking();
        if (resp != null && resp.result != null) {
            JsonNode n = resp.result;
            b.id = n.hasNonNull("id") ? n.get("id").asLong() : null;
            b.code = b.id != null ? String.format("DP%03d", b.id) : "DP000";
            b.status = n.hasNonNull("status") ? n.get("status").asText() : (checkInNow ? "DANG_O" : "DAT_TRUOC");
        }
        return b;
    }

    public static Booking checkInBooking(Long bookingId, LocalDateTime actualCheckInTime, LocalDate actualCheckOutDate, String note) {
        Map<String, Object> req = new HashMap<>();
        req.put("bookingId", bookingId);
        if (actualCheckInTime != null) {
            req.put("actualCheckInTime", actualCheckInTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        } else {
            req.put("actualCheckInTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        }
        if (actualCheckOutDate != null) {
            req.put("actualCheckOutDate", actualCheckOutDate.toString());
            req.put("expectedCheckOutDate", actualCheckOutDate.toString());
        }
        if (note != null && !note.isBlank()) {
            req.put("note", note.trim());
        }

        ApiResponse<JsonNode> resp = ApiClient.post("/api/booking/checkin", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });

        if (resp != null && resp.result != null) {
            return mapBooking(resp.result);
        }
        Booking b = new Booking();
        b.id = bookingId;
        b.status = "DANG_O";
        return b;
    }

    public static Booking checkIn(Long bookingId) {
        return checkInBooking(bookingId, LocalDateTime.now(), null, null);
    }

    public static Booking checkOutBooking(Long bookingId) {
        ApiResponse<JsonNode> resp = ApiClient.post("/api/booking/" + bookingId + "/check-out", Collections.emptyMap(),
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        Booking b = new Booking();
        b.id = bookingId;
        b.status = "DA_TRA";
        return b;
    }

    public static Booking cancelBooking(Long bookingId) {
        ApiResponse<JsonNode> resp = ApiClient.post("/api/booking/" + bookingId + "/cancel", Collections.emptyMap(),
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        Booking b = new Booking();
        b.id = bookingId;
        b.status = "DA_HUY";
        return b;
    }

    public static void deleteBooking(Long bookingId) {
        ApiClient.delete("/api/booking/" + bookingId);
    }

    // ============== INVOICES (HÓA ĐƠN) ==============
    public static List<Invoice> invoices() {
        try {
            ApiResponse<List<JsonNode>> resp = ApiClient.get("/api/invoices/all",
                    new TypeReference<ApiResponse<List<JsonNode>>>() {
                    });
            if (resp != null && resp.result != null) {
                List<Invoice> list = new ArrayList<>();
                for (JsonNode n : resp.result) {
                    list.add(mapInvoice(n));
                }
                return list;
            }
        } catch (Exception ignored) {
        }

        try {
            PageResponse<JsonNode> page = ApiClient.get("/api/invoices?pageNo=1&pageSize=1000&sortBy=id&sortDir=desc",
                    new TypeReference<PageResponse<JsonNode>>() {
                    });
            if (page != null && page.getContent() != null) {
                List<Invoice> list = new ArrayList<>();
                for (JsonNode n : page.getContent()) {
                    list.add(mapInvoice(n));
                }
                return list;
            }
        } catch (Exception ignored) {
        }

        return new ArrayList<>();
    }

    public static PageResponse<Invoice> invoicePage(int pageNo, int pageSize, String keyword, String status,
            LocalDate fromDate, LocalDate toDate, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("asc")) ? "asc" : "desc";
        StringBuilder sb = new StringBuilder("/api/invoices?pageNo=").append(safePage)
                .append("&pageSize=").append(safeSize)
                .append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (status != null && !status.isBlank() && !"Tất cả thanh toán".equalsIgnoreCase(status)
                && !"ALL".equalsIgnoreCase(status)) {
            String beStatus = status;
            if ("Đã thanh toán".equalsIgnoreCase(status))
                beStatus = "DA_THANH_TOAN";
            else if ("Thanh toán một phần".equalsIgnoreCase(status))
                beStatus = "THANH_TOAN_MOT_PHAN";
            else if ("Chưa thanh toán".equalsIgnoreCase(status))
                beStatus = "CHUA_THANH_TOAN";
            else if ("Đã hủy".equalsIgnoreCase(status))
                beStatus = "DA_HUY";
            sb.append("&status=").append(URLEncoder.encode(beStatus, StandardCharsets.UTF_8));
        }
        if (fromDate != null) {
            sb.append("&fromDate=").append(fromDate);
        }
        if (toDate != null) {
            sb.append("&toDate=").append(toDate);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<Invoice> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<Invoice> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapInvoice(n));
            }
        }
        res.content = list;
        return res;
    }

    public static Invoice getInvoiceById(Long id) {
        ApiResponse<JsonNode> resp = ApiClient.get("/api/invoices/" + id,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        if (resp != null && resp.result != null) {
            return mapInvoice(resp.result);
        }
        return null;
    }

    public static PaymentTransaction payInvoice(Long invoiceId, Long paymentMethodId, BigDecimal amount,
            String transactionCode, String note) {
        Map<String, Object> req = new HashMap<>();
        req.put("invoiceId", invoiceId);
        req.put("paymentMethodId", paymentMethodId);
        req.put("amount", amount != null ? amount : BigDecimal.ZERO);
        req.put("transactionCode", transactionCode != null ? transactionCode : "");
        if (note != null && !note.isBlank()) {
            req.put("note", note.trim());
        }

        ApiResponse<JsonNode> resp = ApiClient.post("/api/invoices/" + invoiceId + "/pay", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        if (resp != null && resp.result != null) {
            return mapPaymentTransaction(resp.result);
        }
        return null;
    }

    public static PaymentTransaction payInvoice(Long invoiceId, Long paymentMethodId, BigDecimal amount,
            String transactionCode) {
        return payInvoice(invoiceId, paymentMethodId, amount, transactionCode, null);
    }

    public static Invoice createInvoice(Long bookingId, BigDecimal roomFee, BigDecimal serviceFee,
            BigDecimal penaltyFee,
            BigDecimal discountFee, Long promoId, String promoCode, Long paymentMethodId,
            BigDecimal paidAmount, String transactionCode, String dateStr, boolean autoCheckout) {
        Map<String, Object> req = new HashMap<>();
        req.put("bookingId", bookingId);
        req.put("roomAmount", roomFee != null ? roomFee : BigDecimal.ZERO);
        req.put("serviceAmount", serviceFee != null ? serviceFee : BigDecimal.ZERO);
        req.put("penaltyAmount", penaltyFee != null ? penaltyFee : BigDecimal.ZERO);
        if (discountFee != null)
            req.put("discountAmount", discountFee);
        if (promoId != null)
            req.put("promotionId", promoId);
        if (promoCode != null)
            req.put("promotionCode", promoCode);
        if (paymentMethodId != null)
            req.put("paymentMethodId", paymentMethodId);
        if (paidAmount != null)
            req.put("initialPaidAmount", paidAmount);
        if (transactionCode != null)
            req.put("transactionCode", transactionCode);
        if (dateStr != null && !dateStr.isBlank())
            req.put("issuedDate", dateStr);
        if (Session.getUserId() != null)
            req.put("staffId", Session.getUserId());
        req.put("autoCheckOut", autoCheckout);
        req.put("autoCheckout", autoCheckout);

        ApiResponse<JsonNode> resp = ApiClient.post("/api/invoices", req,
                new TypeReference<ApiResponse<JsonNode>>() {
                });
        if (resp != null && resp.result != null) {
            return mapInvoice(resp.result);
        }
        return null;
    }

    public static Invoice checkout(Long bookingId, Long roomId, Long customerId, String checkOutDate,
            BigDecimal roomFee, BigDecimal penaltyFee, List<Map<String, Object>> services) {
        Map<String, Object> req = new HashMap<>();
        req.put("bookingId", bookingId);
        req.put("roomId", roomId);
        req.put("customerId", customerId);
        if (Session.getUserId() != null)
            req.put("staffId", Session.getUserId());
        req.put("roomAmount", roomFee != null ? roomFee : BigDecimal.ZERO);
        req.put("penaltyAmount", penaltyFee != null ? penaltyFee : BigDecimal.ZERO);
        req.put("autoCheckOut", true);
        req.put("autoCheckout", true);

        try {
            ApiResponse<JsonNode> resp = ApiClient.post("/api/invoices/checkout", req,
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });
            if (resp != null && resp.result != null) {
                return mapInvoice(resp.result);
            }
        } catch (Exception e) {
            System.err.println("Checkout invoice error: " + e.getMessage());
        }

        if (roomId != null) {
            updateRoomStatus(roomId, "DANG_DON");
        }
        Invoice inv = new Invoice();
        inv.code = "HD" + (System.currentTimeMillis() % 1000);
        inv.paymentStatus = "DA_THANH_TOAN";
        inv.total = (roomFee != null ? roomFee : BigDecimal.ZERO)
                .add(penaltyFee != null ? penaltyFee : BigDecimal.ZERO);
        inv.paidAmount = inv.total;
        inv.remainingAmount = BigDecimal.ZERO;
        return inv;
    }

    public static void deleteInvoice(Long id) {
        try {
            ApiClient.delete("/api/invoices/" + id);
        } catch (Exception e) {
            System.err.println("Xóa hóa đơn thất bại: " + e.getMessage());
        }
    }

    private static Invoice mapInvoice(JsonNode n) {
        Invoice inv = new Invoice();
        inv.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
        inv.code = n.has("code") && !n.get("code").isNull() ? n.get("code").asText() : "";
        inv.stayNights = n.has("stayNights") && !n.get("stayNights").isNull() ? n.get("stayNights").asLong() : 1L;

        inv.roomFee = n.has("roomAmount") && !n.get("roomAmount").isNull()
                ? new BigDecimal(n.get("roomAmount").asText())
                : (n.has("roomFee") && !n.get("roomFee").isNull() ? new BigDecimal(n.get("roomFee").asText())
                        : BigDecimal.ZERO);
        inv.serviceFee = n.has("serviceAmount") && !n.get("serviceAmount").isNull()
                ? new BigDecimal(n.get("serviceAmount").asText())
                : (n.has("serviceFee") && !n.get("serviceFee").isNull() ? new BigDecimal(n.get("serviceFee").asText())
                        : BigDecimal.ZERO);
        inv.penaltyFee = n.has("penaltyAmount") && !n.get("penaltyAmount").isNull()
                ? new BigDecimal(n.get("penaltyAmount").asText())
                : (n.has("penaltyFee") && !n.get("penaltyFee").isNull() ? new BigDecimal(n.get("penaltyFee").asText())
                        : BigDecimal.ZERO);
        inv.discountFee = n.has("discountAmount") && !n.get("discountAmount").isNull()
                ? new BigDecimal(n.get("discountAmount").asText())
                : (n.has("discountFee") && !n.get("discountFee").isNull()
                        ? new BigDecimal(n.get("discountFee").asText())
                        : BigDecimal.ZERO);
        inv.total = n.has("totalAmount") && !n.get("totalAmount").isNull()
                ? new BigDecimal(n.get("totalAmount").asText())
                : (n.has("total") && !n.get("total").isNull() ? new BigDecimal(n.get("total").asText())
                        : BigDecimal.ZERO);
        inv.paidAmount = n.has("paidAmount") && !n.get("paidAmount").isNull()
                ? new BigDecimal(n.get("paidAmount").asText())
                : BigDecimal.ZERO;
        inv.remainingAmount = n.has("remainingAmount") && !n.get("remainingAmount").isNull()
                ? new BigDecimal(n.get("remainingAmount").asText())
                : BigDecimal.ZERO;

        inv.promotionCode = n.hasNonNull("promotionCode") ? n.get("promotionCode").asText() : null;
        inv.promotionName = n.hasNonNull("promotionName") ? n.get("promotionName").asText() : null;
        inv.discountType = n.hasNonNull("discountType") ? n.get("discountType").asText() : null;
        inv.discountValue = n.hasNonNull("discountValue") ? new BigDecimal(n.get("discountValue").asText()) : null;

        inv.paymentStatus = n.hasNonNull("paymentStatus") ? n.get("paymentStatus").asText() : "CHUA_THANH_TOAN";
        inv.createdAt = n.hasNonNull("createdAt") ? n.get("createdAt").asText()
                : (n.hasNonNull("issuedDate") ? n.get("issuedDate").asText() : "");
        inv.modifiedAt = n.hasNonNull("modifiedAt") ? n.get("modifiedAt").asText() : "";
        inv.issuedDate = inv.createdAt;

        inv.staffId = n.hasNonNull("staffId") ? n.get("staffId").asLong() : null;
        inv.staffName = n.hasNonNull("staffName") ? n.get("staffName").asText() : null;

        Booking b = new Booking();
        b.id = n.hasNonNull("bookingId") ? n.get("bookingId").asLong() : null;
        b.code = n.hasNonNull("bookingCode") ? n.get("bookingCode").asText() : (b.id != null ? "DP" + b.id : "");
        b.checkIn = n.hasNonNull("checkInDate") ? n.get("checkInDate").asText() : null;
        b.checkOutPlanned = n.hasNonNull("expectedCheckOutDate") ? n.get("expectedCheckOutDate").asText() : null;
        inv.booking = b;

        Customer c = new Customer();
        c.id = n.hasNonNull("customerId") ? n.get("customerId").asLong() : null;
        c.fullName = n.hasNonNull("customerName") ? n.get("customerName").asText() : "Khách vãng lai";
        c.phone = n.hasNonNull("customerPhone") ? n.get("customerPhone").asText() : "";
        c.citizenId = n.hasNonNull("customerCitizenId") ? n.get("customerCitizenId").asText() : "";
        inv.customer = c;

        Room r = new Room();
        r.id = n.hasNonNull("roomId") ? n.get("roomId").asLong() : null;
        r.number = n.hasNonNull("roomNumber") ? n.get("roomNumber").asText() : "";
        RoomType rt = new RoomType();
        rt.name = n.hasNonNull("roomTypeName") ? n.get("roomTypeName").asText() : "Tiêu chuẩn";
        rt.basePrice = n.hasNonNull("roomBasePrice") ? new BigDecimal(n.get("roomBasePrice").asText())
                : BigDecimal.ZERO;
        r.roomType = rt;
        inv.room = r;

        if (n.has("payments") && n.get("payments").isArray()) {
            for (JsonNode pNode : n.get("payments")) {
                inv.payments.add(mapPaymentTransaction(pNode));
            }
        }

        if (n.has("services") && n.get("services").isArray()) {
            for (JsonNode sNode : n.get("services")) {
                inv.services.add(mapBookingServiceDetail(sNode));
            }
        }
        return inv;
    }

    private static PaymentTransaction mapPaymentTransaction(JsonNode n) {
        PaymentTransaction pt = new PaymentTransaction();
        pt.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
        pt.invoiceId = n.has("invoiceId") && !n.get("invoiceId").isNull() ? n.get("invoiceId").asLong() : null;
        pt.paymentMethodId = n.has("paymentMethodId") && !n.get("paymentMethodId").isNull()
                ? n.get("paymentMethodId").asLong()
                : null;
        pt.paymentMethodName = n.hasNonNull("paymentMethodName") ? n.get("paymentMethodName").asText() : "Tiền mặt";
        pt.amount = n.hasNonNull("amount") ? new BigDecimal(n.get("amount").asText()) : BigDecimal.ZERO;
        pt.transactionCode = n.hasNonNull("transactionCode") ? n.get("transactionCode").asText() : "";
        pt.status = n.hasNonNull("status") ? n.get("status").asText() : "THANH_CONG";
        pt.statusLabel = n.hasNonNull("statusLabel") ? n.get("statusLabel").asText() : "Thành công";
        pt.paymentTime = n.hasNonNull("paymentTime") ? n.get("paymentTime").asText() : "";
        pt.note = n.hasNonNull("note") ? n.get("note").asText() : "";
        pt.staffId = n.has("staffId") && !n.get("staffId").isNull() ? n.get("staffId").asLong() : null;
        pt.staffName = n.hasNonNull("staffName") ? n.get("staffName").asText() : "";
        return pt;
    }

    // ============== HELPERS CHUYỂN ĐỔI DỮ LIỆU ==============
    private static String mapBackendRoomStatus(String status) {
        if (status == null)
            return "TRONG";
        String s = status.trim().toLowerCase();
        if (s.contains("trống") || s.equals("trong"))
            return "TRONG";
        if (s.contains("ở") || s.equals("dang_o") || s.contains("?"))
            return "DANG_O";
        if (s.contains("dọn") || s.equals("dang_don") || s.contains("d?n"))
            return "DANG_DON";
        if (s.contains("trì") || s.equals("bao_tri") || s.equals("dang_bao_tri") || s.contains("b?o"))
            return "BAO_TRI";
        return "TRONG";
    }

    private static String mapFeRoomStatusToBackendEnum(String feStatus) {
        if (feStatus == null || feStatus.isBlank()
                || "Tất cả trạng thái".equalsIgnoreCase(feStatus)
                || "ALL".equalsIgnoreCase(feStatus)) {
            return null;
        }
        String s = feStatus.trim().toLowerCase();
        if (s.contains("trống") || s.equals("trong"))
            return "TRONG";
        if (s.contains("ở") || s.equals("dang_o") || s.contains("?"))
            return "DANG_O";
        if (s.contains("dọn") || s.equals("dang_don") || s.contains("d?n"))
            return "DANG_DON";
        if (s.contains("trì") || s.equals("bao_tri") || s.equals("dang_bao_tri") || s.contains("b?o"))
            return "DANG_BAO_TRI";
        return null;
    }

    private static String mapFeRoomStatusToVietnamese(String feStatus) {
        if (feStatus == null)
            return "Trống";
        return switch (feStatus.toUpperCase()) {
            case "DANG_O" -> "Đang ở";
            case "DANG_DON" -> "Đang dọn";
            case "BAO_TRI", "DANG_BAO_TRI" -> "Đang bảo trì";
            default -> "Trống";
        };
    }

    private static String mapStaffRoleToBackendEnum(String role) {
        if (role == null || role.isBlank() || "Tất cả vai trò".equalsIgnoreCase(role) || "ALL".equalsIgnoreCase(role)) {
            return null;
        }
        String r = role.trim().toLowerCase();
        if (r.contains("trị") || r.contains("tri") || r.equals("admin"))
            return "ADMIN";
        if (r.contains("lý") || r.contains("ly") || r.equals("manager"))
            return "MANAGER";
        if (r.contains("tân") || r.contains("tan") || r.equals("reception"))
            return "RECEPTION";
        return null;
    }

    private static String mapBackendAttendanceStatus(String status) {
        if (status == null)
            return "DU_CONG";
        return switch (status.trim().toLowerCase()) {
            case "đi trễ", "di_tre" -> "DI_TRE";
            case "về sớm", "ve_som" -> "VE_SOM";
            case "vắng mặt", "vang_mat" -> "VANG_MAT";
            default -> "DU_CONG";
        };
    }

    private static String mapFeAttendanceStatusToVietnamese(String feStatus) {
        if (feStatus == null)
            return "Đủ công";
        return switch (feStatus.toUpperCase()) {
            case "DI_TRE" -> "Đi trễ";
            case "VE_SOM" -> "Về sớm";
            case "VANG_MAT" -> "Vắng mặt";
            default -> "Đủ công";
        };
    }

    private static String formatDateTimeNode(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        if (node.isArray() && node.size() >= 5) {
            int y = node.get(0).asInt();
            int m = node.get(1).asInt();
            int d = node.get(2).asInt();
            int h = node.get(3).asInt();
            int min = node.get(4).asInt();
            return String.format("%02d:%02d %02d/%02d/%04d", h, min, d, m, y);
        }
        String txt = node.asText();
        if (txt == null || txt.isBlank())
            return null;
        if (txt.contains("T")) {
            String[] parts = txt.split("T");
            String datePart = formatToVnDate(parts[0]);
            String timePart = parts.length > 1 && parts[1].length() >= 5 ? parts[1].substring(0, 5) : "";
            return timePart.isEmpty() ? datePart : timePart + " " + datePart;
        }
        return txt;
    }

    private static String formatTimeNode(JsonNode node) {
        if (node == null || node.isNull())
            return "";
        if (node.isArray() && node.size() >= 2) {
            int h = node.get(0).asInt();
            int m = node.get(1).asInt();
            return String.format("%02d:%02d", h, m);
        }
        return formatTime(node.asText());
    }

    private static String formatTime(String time) {
        if (time == null || time.isBlank())
            return "";
        String t = time.trim();
        if (t.contains("T")) {
            String[] parts = t.split("T");
            if (parts.length > 1)
                t = parts[1];
        }
        if (t.length() >= 5)
            return t.substring(0, 5);
        return t;
    }

    private static String ensureTimeSeconds(String time) {
        if (time == null || time.isBlank())
            return "00:00:00";
        String t = time.trim();
        if (t.length() == 5)
            return t + ":00";
        return t;
    }

    private static String formatToVnDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return "";
        try {
            if (dateStr.contains("-")) {
                LocalDate d = LocalDate.parse(dateStr, DATE_ISO);
                return d.format(DATE_VN);
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }

    private static String formatToIsoDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return LocalDate.now().toString();
        try {
            if (dateStr.contains("/")) {
                LocalDate d = LocalDate.parse(dateStr, DATE_VN);
                return d.format(DATE_ISO);
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }

    // ============== BOOKING SERVICE DETAILS (CHI TIẾT DỊCH VỤ PHÒNG)
    // ==============
    public static PageResponse<BookingServiceDetail> bookingServiceDetailsPage(int pageNo, int pageSize, String keyword,
            Long bookingId, Long roomId, Long serviceId, BigDecimal minPrice, BigDecimal maxPrice, String sortDir) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String sDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder("/api/booking-service-details/search?pageNo=").append(safePage)
                .append("&pageSize=").append(safeSize).append("&sortBy=id&sortDir=").append(sDir);
        if (keyword != null && !keyword.isBlank()) {
            sb.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (bookingId != null) {
            sb.append("&bookingId=").append(bookingId);
        }
        if (roomId != null) {
            sb.append("&roomId=").append(roomId);
        }
        if (serviceId != null) {
            sb.append("&serviceId=").append(serviceId);
        }
        if (minPrice != null) {
            sb.append("&minPrice=").append(minPrice);
        }
        if (maxPrice != null) {
            sb.append("&maxPrice=").append(maxPrice);
        }

        PageResponse<JsonNode> page = ApiClient.get(sb.toString(), new TypeReference<PageResponse<JsonNode>>() {
        });
        PageResponse<BookingServiceDetail> res = new PageResponse<>();
        if (page == null) {
            res.pageNo = safePage;
            res.pageSize = safeSize;
            res.totalElements = 0;
            res.totalPages = 1;
            res.last = true;
            res.content = new ArrayList<>();
            return res;
        }
        res.pageNo = page.pageNo;
        res.pageSize = page.pageSize;
        res.totalElements = page.totalElements;
        res.totalPages = Math.max(1, page.totalPages);
        res.last = page.last;

        List<BookingServiceDetail> list = new ArrayList<>();
        if (page.getContent() != null) {
            for (JsonNode n : page.getContent()) {
                list.add(mapBookingServiceDetail(n));
            }
        }
        res.content = list;
        return res;
    }

    public static PageResponse<BookingServiceDetail> bookingServiceDetailsPage(int pageNo, int pageSize,
            Long bookingId) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, pageSize);
        String path = "/api/booking-service-details?pageNo=" + safePage + "&pageSize=" + safeSize
                + "&sortBy=id&sortDir=desc";
        if (bookingId != null) {
            path += "&bookingId=" + bookingId;
        }

        try {
            PageResponse<JsonNode> page = ApiClient.get(path, new TypeReference<PageResponse<JsonNode>>() {
            });

            PageResponse<BookingServiceDetail> res = new PageResponse<>();
            if (page == null) {
                res.pageNo = safePage;
                res.pageSize = safeSize;
                res.totalElements = 0;
                res.totalPages = 1;
                res.last = true;
                res.content = new ArrayList<>();
                return res;
            }

            res.pageNo = page.pageNo;
            res.pageSize = page.pageSize;
            res.totalElements = page.totalElements;
            res.totalPages = Math.max(1, page.totalPages);
            res.last = page.last;

            List<BookingServiceDetail> list = new ArrayList<>();
            if (page.getContent() != null) {
                for (JsonNode n : page.getContent()) {
                    list.add(mapBookingServiceDetail(n));
                }
            }
            res.content = list;
            return res;
        } catch (ApiClient.ApiException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                PageResponse<BookingServiceDetail> empty = new PageResponse<>();
                empty.pageNo = safePage;
                empty.pageSize = safeSize;
                empty.totalElements = 0;
                empty.totalPages = 1;
                empty.last = true;
                empty.content = new ArrayList<>();
                return empty;
            }
            throw ex;
        }
    }

    public static List<BookingServiceDetail> bookingServiceDetailsByBooking(Long bookingId) {
        if (bookingId == null)
            return new ArrayList<>();
        try {
            ApiResponse<JsonNode> resp = ApiClient.get("/api/booking-service-details/booking/" + bookingId,
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });
            List<BookingServiceDetail> list = new ArrayList<>();
            if (resp != null && resp.result != null && resp.result.has("details")) {
                JsonNode arr = resp.result.get("details");
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        list.add(mapBookingServiceDetail(n));
                    }
                }
            }
            return list;
        } catch (ApiClient.ApiException ex) {
            return new ArrayList<>();
        }
    }

    public static BookingServiceDetail createBookingServiceDetail(Long bookingId, Long serviceId, int quantity,
            BigDecimal unitPrice) {
        Map<String, Object> req = new HashMap<>();
        req.put("bookingId", bookingId);
        req.put("serviceId", serviceId);
        req.put("quantity", quantity);
        req.put("unitPrice", unitPrice);

        try {
            ApiResponse<JsonNode> resp = ApiClient.post("/api/booking-service-details", req,
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });

            if (resp != null && resp.result != null && resp.result.has("detail")) {
                return mapBookingServiceDetail(resp.result.get("detail"));
            }
            return null;
        } catch (ApiClient.ApiException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                throw new ApiClient.ApiException(
                        "API dịch vụ phòng chưa được kích hoạt trên server. Vui lòng Rerun/Khởi động lại Backend (HotelManagementProjectApplication) trong IntelliJ!");
            }
            throw ex;
        }
    }

    public static BookingServiceDetail updateBookingServiceDetail(Long id, Long bookingId, Long serviceId, int quantity,
            BigDecimal unitPrice) {
        Map<String, Object> req = new HashMap<>();
        req.put("bookingId", bookingId);
        req.put("serviceId", serviceId);
        req.put("quantity", quantity);
        req.put("unitPrice", unitPrice);

        try {
            ApiResponse<JsonNode> resp = ApiClient.put("/api/booking-service-details/" + id, req,
                    new TypeReference<ApiResponse<JsonNode>>() {
                    });

            if (resp != null && resp.result != null && resp.result.has("detail")) {
                return mapBookingServiceDetail(resp.result.get("detail"));
            }
            return null;
        } catch (ApiClient.ApiException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                throw new ApiClient.ApiException(
                        "API dịch vụ phòng chưa được kích hoạt trên server. Vui lòng Rerun/Khởi động lại Backend (HotelManagementProjectApplication) trong IntelliJ!");
            }
            throw ex;
        }
    }

    public static void deleteBookingServiceDetail(Long id) {
        try {
            ApiClient.delete("/api/booking-service-details/" + id);
        } catch (ApiClient.ApiException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                throw new ApiClient.ApiException(
                        "API dịch vụ phòng chưa được kích hoạt trên server. Vui lòng Rerun/Khởi động lại Backend (HotelManagementProjectApplication) trong IntelliJ!");
            }
            throw ex;
        }
    }

    private static BookingServiceDetail mapBookingServiceDetail(JsonNode n) {
        BookingServiceDetail d = new BookingServiceDetail();
        d.id = n.has("id") && !n.get("id").isNull() ? n.get("id").asLong() : null;
        d.bookingId = n.has("bookingId") && !n.get("bookingId").isNull() ? n.get("bookingId").asLong() : null;
        d.roomNumber = n.has("roomNumber") && !n.get("roomNumber").isNull() ? n.get("roomNumber").asText() : "";
        d.customerName = n.has("customerName") && !n.get("customerName").isNull() ? n.get("customerName").asText() : "";
        d.serviceId = n.has("serviceId") && !n.get("serviceId").isNull() ? n.get("serviceId").asLong() : null;
        d.serviceName = n.has("serviceName") && !n.get("serviceName").isNull() ? n.get("serviceName").asText() : "";
        d.quantity = n.has("quantity") && !n.get("quantity").isNull() ? n.get("quantity").asInt() : 1;
        d.unitPrice = n.has("unitPrice") && !n.get("unitPrice").isNull()
                ? new BigDecimal(n.get("unitPrice").asText())
                : BigDecimal.ZERO;
        d.totalPrice = n.has("totalPrice") && !n.get("totalPrice").isNull()
                ? new BigDecimal(n.get("totalPrice").asText())
                : d.unitPrice.multiply(BigDecimal.valueOf(d.quantity));
        d.usedAt = n.has("usedAt") ? formatDateTimeNode(n.get("usedAt")) : "";
        return d;
    }
}

package com.lotusstay.javafx.net;

/** Lưu thông tin phiên đăng nhập hiện tại (token JWT + người dùng) cho toàn ứng dụng. */
public class Session {
    private static String token;
    private static Long userId;
    private static String username;
    private static String fullName;
    private static String role; // ADMIN / MANAGER / RECEPTION

    public static void set(String token, Long userId, String username, String fullName, String role) {
        Session.token = token;
        Session.userId = userId;
        Session.username = username;
        Session.fullName = fullName;
        Session.role = role;
    }

    public static void clear() {
        token = null; userId = null; username = null; fullName = null; role = null;
    }

    public static boolean isLoggedIn() { return token != null; }
    public static String getToken() { return token; }
    public static Long getUserId() { return userId; }
    public static String getUsername() { return username; }
    public static String getFullName() { return fullName; }
    public static String getRole() { return role; }

    public static boolean isAdmin() { return "ADMIN".equals(role); }
    public static boolean isManagerOrAbove() { return isAdmin() || "MANAGER".equals(role); }

    public static boolean isReception() { return "RECEPTION".equals(role); }

    // Quản lý loại phòng: chỉ ADMIN
    public static boolean canManageRoomTypes() { return isAdmin(); }

    // Quản lý khuyến mãi: chỉ ADMIN
    public static boolean canManagePromotions() { return isAdmin(); }

    // Quản lý phương thức thanh toán: chỉ ADMIN
    public static boolean canManagePaymentMethods() { return isAdmin(); }

    // Quản lý nhân viên & tài khoản: ADMIN + MANAGER (theo Use Case Diagram)
    public static boolean canManageStaff() { return isManagerOrAbove(); }

    // Quản lý dịch vụ: ADMIN + MANAGER
    public static boolean canManageServices() { return isManagerOrAbove(); }

    // Quản lý ca, chấm công, phân ca: ADMIN + MANAGER
    public static boolean canManageHR() { return isManagerOrAbove(); }

    // Thêm / Xóa phòng vật lý: ADMIN + MANAGER
    public static boolean canManagePhysicalRooms() { return isManagerOrAbove(); }

    // Xóa đặt phòng/khách hàng: ADMIN + MANAGER
    public static boolean canDelete() { return isManagerOrAbove(); }

    // Xóa hóa đơn: chỉ ADMIN
    public static boolean canDeleteInvoice() { return isAdmin(); }

    // Sửa hóa đơn: ADMIN + MANAGER
    public static boolean canUpdateInvoice() { return isManagerOrAbove(); }

    // Xem nhân viên: ADMIN + MANAGER
    public static boolean canViewStaff() { return isManagerOrAbove(); }
}

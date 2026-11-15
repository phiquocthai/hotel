package com.example.hotel_management_project.Services;

public interface HotelAutomationService {
    /**
     * Chạy định kỳ toàn bộ tác vụ tự động hóa:
     * 1. Hủy đơn đặt phòng quá hạn 5 tiếng nhận phòng mà khách không đến (No-show).
     * 2. Hoàn tất đặt phòng và chuyển phòng sang 'Đang dọn' khi đến ngày trả phòng.
     * 3. Chuyển trạng thái khuyến mãi sang 'Đã hết hạn' (active = false) khi quá hạn kết thúc.
     */
    void runAutomations();

    /**
     * Hủy đơn đặt phòng quá 5 tiếng nhận phòng mà khách không đến
     * @return số lượng đơn đã tự động hủy
     */
    int autoCancelNoShowBookings();

    /**
     * Chuyển phòng sang 'Đang dọn' và đặt phòng sang 'Đã trả' khi đến ngày trả phòng
     * @return số lượng đơn đặt / phòng đã chuyển
     */
    int autoCheckoutAndCleanRooms();

    /**
     * Tự động chuyển khuyến mãi quá ngày kết thúc sang 'Đã hết hạn'
     * @return số lượng khuyến mãi đã chuyển
     */
    int autoExpirePromotions();
}


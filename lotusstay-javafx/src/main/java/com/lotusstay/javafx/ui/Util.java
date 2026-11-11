package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.net.ApiClient;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Util {

    public static void applyTheme(Alert a) {
        try {
            DialogPane pane = a.getDialogPane();
            if (pane != null && Util.class.getResource("/com/lotusstay/javafx/theme.css") != null) {
                pane.getStylesheets().add(
                        Util.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    public static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setTitle("Lỗi");
        a.setHeaderText("Có lỗi xảy ra");
        applyTheme(a);
        a.showAndWait();
    }

    public static void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle("Thông báo");
        a.setHeaderText(null);
        applyTheme(a);
        a.showAndWait();
    }

    public static void success(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle("Thành công");
        a.setHeaderText("Thông báo");
        applyTheme(a);
        a.showAndWait();
    }

    public static void warning(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message);
        a.setTitle("Cảnh báo");
        a.setHeaderText("Cảnh báo hệ thống");
        applyTheme(a);
        a.showAndWait();
    }

    /**
     * Gọi API ở luồng nền, trả kết quả về luồng JavaFX qua onSuccess;
     * Tự động lấy và hiển thị đúng thông báo (message) do backend trả về nếu có.
     * Nếu xảy ra lỗi, hiển thị đúng mã/nội dung lỗi từ backend.
     */
    public static <T> void runAsync(Supplier<T> apiCall, Consumer<T> onSuccess) {
        new Thread(() -> {
            try {
                ApiClient.clearLastMessage();
                T result = apiCall.get();
                String serverMsg = ApiClient.getLastMessage();
                Platform.runLater(() -> {
                    if (serverMsg != null && !serverMsg.isBlank()) {
                        success(serverMsg);
                    }
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
            } catch (ApiClient.ApiException ex) {
                Platform.runLater(() -> error(ex.getMessage()));
            } catch (Exception ex) {
                Platform.runLater(() -> error("Lỗi không xác định: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Dùng cho các action không trả dữ liệu (ví dụ delete) nhưng vẫn cần chạy nền + callback khi xong.
     * Tự động hiển thị đúng thông báo thành công từ backend nếu có.
     */
    public static void runAsyncVoid(Runnable apiCall, Runnable onSuccess) {
        new Thread(() -> {
            try {
                ApiClient.clearLastMessage();
                apiCall.run();
                String serverMsg = ApiClient.getLastMessage();
                Platform.runLater(() -> {
                    if (serverMsg != null && !serverMsg.isBlank()) {
                        success(serverMsg);
                    }
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            } catch (ApiClient.ApiException ex) {
                Platform.runLater(() -> error(ex.getMessage()));
            } catch (Exception ex) {
                Platform.runLater(() -> error("Lỗi không xác định: " + ex.getMessage()));
            }
        }).start();
    }

    public static String money(BigDecimal v) {
        if (v == null) return "0 ₫";
        return String.format("%,.0f ₫", v);
    }
}

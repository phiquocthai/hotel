package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.ServiceItem;
import com.lotusstay.javafx.net.ApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.util.List;

public class ServicesView {

    public static BorderPane build() {
        final Runnable[] reloadPageRef = new Runnable[1];
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(16);
        card.getStyleClass().add("panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header info
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Quản lý dịch vụ & Tiện ích");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(title, topSpacer);

        // Data collection
        ObservableList<ServiceItem> masterData = FXCollections.observableArrayList();

        // Top Toolbar với bộ lọc theo tên và khoảng giá
        TextField search = new TextField();
        search.setPromptText("🔍 Tìm theo tên dịch vụ...");
        search.setPrefWidth(280);
        search.setMinWidth(220);
        search.setPrefHeight(38);

        TextField minPrice = new TextField();
        minPrice.setPromptText("Giá từ (đ)");
        minPrice.setPrefWidth(145);
        minPrice.setMinWidth(135);
        minPrice.setPrefHeight(38);

        Label rangeDash = new Label("—");
        rangeDash.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: 700;");

        TextField maxPrice = new TextField();
        maxPrice.setPromptText("Giá đến (đ)");
        maxPrice.setPrefWidth(145);
        maxPrice.setMinWidth(135);
        maxPrice.setPrefHeight(38);

        // Thanh trượt mức giá trực quan cho dịch vụ
        Slider priceSlider = new Slider(0, 1000000, 1000000);
        priceSlider.setPrefWidth(160);
        priceSlider.setMinWidth(140);
        priceSlider.setBlockIncrement(20000);
        Label sliderLabel = new Label("Trần giá: Tất cả");
        sliderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F59E0B; -fx-font-weight: 700;");

        VBox sliderBox = new VBox(2, sliderLabel, priceSlider);
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        // Kéo thanh trượt chỉ cập nhật hiển thị trần giá, không gửi truy vấn tự động
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            long rounded = Math.round(newVal.doubleValue() / 10000.0) * 10000;
            if (rounded >= 1000000) {
                sliderLabel.setText("Trần giá: Tất cả");
                maxPrice.clear();
            } else {
                sliderLabel.setText("Trần giá: " + String.format("%,d đ", rounded));
                maxPrice.setText(String.valueOf(rounded));
            }
        });

        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setGraphic(IconUtils.icon("search", 13, Color.web("#94A3B8")));
        searchBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setGraphic(IconUtils.icon("refresh", 13, Color.web("#94A3B8")));
        refreshBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button addBtn = new Button("Thêm dịch vụ");
        addBtn.getStyleClass().add("btn-gold");
        addBtn.setGraphic(IconUtils.icon("plus", 14, Color.web("#0A0D14")));
        addBtn.setMinWidth(Region.USE_PREF_SIZE);
        addBtn.setPrefWidth(150);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID tăng dần", "ID giảm dần");
        sortOrder.setValue("ID tăng dần");

        HBox toolbar = new HBox(8, search, minPrice, rangeDash, maxPrice, sliderBox, searchBtn, refreshBtn, spacer,
                addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<ServiceItem> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setItems(masterData);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Chưa có dịch vụ nào trong hệ thống."));

        TableColumn<ServiceItem, String> idCol = new TableColumn<>();
        idCol.setMinWidth(75);
        idCol.setPrefWidth(85);
        Label idHeader = new Label("ID ▲");
        idHeader.setStyle("-fx-font-weight: 800; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
        idCol.setGraphic(idHeader);
        idCol.setSortable(false);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().id)));
        idCol.setStyle(
                "-fx-font-weight: 800; -fx-alignment: CENTER_LEFT; -fx-font-size: 13.5px; -fx-text-fill: #FFFFFF;");

        idHeader.setOnMouseClicked(e -> {
            if ("ID tăng dần".equals(sortOrder.getValue())) {
                sortOrder.setValue("ID giảm dần");
            } else {
                sortOrder.setValue("ID tăng dần");
            }
        });
        sortOrder.valueProperty().addListener((obs, oldV, newV) -> {
            idHeader.setText("ID " + ("ID giảm dần".equals(newV) ? "▼" : "▲"));
            if (reloadPageRef[0] != null)
                reloadPageRef[0].run();
        });

        TableColumn<ServiceItem, String> nameCol = new TableColumn<>("Tên dịch vụ");
        nameCol.setMinWidth(260);
        nameCol.setPrefWidth(280);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        nameCol.setStyle("-fx-font-weight: 800;");

        TableColumn<ServiceItem, String> priceCol = new TableColumn<>("Đơn giá (VNĐ)");
        priceCol.setMinWidth(170);
        priceCol.setPrefWidth(180);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().price)));
        priceCol.setStyle("-fx-font-weight: 800; -fx-text-fill: #10B981; -fx-font-size: 14px;");

        TableColumn<ServiceItem, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setMinWidth(135);
        statusCol.setPrefWidth(145);
        statusCol
                .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().active ? "Đang phục vụ" : "Tạm ngưng"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean act = "Đang phục vụ".equalsIgnoreCase(item);
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", act ? "badge-trong" : "badge-bao_tri");
                    badge.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 4 12;");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        // ── THANH PHÂN TRANG HIỆN ĐẠI ──────────────────────────────────────────
        HBox paginationBar = new HBox(12);
        paginationBar.setAlignment(Pos.CENTER_LEFT);
        paginationBar.setPadding(new Insets(10, 8, 4, 8));
        paginationBar.setStyle("-fx-background-color: transparent;");

        Label recordInfoLabel = new Label("Hiển thị 0 dịch vụ");
        recordInfoLabel.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-weight: 600;");

        Label sizeLabel = new Label("Số bản ghi / trang:");
        sizeLabel.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-text-fill: #94A3B8; -fx-font-weight: 500;");

        final int DEFAULT_PAGE_SIZE = 10;
        ComboBox<Integer> pageSizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 20));
        pageSizeCombo.setValue(DEFAULT_PAGE_SIZE);
        pageSizeCombo.setStyle(
                "-fx-background-color: #1E293B; -fx-border-color: rgba(148, 163, 184, 0.3); " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-pref-height: 28px; -fx-font-size: 12px; -fx-cursor: hand;");

        HBox leftPagingBox = new HBox(16, recordInfoLabel, sizeLabel, pageSizeCombo);
        leftPagingBox.setAlignment(Pos.CENTER_LEFT);

        Region pagSpacer = new Region();
        HBox.setHgrow(pagSpacer, Priority.ALWAYS);

        HBox pageButtonsBox = new HBox(6);
        pageButtonsBox.setAlignment(Pos.CENTER_RIGHT);

        paginationBar.getChildren().addAll(leftPagingBox, pagSpacer, pageButtonsBox);

        int[] currentPage = new int[] { 1 };
        int[] currentPageSize = new int[] { DEFAULT_PAGE_SIZE };

        @SuppressWarnings("unchecked")
        java.util.function.Consumer<Integer>[] loadPageHolder = new java.util.function.Consumer[1];
        reloadPageRef[0] = () -> loadPageHolder[0].accept(1);
        loadPageHolder[0] = targetPage -> {
            currentPage[0] = Math.max(1, targetPage);
            String q = search.getText() == null ? "" : search.getText().trim();

            BigDecimal minVal = null;
            try {
                if (minPrice.getText() != null && !minPrice.getText().trim().isEmpty()) {
                    minVal = new BigDecimal(minPrice.getText().trim().replaceAll("[^0-9.]", ""));
                }
            } catch (Exception ignored) {
            }

            BigDecimal maxVal = null;
            try {
                if (maxPrice.getText() != null && !maxPrice.getText().trim().isEmpty()) {
                    maxVal = new BigDecimal(maxPrice.getText().trim().replaceAll("[^0-9.]", ""));
                }
            } catch (Exception ignored) {
            }

            final BigDecimal fMin = minVal;
            final BigDecimal fMax = maxVal;

            String sDir = "ID giảm dần".equals(sortOrder.getValue()) ? "desc" : "asc";
            Util.runAsync(() -> ApiService.servicePage(currentPage[0], currentPageSize[0], q, fMin, fMax, sDir),
                    page -> {
                        masterData.setAll(page.getContent() != null ? page.getContent() : List.of());
                        table.scrollTo(0);

                        int totalPages = Math.max(1, page.totalPages);
                        long totalElements = page.totalElements;
                        currentPage[0] = page.pageNo;

                        if (totalElements == 0) {
                            recordInfoLabel.setText("Hiển thị 0 dịch vụ");
                        } else {
                            int from = (page.pageNo - 1) * page.pageSize + 1;
                            int to = (int) Math.min((long) page.pageNo * page.pageSize, totalElements);
                            recordInfoLabel.setText(
                                    "Hiển thị " + from + " – " + to + " trên tổng " + totalElements + " dịch vụ");
                        }

                        renderPageButtons(pageButtonsBox, currentPage[0], totalPages,
                                newPage -> loadPageHolder[0].accept(newPage));
                    });
        };

        Runnable loadCurrentPage = () -> loadPageHolder[0].accept(currentPage[0]);
        Runnable doSearch = () -> loadPageHolder[0].accept(1);

        searchBtn.setOnAction(e -> doSearch.run());
        search.setOnAction(e -> doSearch.run());
        minPrice.setOnAction(e -> doSearch.run());
        maxPrice.setOnAction(e -> doSearch.run());

        refreshBtn.setOnAction(e -> {
            search.clear();
            minPrice.clear();
            maxPrice.clear();
            priceSlider.setValue(1000000);
            sliderLabel.setText("Trần giá: Tất cả");
            sortOrder.setValue("ID tăng dần");
            doSearch.run();
        });

        pageSizeCombo.setOnAction(e -> {
            Integer sz = pageSizeCombo.getValue();
            if (sz != null && sz != currentPageSize[0]) {
                currentPageSize[0] = sz;
                doSearch.run();
            }
        });

        // Column Thao tác: Sửa, Xóa
        TableColumn<ServiceItem, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setMinWidth(180);
        actionCol.setPrefWidth(190);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button delBtn = new Button("Xóa");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-action-edit");
                editBtn.setGraphic(IconUtils.icon("edit", 12, Color.WHITE));
                editBtn.setGraphicTextGap(6);
                editBtn.setMinWidth(68);
                editBtn.setStyle("-fx-padding: 4 11; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 68px;");

                delBtn.getStyleClass().add("btn-danger");
                delBtn.setGraphic(IconUtils.icon("trash", 12, Color.WHITE));
                delBtn.setGraphicTextGap(6);
                delBtn.setMinWidth(68);
                delBtn.setStyle("-fx-padding: 4 11; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 68px;");

                box.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    ServiceItem s = getTableView().getItems().get(getIndex());
                    openServiceDialog(s, loadCurrentPage);
                });

                delBtn.setOnAction(e -> {
                    ServiceItem s = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận xóa dịch vụ");
                    alert.setHeaderText("Xóa dịch vụ " + s.name + "?");
                    Util.applyTheme(alert);
                    alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
                        Util.runAsyncVoid(() -> ApiService.deleteService(s.id), loadCurrentPage);
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(idCol, nameCol, priceCol, statusCol, actionCol);

        // Initial load
        doSearch.run();

        addBtn.setOnAction(e -> openServiceDialog(null, loadCurrentPage));

        card.getChildren().addAll(headerRow, toolbar, table, paginationBar);
        root.setCenter(card);
        return root;
    }

    private static void openServiceDialog(ServiceItem existing, Runnable reload) {
        boolean isEdit = existing != null;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Cập nhật dịch vụ" : "Thêm mới dịch vụ");
        dialog.getDialogPane().getStylesheets().add(
                ServicesView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());

        TextField name = new TextField(isEdit ? existing.name : "");
        name.setPromptText("Tên dịch vụ (VD: Đưa đón sân bay, tối đa 100 ký tự)");

        TextField price = new TextField(isEdit && existing.price != null ? existing.price.toPlainString() : "50000");
        price.setPromptText("Đơn giá (VNĐ)");

        VBox form = new VBox(10,
                labeledBox("Tên dịch vụ (*)", name),
                labeledBox("Đơn giá (don_gia) (*)", price));
        form.setPadding(new Insets(16));
        form.setPrefWidth(420);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String nameVal = name.getText().trim();
                if (nameVal.isBlank()) {
                    Util.error("Tên dịch vụ không được để trống.");
                    return null;
                }
                BigDecimal priceVal;
                try {
                    priceVal = new BigDecimal(price.getText().trim());
                    if (priceVal.compareTo(BigDecimal.ZERO) < 0)
                        throw new IllegalArgumentException();
                } catch (Exception ex) {
                    Util.error("Đơn giá phải là số dương hợp lệ.");
                    return null;
                }

                final BigDecimal finalPrice = priceVal;

                if (isEdit) {
                    Util.runAsync(() -> ApiService.updateService(existing.id, null, nameVal, finalPrice),
                            s -> reload.run());
                } else {
                    Util.runAsync(() -> ApiService.createService(nameVal, finalPrice), s -> reload.run());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private static VBox labeledBox(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        return new VBox(4, l, field);
    }

    private static void renderPageButtons(HBox box, int cur, int total, java.util.function.Consumer<Integer> onSelect) {
        box.getChildren().clear();
        if (total <= 1)
            return;

        Button prev = new Button("‹");
        prev.setStyle(pageButtonStyle(false));
        prev.setDisable(cur <= 1);
        prev.setOnAction(e -> onSelect.accept(cur - 1));
        box.getChildren().add(prev);

        if (total <= 7) {
            for (int i = 1; i <= total; i++) {
                box.getChildren().add(createNumBtn(i, i == cur, onSelect));
            }
        } else {
            if (cur <= 4) {
                for (int i = 1; i <= 5; i++) {
                    box.getChildren().add(createNumBtn(i, i == cur, onSelect));
                }
                box.getChildren().add(createEllipsis());
                box.getChildren().add(createNumBtn(total, false, onSelect));
            } else if (cur >= total - 3) {
                box.getChildren().add(createNumBtn(1, false, onSelect));
                box.getChildren().add(createEllipsis());
                for (int i = total - 4; i <= total; i++) {
                    box.getChildren().add(createNumBtn(i, i == cur, onSelect));
                }
            } else {
                box.getChildren().add(createNumBtn(1, false, onSelect));
                box.getChildren().add(createEllipsis());
                box.getChildren().add(createNumBtn(cur - 1, false, onSelect));
                box.getChildren().add(createNumBtn(cur, true, onSelect));
                box.getChildren().add(createNumBtn(cur + 1, false, onSelect));
                box.getChildren().add(createEllipsis());
                box.getChildren().add(createNumBtn(total, false, onSelect));
            }
        }

        Button next = new Button("›");
        next.setStyle(pageButtonStyle(false));
        next.setDisable(cur >= total);
        next.setOnAction(e -> onSelect.accept(cur + 1));
        box.getChildren().add(next);
    }

    private static Button createNumBtn(int page, boolean active, java.util.function.Consumer<Integer> onSelect) {
        Button b = new Button(String.valueOf(page));
        b.setStyle(pageButtonStyle(active));
        b.setOnAction(e -> {
            if (!active)
                onSelect.accept(page);
        });
        return b;
    }

    private static Label createEllipsis() {
        Label l = new Label("…");
        l.setStyle("-fx-text-fill: #64748B; -fx-padding: 0 4; -fx-font-weight: 700;");
        return l;
    }

    private static String pageButtonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #D97706; -fx-text-fill: #FFFFFF; " +
                    "-fx-font-weight: 700; -fx-min-width: 30px; -fx-min-height: 30px; " +
                    "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;";
        }
        return "-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; " +
                "-fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-min-width: 30px; -fx-min-height: 30px; " +
                "-fx-font-size: 12px; -fx-cursor: hand;";
    }
}

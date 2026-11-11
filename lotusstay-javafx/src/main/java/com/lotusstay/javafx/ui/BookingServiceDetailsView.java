package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.Booking;
import com.lotusstay.javafx.model.BookingServiceDetail;
import com.lotusstay.javafx.model.Room;
import com.lotusstay.javafx.model.ServiceItem;
import com.lotusstay.javafx.net.ApiService;
import com.lotusstay.javafx.net.PageResponse;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class BookingServiceDetailsView {

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,###");

    private static int currentPage = 1;
    private static int pageSize = 10;
    private static int totalPages = 1;
    private static long totalElements = 0;

    public static Node build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        // ── Header Row ────────────────────────────────────────────────────────
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label titleLbl = new Label("Chi tiết dịch vụ phòng");
        titleLbl.getStyleClass().add("card-title");
        titleLbl.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");

        Label subtitleLbl = new Label("Quản lý và ghi nhận các dịch vụ khách sử dụng trong quá trình lưu trú");
        subtitleLbl.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #8C9CB2;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Thêm dịch vụ phòng");
        addBtn.getStyleClass().add("btn-gold");
        addBtn.setGraphic(IconUtils.icon("plus", 12, Color.web("#0A0D14")));
        addBtn.setGraphicTextGap(8);

        headerRow.getChildren().addAll(titleBox, spacer, addBtn);

        // ── Toolbar: Filter & Search ───────────────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("Từ khóa (phòng, khách, DV)...");
        searchField.setPrefWidth(210);

        ComboBox<RoomOption> roomFilter = new ComboBox<>();
        roomFilter.setPromptText("Tất cả phòng");
        roomFilter.setPrefWidth(140);

        ComboBox<ServiceOption> serviceFilter = new ComboBox<>();
        serviceFilter.setPromptText("Tất cả dịch vụ");
        serviceFilter.setPrefWidth(160);

        ComboBox<BookingOption> bookingFilter = new ComboBox<>();
        bookingFilter.setPromptText("Tất cả đơn đặt");
        bookingFilter.setPrefWidth(180);

        TextField minPriceField = new TextField();
        minPriceField.setPromptText("Tiền từ (đ)");
        minPriceField.setPrefWidth(110);

        Label rangeDash = new Label("—");
        rangeDash.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: 700;");

        TextField maxPriceField = new TextField();
        maxPriceField.setPromptText("Tiền đến (đ)");
        maxPriceField.setPrefWidth(110);

        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setGraphic(IconUtils.icon("search", 13, Color.web("#F59E0B")));
        searchBtn.setGraphicTextGap(6);

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setGraphic(IconUtils.icon("refresh", 13, Color.web("#94A3B8")));
        refreshBtn.setGraphicTextGap(6);

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID giảm dần", "ID tăng dần");
        sortOrder.setValue("ID giảm dần");

        HBox toolbar = new HBox(8, searchField, roomFilter, serviceFilter, bookingFilter, minPriceField, rangeDash, maxPriceField, searchBtn, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // ── Table ─────────────────────────────────────────────────────────────
        TableView<BookingServiceDetail> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có bản ghi dịch vụ phòng nào"));
        VBox.setVgrow(table, Priority.ALWAYS);

        ObservableList<BookingServiceDetail> masterData = FXCollections.observableArrayList();
        table.setItems(masterData);

        // ── Pagination Footer Controls (Chuẩn PaginationHelper thống nhất giao diện) ──
        PaginationHelper.PaginationBar paginationBar = new PaginationHelper.PaginationBar("dịch vụ phòng");

        // ── Load rooms into filter ComboBox ────────────────────────────────────
        Runnable loadRooms = () -> Util.runAsync(ApiService::rooms, list -> {
            roomFilter.getItems().clear();
            roomFilter.getItems().add(new RoomOption(null, "Tất cả phòng"));
            for (Room r : list) {
                if (r.number != null) {
                    roomFilter.getItems().add(new RoomOption(r.id, "Phòng " + r.number));
                }
            }
            if (!roomFilter.getItems().isEmpty()) {
                roomFilter.setValue(roomFilter.getItems().get(0));
            }
        });
        loadRooms.run();

        // ── Load services into filter ComboBox ─────────────────────────────────
        Runnable loadServices = () -> Util.runAsync(ApiService::services, list -> {
            serviceFilter.getItems().clear();
            serviceFilter.getItems().add(new ServiceOption(null, "Tất cả dịch vụ", BigDecimal.ZERO, "Tất cả dịch vụ"));
            for (ServiceItem s : list) {
                serviceFilter.getItems().add(new ServiceOption(s.id, s.name, s.price, s.name != null ? s.name : "DV #" + s.id));
            }
            if (!serviceFilter.getItems().isEmpty()) {
                serviceFilter.setValue(serviceFilter.getItems().get(0));
            }
        });
        loadServices.run();

        // ── Load bookings into filter ComboBox ─────────────────────────────────
        Runnable loadBookings = () -> Util.runAsync(ApiService::bookings, list -> {
            bookingFilter.getItems().clear();
            bookingFilter.getItems().add(new BookingOption(null, "Tất cả đơn đặt phòng"));
            for (Booking b : list) {
                String label = (b.room != null ? "Phòng " + b.room.number : "Đơn #" + b.id)
                        + (b.customer != null ? " - " + b.customer.fullName : "")
                        + " (" + (b.status != null ? b.status : "") + ")";
                bookingFilter.getItems().add(new BookingOption(b.id, label));
            }
            if (!bookingFilter.getItems().isEmpty()) {
                bookingFilter.setValue(bookingFilter.getItems().get(0));
            }
        });
        loadBookings.run();

        // ── Reload Table Data from Backend Search API ──────────────────────────
        final Runnable[] reloadRef = new Runnable[1];
        reloadRef[0] = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().trim();
            Long selRoomId = (roomFilter.getValue() != null) ? roomFilter.getValue().roomId : null;
            Long selServiceId = (serviceFilter.getValue() != null) ? serviceFilter.getValue().serviceId : null;
            Long selBookingId = (bookingFilter.getValue() != null) ? bookingFilter.getValue().bookingId : null;

            BigDecimal minVal = null;
            if (minPriceField.getText() != null && !minPriceField.getText().trim().isEmpty()) {
                try {
                    minVal = new BigDecimal(minPriceField.getText().trim().replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
            }

            BigDecimal maxVal = null;
            if (maxPriceField.getText() != null && !maxPriceField.getText().trim().isEmpty()) {
                try {
                    maxVal = new BigDecimal(maxPriceField.getText().trim().replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
            }

            final BigDecimal fMin = minVal;
            final BigDecimal fMax = maxVal;

            String sDir = "ID tăng dần".equals(sortOrder.getValue()) ? "asc" : "desc";
            Util.runAsync(() -> ApiService.bookingServiceDetailsPage(
                    currentPage, pageSize, q, selBookingId, selRoomId, selServiceId, fMin, fMax, sDir), page -> {
                masterData.setAll(page.getContent() != null ? page.getContent() : List.of());
                totalElements = page.totalElements;
                totalPages = Math.max(1, page.totalPages);
                currentPage = Math.min(page.pageNo, totalPages);

                paginationBar.update(currentPage, pageSize, totalElements, p -> {
                    currentPage = p;
                    reloadRef[0].run();
                });
            });
        };
        Runnable reload = () -> reloadRef[0].run();
        Runnable doSearch = () -> {
            currentPage = 1;
            reload.run();
        };

        // Bắt buộc nhấn Tìm kiếm hoặc Enter mới truy vấn xuống backend
        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());
        minPriceField.setOnAction(e -> doSearch.run());
        maxPriceField.setOnAction(e -> doSearch.run());

        paginationBar.getPageSizeCombo().setOnAction(e -> {
            Integer selectedSize = paginationBar.getPageSizeCombo().getValue();
            if (selectedSize != null && selectedSize != pageSize) {
                pageSize = selectedSize;
                currentPage = 1;
                reload.run();
            }
        });

        refreshBtn.setOnAction(e -> {
            searchField.clear();
            minPriceField.clear();
            maxPriceField.clear();
            if (!roomFilter.getItems().isEmpty()) roomFilter.setValue(roomFilter.getItems().get(0));
            if (!serviceFilter.getItems().isEmpty()) serviceFilter.setValue(serviceFilter.getItems().get(0));
            if (!bookingFilter.getItems().isEmpty()) bookingFilter.setValue(bookingFilter.getItems().get(0));
            loadRooms.run();
            loadServices.run();
            loadBookings.run();
            doSearch.run();
        });

        // ── Columns Definition ────────────────────────────────────────────────
        TableColumn<BookingServiceDetail, String> idCol = new TableColumn<>();
        idCol.setMinWidth(60);
        idCol.setMaxWidth(80);
        Label idHeader = new Label("ID ▼");
        idHeader.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 700; -fx-cursor: hand; -fx-text-fill: #94A3B8;");
        idCol.setGraphic(idHeader);
        idCol.setSortable(false);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().id != null ? String.valueOf(d.getValue().id) : "—"));
        idCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");

        idHeader.setOnMouseClicked(e -> {
            if ("ID giảm dần".equals(sortOrder.getValue())) {
                sortOrder.setValue("ID tăng dần");
            } else {
                sortOrder.setValue("ID giảm dần");
            }
        });
        sortOrder.valueProperty().addListener((obs, oldV, newV) -> {
            idHeader.setText("ID " + ("ID giảm dần".equals(newV) ? "▼" : "▲"));
            doSearch.run();
        });

        TableColumn<BookingServiceDetail, String> timeCol = new TableColumn<>("THỜI GIAN SỬ DỤNG");
        timeCol.setMinWidth(140);
        timeCol.setPrefWidth(150);
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().usedAt != null && !d.getValue().usedAt.isBlank() ? d.getValue().usedAt : "—"));
        timeCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #94A3B8; -fx-font-size: 12px;");

        TableColumn<BookingServiceDetail, String> custCol = new TableColumn<>("TÊN KHÁCH");
        custCol.setMinWidth(140);
        custCol.setPrefWidth(160);
        custCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().customerName != null && !d.getValue().customerName.isBlank() ? d.getValue().customerName : "—"));
        custCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 600; -fx-text-fill: #F1F5F9;");

        TableColumn<BookingServiceDetail, String> roomCol = new TableColumn<>("PHÒNG");
        roomCol.setMinWidth(100);
        roomCol.setPrefWidth(110);
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().roomNumber != null && !d.getValue().roomNumber.isBlank() ? "Phòng " + d.getValue().roomNumber : "—"));
        roomCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 800; -fx-text-fill: #FCD34D;");

        TableColumn<BookingServiceDetail, String> serviceCol = new TableColumn<>("DỊCH VỤ");
        serviceCol.setMinWidth(140);
        serviceCol.setPrefWidth(160);
        serviceCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().serviceName != null && !d.getValue().serviceName.isBlank() ? d.getValue().serviceName : "—"));
        serviceCol.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 700; -fx-text-fill: #38BDF8;");

        TableColumn<BookingServiceDetail, String> qtyCol = new TableColumn<>("SL");
        qtyCol.setMinWidth(60);
        qtyCol.setMaxWidth(80);
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().quantity != null ? String.valueOf(d.getValue().quantity) : "1"));
        qtyCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: 700; -fx-text-fill: #F1F5F9;");

        TableColumn<BookingServiceDetail, String> priceCol = new TableColumn<>("ĐƠN GIÁ");
        priceCol.setMinWidth(110);
        priceCol.setPrefWidth(120);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().unitPrice != null ? CURRENCY_FMT.format(d.getValue().unitPrice) : "0") + " đ"));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #E2E8F0;");

        TableColumn<BookingServiceDetail, String> totalCol = new TableColumn<>("THÀNH TIỀN");
        totalCol.setMinWidth(120);
        totalCol.setPrefWidth(130);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().totalPrice != null ? CURRENCY_FMT.format(d.getValue().totalPrice) : "0") + " đ"));
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");

        TableColumn<BookingServiceDetail, Void> actionCol = new TableColumn<>("THAO TÁC");
        actionCol.setMinWidth(160);
        actionCol.setMaxWidth(180);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button delBtn = new Button("Xóa");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-action-edit");
                editBtn.setGraphic(IconUtils.icon("edit", 11, Color.WHITE));
                editBtn.setGraphicTextGap(5);
                editBtn.setMinWidth(65);
                editBtn.setStyle("-fx-padding: 4 9; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 65px;");

                delBtn.getStyleClass().add("btn-danger");
                delBtn.setGraphic(IconUtils.icon("trash", 11, Color.WHITE));
                delBtn.setGraphicTextGap(5);
                delBtn.setMinWidth(65);
                delBtn.setStyle("-fx-padding: 4 9; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 65px;");

                box.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    BookingServiceDetail item = getTableView().getItems().get(getIndex());
                    openEditDialog(item, reload);
                });

                delBtn.setOnAction(e -> {
                    BookingServiceDetail item = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Xóa dịch vụ '" + item.serviceName + "' khỏi phòng " + item.roomNumber + "?");
                    confirm.setHeaderText("Xác nhận xóa dịch vụ");
                    confirm.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt ->
                            Util.runAsyncVoid(() -> ApiService.deleteBookingServiceDetail(item.id), reload));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(idCol, timeCol, custCol, roomCol, serviceCol, qtyCol, priceCol, totalCol, actionCol);

        addBtn.setOnAction(e -> openAddDialog(bookingFilter.getValue() != null ? bookingFilter.getValue().bookingId : null, reload));

        // Load ban đầu
        reload.run();

        card.getChildren().addAll(headerRow, toolbar, table, paginationBar);
        root.setCenter(card);
        return root;
    }

    // ── Dialog Thêm Dịch vụ phòng ─────────────────────────────────────────────
    public static void openAddDialog(Long defaultBookingId, Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Thêm dịch vụ phòng");
        dialog.setHeaderText("Ghi nhận dịch vụ khách gọi / sử dụng tại phòng");
        try {
            dialog.getDialogPane().getStylesheets().add(
                    BookingServiceDetailsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
        } catch (Exception ignored) {}

        ComboBox<BookingOption> bookingBox = new ComboBox<>();
        bookingBox.setPromptText("Chọn đơn đặt phòng...");
        bookingBox.setPrefWidth(320);

        ComboBox<ServiceOption> serviceBox = new ComboBox<>();
        serviceBox.setPromptText("Chọn dịch vụ...");
        serviceBox.setPrefWidth(320);

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(120);

        TextField priceField = new TextField();
        priceField.setPromptText("Đơn giá (VNĐ)");
        priceField.setPrefWidth(180);

        Label totalCalcLabel = new Label("0 đ");
        totalCalcLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");

        Runnable updateCalc = () -> {
            try {
                int q = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                String pStr = priceField.getText() == null ? "0" : priceField.getText().trim().replace(",", "");
                BigDecimal p = pStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(pStr);
                totalCalcLabel.setText(CURRENCY_FMT.format(p.multiply(BigDecimal.valueOf(q))) + " đ");
            } catch (Exception e) {
                totalCalcLabel.setText("0 đ");
            }
        };

        serviceBox.setOnAction(e -> {
            ServiceOption s = serviceBox.getValue();
            if (s != null && s.price != null) {
                priceField.setText(s.price.toPlainString());
                updateCalc.run();
            }
        });

        qtySpinner.valueProperty().addListener((obs, oldV, newV) -> updateCalc.run());
        priceField.textProperty().addListener((obs, oldV, newV) -> updateCalc.run());

        // Load Bookings
        Util.runAsync(ApiService::bookings, bookings -> {
            for (Booking b : bookings) {
                String label = (b.room != null ? "Phòng " + b.room.number : "Đơn #" + b.id)
                        + (b.customer != null ? " - " + b.customer.fullName : "")
                        + " (" + (b.status != null ? b.status : "") + ")";
                BookingOption opt = new BookingOption(b.id, label);
                bookingBox.getItems().add(opt);
                if (defaultBookingId != null && defaultBookingId.equals(b.id)) {
                    bookingBox.setValue(opt);
                }
            }
            if (bookingBox.getValue() == null && !bookingBox.getItems().isEmpty()) {
                bookingBox.setValue(bookingBox.getItems().get(0));
            }
        });

        // Load Services
        Util.runAsync(ApiService::services, services -> {
            for (ServiceItem s : services) {
                String label = s.name + " (" + CURRENCY_FMT.format(s.price != null ? s.price : BigDecimal.ZERO) + " đ)";
                serviceBox.getItems().add(new ServiceOption(s.id, s.name, s.price, label));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 20, 24));

        grid.add(new Label("Đơn đặt phòng / Phòng: *"), 0, 0);
        grid.add(bookingBox, 1, 0, 2, 1);

        grid.add(new Label("Dịch vụ: *"), 0, 1);
        grid.add(serviceBox, 1, 1, 2, 1);

        grid.add(new Label("Số lượng: *"), 0, 2);
        grid.add(qtySpinner, 1, 2);

        grid.add(new Label("Đơn giá (đ):"), 0, 3);
        grid.add(priceField, 1, 3);

        grid.add(new Label("Thành tiền tạm tính:"), 0, 4);
        grid.add(totalCalcLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                BookingOption bOpt = bookingBox.getValue();
                ServiceOption sOpt = serviceBox.getValue();
                if (bOpt == null || bOpt.bookingId == null) {
                    Util.error("Vui lòng chọn đơn đặt phòng.");
                    return null;
                }
                if (sOpt == null || sOpt.serviceId == null) {
                    Util.error("Vui lòng chọn dịch vụ.");
                    return null;
                }
                int q = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                BigDecimal p = null;
                try {
                    String pStr = priceField.getText() != null ? priceField.getText().trim().replace(",", "") : "";
                    if (!pStr.isEmpty()) p = new BigDecimal(pStr);
                } catch (Exception ignored) {}

                BigDecimal finalP = p;
                Util.runAsync(() -> ApiService.createBookingServiceDetail(bOpt.bookingId, sOpt.serviceId, q, finalP), res -> {
                    if (reload != null) reload.run();
                });
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ── Dialog Sửa Dịch vụ phòng ──────────────────────────────────────────────
    private static void openEditDialog(BookingServiceDetail item, Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật dịch vụ phòng");
        dialog.setHeaderText("Cập nhật số lượng và đơn giá dịch vụ: " + item.serviceName);
        try {
            dialog.getDialogPane().getStylesheets().add(
                    BookingServiceDetailsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
        } catch (Exception ignored) {}

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100, item.quantity != null ? item.quantity : 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(120);

        TextField priceField = new TextField(item.unitPrice != null ? item.unitPrice.toPlainString() : "0");
        priceField.setPrefWidth(180);

        Label totalCalcLabel = new Label();
        totalCalcLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");

        Runnable updateCalc = () -> {
            try {
                int q = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                String pStr = priceField.getText() == null ? "0" : priceField.getText().trim().replace(",", "");
                BigDecimal p = pStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(pStr);
                totalCalcLabel.setText(CURRENCY_FMT.format(p.multiply(BigDecimal.valueOf(q))) + " đ");
            } catch (Exception e) {
                totalCalcLabel.setText("0 đ");
            }
        };

        qtySpinner.valueProperty().addListener((obs, oldV, newV) -> updateCalc.run());
        priceField.textProperty().addListener((obs, oldV, newV) -> updateCalc.run());
        updateCalc.run();

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 20, 24));

        grid.add(new Label("Phòng:"), 0, 0);
        Label roomLbl = new Label("Phòng " + (item.roomNumber != null ? item.roomNumber : "—") + " (" + (item.customerName != null ? item.customerName : "") + ")");
        roomLbl.setStyle("-fx-font-weight: 700;");
        grid.add(roomLbl, 1, 0);

        grid.add(new Label("Dịch vụ:"), 0, 1);
        Label sNameLbl = new Label(item.serviceName != null ? item.serviceName : "—");
        sNameLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #38BDF8;");
        grid.add(sNameLbl, 1, 1);

        grid.add(new Label("Số lượng: *"), 0, 2);
        grid.add(qtySpinner, 1, 2);

        grid.add(new Label("Đơn giá (đ): *"), 0, 3);
        grid.add(priceField, 1, 3);

        grid.add(new Label("Thành tiền:"), 0, 4);
        grid.add(totalCalcLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                int q = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                BigDecimal p = null;
                try {
                    String pStr = priceField.getText() != null ? priceField.getText().trim().replace(",", "") : "";
                    if (!pStr.isEmpty()) p = new BigDecimal(pStr);
                } catch (Exception ignored) {}

                BigDecimal finalP = p;
                Util.runAsync(() -> ApiService.updateBookingServiceDetail(item.id, item.bookingId, item.serviceId, q, finalP), res -> {
                    if (reload != null) reload.run();
                });
            }
            return null;
        });

        dialog.showAndWait();
    }



    // ── Helper Models ─────────────────────────────────────────────────────
    private static class BookingOption {
        final Long bookingId;
        final String label;

        BookingOption(Long bookingId, String label) {
            this.bookingId = bookingId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class ServiceOption {
        final Long serviceId;
        final String name;
        final BigDecimal price;
        final String label;

        ServiceOption(Long serviceId, String name, BigDecimal price, String label) {
            this.serviceId = serviceId;
            this.name = name;
            this.price = price;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class RoomOption {
        final Long roomId;
        final String label;

        RoomOption(Long roomId, String label) {
            this.roomId = roomId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

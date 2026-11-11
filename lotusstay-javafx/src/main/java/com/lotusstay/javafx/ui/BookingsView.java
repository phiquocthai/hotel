package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.Booking;
import com.lotusstay.javafx.model.Customer;
import com.lotusstay.javafx.model.Room;
import com.lotusstay.javafx.model.RoomType;
import com.lotusstay.javafx.net.ApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookingsView {

    public static BorderPane build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        VBox container = new VBox(16);
        container.getStyleClass().add("panel");
        VBox.setVgrow(container, Priority.ALWAYS);

        // Header info
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label title = new Label("Quản lý đặt phòng & Lưu trú");
        title.getStyleClass().add("section-title");
        title.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");

        Label subtitle = new Label(
                "Theo dõi danh sách khách đặt, tiến hành nhận/trả phòng và cập nhật tình trạng phòng tức thì");
        subtitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-text-fill: #8C9CB2;");
        titleBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button topAddBtn = new Button("+ Đặt phòng mới");
        topAddBtn.getStyleClass().add("btn-gold");
        topAddBtn.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-background-color: #F59E0B; -fx-text-fill: #0A0D14; -fx-background-radius: 8; -fx-padding: 8 18; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(245, 158, 11, 0.4), 10, 0, 0, 2);");

        headerRow.getChildren().addAll(titleBox, topSpacer, topAddBtn);

        // Data collection
        ObservableList<Booking> masterData = FXCollections.observableArrayList();
        ObservableList<Booking> pagedData = FXCollections.observableArrayList();

        // KPI Summary Cards
        Label totalCountLbl = new Label("0");
        Label stayingCountLbl = new Label("0");
        Label reservedCountLbl = new Label("0");
        Label completedCountLbl = new Label("0");

        GridPane kpiGrid = buildKpiRow(totalCountLbl, stayingCountLbl, reservedCountLbl, completedCountLbl);

        Runnable updateKpi = () -> {
            long total = masterData.size();
            long staying = masterData.stream()
                    .filter(b -> "DANG_O".equalsIgnoreCase(b.status) || "DANG_SU_DUNG".equalsIgnoreCase(b.status))
                    .count();
            long reserved = masterData.stream()
                    .filter(b -> "DAT_TRUOC".equalsIgnoreCase(b.status) || "CHO_XAC_NHAN".equalsIgnoreCase(b.status))
                    .count();
            long completed = masterData.stream()
                    .filter(b -> "DA_TRA".equalsIgnoreCase(b.status) || "DA_TRA_PHONG".equalsIgnoreCase(b.status))
                    .count();

            totalCountLbl.setText(String.valueOf(total));
            stayingCountLbl.setText(String.valueOf(staying));
            reservedCountLbl.setText(String.valueOf(reserved));
            completedCountLbl.setText(String.valueOf(completed));
        };

        // Top Toolbar
        TextField search = new TextField();
        search.setPromptText("Tìm theo ID, tên khách, SĐT, số phòng...");
        search.setPrefWidth(290);
        search.setMinWidth(260);
        search.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-background-color: #121824; -fx-border-color: #232D3F; -fx-border-radius: 6; -fx-text-fill: #FFFFFF; -fx-padding: 7 12;");

        ComboBox<String> roomTypeFilter = new ComboBox<>();
        roomTypeFilter.getItems().add("Tất cả loại phòng");
        roomTypeFilter.setValue("Tất cả loại phòng");
        roomTypeFilter.setPrefWidth(185);
        roomTypeFilter.setMinWidth(175);
        roomTypeFilter.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-background-color: #121824; -fx-border-color: #232D3F; -fx-border-radius: 6; -fx-text-fill: #FFFFFF;");

        ComboBox<String> roomFilter = new ComboBox<>();
        roomFilter.getItems().add("Tất cả phòng");
        roomFilter.setValue("Tất cả phòng");
        roomFilter.setPrefWidth(150);
        roomFilter.setMinWidth(140);
        roomFilter.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-background-color: #121824; -fx-border-color: #232D3F; -fx-border-radius: 6; -fx-text-fill: #FFFFFF;");

        List<RoomType> loadedRoomTypes = new ArrayList<>();
        List<Room> loadedRooms = new ArrayList<>();

        Util.runAsync(ApiService::roomTypes, rTypes -> {
            loadedRoomTypes.clear();
            if (rTypes != null) {
                loadedRoomTypes.addAll(rTypes);
                for (RoomType rt : rTypes) {
                    if (rt.name != null && !rt.name.isBlank()) {
                        roomTypeFilter.getItems().add(rt.name);
                    }
                }
            }
        });

        Runnable updateRoomCombo = () -> {
            String selType = roomTypeFilter.getValue();
            roomFilter.getItems().clear();
            roomFilter.getItems().add("Tất cả phòng");
            for (Room r : loadedRooms) {
                if (selType == null || "Tất cả loại phòng".equals(selType)) {
                    roomFilter.getItems().add("Phòng " + r.number);
                } else if (r.roomType != null && selType.equalsIgnoreCase(r.roomType.name)) {
                    roomFilter.getItems().add("Phòng " + r.number);
                }
            }
            roomFilter.setValue("Tất cả phòng");
        };

        Util.runAsync(ApiService::rooms, rooms -> {
            loadedRooms.clear();
            if (rooms != null) {
                loadedRooms.addAll(rooms);
                updateRoomCombo.run();
            }
        });

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả trạng thái", "Đang lưu trú", "Đã đặt trước", "Đã trả phòng", "Đã hủy");
        statusFilter.setValue("Tất cả trạng thái");
        statusFilter.setPrefWidth(180);
        statusFilter.setMinWidth(170);
        statusFilter.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-background-color: #121824; -fx-border-color: #232D3F; -fx-border-radius: 6; -fx-text-fill: #FFFFFF;");

        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.setGraphic(IconUtils.icon("search", 13, Color.web("#CBD5E1")));
        searchBtn.setGraphicTextGap(6);
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-color: #141A24; -fx-border-color: #2D3A4F; -fx-border-radius: 6; -fx-text-fill: #CBD5E1; -fx-padding: 7 14; -fx-cursor: hand;");

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.setGraphic(IconUtils.icon("refresh", 13, Color.web("#CBD5E1")));
        refreshBtn.setGraphicTextGap(6);
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-color: #141A24; -fx-border-color: #2D3A4F; -fx-border-radius: 6; -fx-text-fill: #CBD5E1; -fx-padding: 7 14; -fx-cursor: hand;");

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID giảm dần", "ID tăng dần");
        sortOrder.setValue("ID giảm dần");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, search, roomTypeFilter, roomFilter, statusFilter, searchBtn, refreshBtn, spacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<Booking> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setItems(pagedData);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Chưa có lượt đặt phòng nào trong hệ thống."));
        table.setStyle("-fx-font-family: 'Inter', sans-serif;");

        // ── Pagination Bar & Logic ────────────────────────────────────────────
        int[] currentPage = new int[]{1};
        int[] currentPageSize = new int[]{PaginationHelper.DEFAULT_PAGE_SIZE};
        PaginationHelper.PaginationBar paginationBar = new PaginationHelper.PaginationBar("đơn đặt phòng");

        // ── Backend Query & Pagination Logic ─────────────────────────────────
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<Integer>[] loadBookingPage = new java.util.function.Consumer[1];
        loadBookingPage[0] = targetPage -> {
            currentPage[0] = Math.max(1, targetPage);
            String q = search.getText() == null ? "" : search.getText().trim();
            String st = statusFilter.getValue();
            String sDir = "ID tăng dần".equals(sortOrder.getValue()) ? "asc" : "desc";

            Long selRoomTypeId = null;
            String selTypeStr = roomTypeFilter.getValue();
            if (selTypeStr != null && !"Tất cả loại phòng".equals(selTypeStr)) {
                for (RoomType rt : loadedRoomTypes) {
                    if (selTypeStr.equalsIgnoreCase(rt.name)) {
                        selRoomTypeId = rt.id;
                        break;
                    }
                }
            }

            Long selRoomId = null;
            String selRoomStr = roomFilter.getValue();
            if (selRoomStr != null && !"Tất cả phòng".equals(selRoomStr)) {
                String rNumClean = selRoomStr.replace("Phòng ", "").trim();
                for (Room r : loadedRooms) {
                    if (rNumClean.equalsIgnoreCase(r.number)) {
                        selRoomId = r.id;
                        break;
                    }
                }
            }

            final Long finalTypeId = selRoomTypeId;
            final Long finalRoomId = selRoomId;

            Util.runAsync(() -> ApiService.bookingPage(currentPage[0], currentPageSize[0], q, st, sDir, finalTypeId, finalRoomId), page -> {
                pagedData.setAll(page.getContent() != null ? page.getContent() : List.of());
                table.scrollTo(0);

                int totalPages = Math.max(1, page.totalPages);
                long totalElements = page.totalElements;
                currentPage[0] = page.pageNo;

                paginationBar.update(currentPage[0], currentPageSize[0], (int) totalElements, newPage -> {
                    loadBookingPage[0].accept(newPage);
                });
            });
        };

        Runnable loadCurrentPage = () -> loadBookingPage[0].accept(currentPage[0]);
        Runnable doSearch = () -> loadBookingPage[0].accept(1);

        // Chỉ tìm kiếm khi người dùng bấm nút Tìm kiếm hoặc gõ Enter hoặc đổi dropdown
        searchBtn.setOnAction(e -> doSearch.run());
        search.setOnAction(e -> doSearch.run());

        roomTypeFilter.setOnAction(e -> {
            updateRoomCombo.run();
            doSearch.run();
        });
        roomFilter.setOnAction(e -> doSearch.run());
        statusFilter.setOnAction(e -> doSearch.run());

        refreshBtn.setOnAction(e -> {
            search.clear();
            roomTypeFilter.setValue("Tất cả loại phòng");
            updateRoomCombo.run();
            roomFilter.setValue("Tất cả phòng");
            statusFilter.setValue("Tất cả trạng thái");
            doSearch.run();
        });

        paginationBar.getPageSizeCombo().setOnAction(e -> {
            Integer selectedSize = paginationBar.getPageSizeCombo().getValue();
            if (selectedSize != null && selectedSize != currentPageSize[0]) {
                currentPageSize[0] = selectedSize;
                doSearch.run();
            }
        });

        TableColumn<Booking, String> idCol = new TableColumn<>();
        idCol.setMinWidth(75);
        idCol.setPrefWidth(85);
        Label idHeader = new Label("ID ▼");
        idHeader.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-weight: 800; -fx-cursor: hand; -fx-text-fill: #F59E0B; -fx-font-size: 13px;");
        idCol.setGraphic(idHeader);
        idCol.setSortable(false);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().id != null ? String.valueOf(d.getValue().id) : "—"));
        idCol.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-weight: 800; -fx-text-fill: #F59E0B; -fx-font-size: 13.5px; -fx-alignment: CENTER_LEFT;");

        idHeader.setOnMouseClicked(e -> {
            if ("ID giảm dần".equals(sortOrder.getValue())) {
                sortOrder.setValue("ID tăng dần");
            } else {
                sortOrder.setValue("ID giảm dần");
            }
        });
        sortOrder.valueProperty().addListener((obs, oldV, newV) -> {
            idHeader.setText("ID " + ("ID giảm dần".equals(newV) ? "▼" : "▲"));
            loadBookingPage[0].accept(1);
        });

        TableColumn<Booking, Void> custCol = new TableColumn<>("KHÁCH HÀNG");
        custCol.setMinWidth(200);
        custCol.setPrefWidth(225);
        custCol.setCellFactory(col -> new TableCell<>() {
            private final VBox box = new VBox(2);
            private final Label nameLbl = new Label();
            private final Label phoneLbl = new Label();

            {
                nameLbl.setStyle(
                        "-fx-font-family: 'Inter', sans-serif; -fx-font-weight: 700; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
                phoneLbl.setStyle(
                        "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-text-fill: #8C9CB2;");
                box.getChildren().addAll(nameLbl, phoneLbl);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Booking b = getTableView().getItems().get(getIndex());
                    if (b.customer != null) {
                        nameLbl.setText(b.customer.fullName != null ? b.customer.fullName : "Khách vãng lai");
                        phoneLbl.setText(
                                "📞 " + (b.customer.phone != null && !b.customer.phone.isBlank() ? b.customer.phone
                                        : "Chưa có SĐT"));
                    } else {
                        nameLbl.setText("—");
                        phoneLbl.setText("");
                    }
                    setGraphic(box);
                }
            }
        });

        TableColumn<Booking, Void> roomCol = new TableColumn<>("PHÒNG");
        roomCol.setMinWidth(140);
        roomCol.setPrefWidth(160);
        roomCol.setCellFactory(col -> new TableCell<>() {
            private final VBox box = new VBox(2);
            private final Label roomLbl = new Label();
            private final Label typeLbl = new Label();

            {
                roomLbl.setStyle(
                        "-fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 800; -fx-text-fill: #FCD34D; -fx-font-size: 13px;");
                typeLbl.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                box.getChildren().addAll(roomLbl, typeLbl);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Booking b = getTableView().getItems().get(getIndex());
                    if (b.room != null) {
                        roomLbl.setText("Phòng " + b.room.number);
                        typeLbl.setText(b.room.roomType != null ? b.room.roomType.name : "Tiêu chuẩn");
                    } else {
                        roomLbl.setText("—");
                        typeLbl.setText("");
                    }
                    setGraphic(box);
                }
            }
        });

        TableColumn<Booking, String> guestsCol = new TableColumn<>("SỐ KHÁCH");
        guestsCol.setMinWidth(90);
        guestsCol.setPrefWidth(100);
        guestsCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().numberOfGuests != null ? d.getValue().numberOfGuests : 1) + " người"));
        guestsCol.setStyle("-fx-alignment: CENTER_LEFT; -fx-font-weight: 600; -fx-text-fill: #E2E8F0;");

        TableColumn<Booking, String> inCol = new TableColumn<>("NGÀY NHẬN");
        inCol.setMinWidth(110);
        inCol.setPrefWidth(120);
        inCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().checkIn != null ? d.getValue().checkIn : "—"));
        inCol.setStyle("-fx-text-fill: #E2E8F0;");

        TableColumn<Booking, String> outCol = new TableColumn<>("DỰ KIẾN TRẢ");
        outCol.setMinWidth(110);
        outCol.setPrefWidth(120);
        outCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().checkOutPlanned != null ? d.getValue().checkOutPlanned : "—"));
        outCol.setStyle("-fx-text-fill: #E2E8F0;");

        TableColumn<Booking, String> actInCol = new TableColumn<>("NHẬN THỰC TẾ");
        actInCol.setMinWidth(135);
        actInCol.setPrefWidth(145);
        actInCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().actualCheckIn != null ? d.getValue().actualCheckIn : "Chưa nhận"));
        actInCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if ("Chưa nhận".equals(item)) {
                        setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #34D399; -fx-font-weight: 600;");
                    }
                }
            }
        });

        TableColumn<Booking, String> actOutCol = new TableColumn<>("TRẢ THỰC TẾ");
        actOutCol.setMinWidth(135);
        actOutCol.setPrefWidth(145);
        actOutCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().actualCheckOut != null ? d.getValue().actualCheckOut : "Chưa trả"));
        actOutCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if ("Chưa trả".equals(item)) {
                        setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #60A5FA; -fx-font-weight: 600;");
                    }
                }
            }
        });

        TableColumn<Booking, String> statusCol = new TableColumn<>("TRẠNG THÁI");
        statusCol.setMinWidth(140);
        statusCol.setPrefWidth(150);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().statusLabel()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Booking b = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", b.badgeClass());
                    badge.setStyle(
                            "-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-padding: 4 12; -fx-background-radius: 6;");
                    badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                    badge.setWrapText(false);
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<Booking, String> noteCol = new TableColumn<>("GHI CHÚ");
        noteCol.setPrefWidth(150);
        noteCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().notes != null && !d.getValue().notes.isBlank() ? d.getValue().notes : "—"));
        noteCol.setStyle("-fx-text-fill: #94A3B8;");

        // Action Column: Check-in, Check-out, Edit, Delete, Service, Cancel
        TableColumn<Booking, Void> actionCol = new TableColumn<>("THAO TÁC");
        actionCol.setMinWidth(460);
        actionCol.setPrefWidth(470);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button checkInBtn = new Button("Nhận phòng");
            private final Button checkOutBtn = new Button("Trả phòng");
            private final Button cancelBtn = new Button("Hủy");
            private final Button serviceBtn = new Button("+ Dịch vụ");
            private final Button editBtn = new Button("Sửa");
            private final Button delBtn = new Button("Xóa");
            private final Label completedLbl = new Label("Đã hoàn tất");
            private final HBox box = new HBox(6);

            {
                checkInBtn.getStyleClass().add("btn-gold");
                checkInBtn.setGraphic(IconUtils.icon("check", 11, Color.web("#0A0D14")));
                checkInBtn.setGraphicTextGap(5);
                checkInBtn.setMinWidth(Region.USE_PREF_SIZE);
                checkInBtn.setStyle(
                        "-fx-padding: 4 10; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-color: #F59E0B; -fx-text-fill: #0A0D14; -fx-background-radius: 6; -fx-cursor: hand;");

                checkOutBtn.setStyle(
                        "-fx-padding: 4 10; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-color: #10B981; -fx-text-fill: #FFFFFF; -fx-background-radius: 6; -fx-cursor: hand;");
                checkOutBtn.setGraphic(IconUtils.icon("exit", 11, Color.web("#FFFFFF")));
                checkOutBtn.setGraphicTextGap(5);
                checkOutBtn.setMinWidth(Region.USE_PREF_SIZE);

                editBtn.getStyleClass().add("btn-action-edit");
                editBtn.setGraphic(IconUtils.icon("edit", 11, Color.web("#FFFFFF")));
                editBtn.setGraphicTextGap(4);
                editBtn.setMinWidth(Region.USE_PREF_SIZE);
                editBtn.setStyle(
                        "-fx-padding: 4 9; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-color: #3B82F6; -fx-text-fill: #FFFFFF; -fx-background-radius: 6; -fx-cursor: hand;");

                delBtn.getStyleClass().add("btn-danger");
                delBtn.setGraphic(IconUtils.icon("trash", 11, Color.web("#FFFFFF")));
                delBtn.setGraphicTextGap(4);
                delBtn.setMinWidth(Region.USE_PREF_SIZE);
                delBtn.setStyle(
                        "-fx-padding: 4 9; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-color: #EF4444; -fx-text-fill: #FFFFFF; -fx-background-radius: 6; -fx-cursor: hand;");

                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setGraphic(IconUtils.icon("cancel", 11, Color.web("#F87171")));
                cancelBtn.setGraphicTextGap(5);
                cancelBtn.setMinWidth(Region.USE_PREF_SIZE);
                cancelBtn.setStyle(
                        "-fx-padding: 4 9; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #F87171; -fx-border-color: rgba(239, 68, 68, 0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

                serviceBtn.getStyleClass().add("btn-outline");
                serviceBtn.setGraphic(IconUtils.icon("bell", 11, Color.web("#38BDF8")));
                serviceBtn.setGraphicTextGap(4);
                serviceBtn.setMinWidth(Region.USE_PREF_SIZE);
                serviceBtn.setStyle(
                        "-fx-padding: 4 9; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-border-color: rgba(56, 189, 248, 0.4); -fx-text-fill: #38BDF8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;");

                completedLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: 600;");
                completedLbl.setMinWidth(Region.USE_PREF_SIZE);

                box.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);

                checkInBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    if (b.room != null) {
                        String rStat = b.room.status != null ? b.room.status.toUpperCase() : "";
                        if ("BAO_TRI".equals(rStat) || "DANG_BAO_TRI".equals(rStat)) {
                            Util.error("Không thể nhận phòng! Phòng " + b.room.number + " hiện đang bảo trì.");
                            return;
                        }
                        if ("DANG_DON".equals(rStat)) {
                            Util.error("Không thể nhận phòng! Phòng " + b.room.number + " đang dọn dẹp, chưa sẵn sàng đón khách.");
                            return;
                        }
                        if ("DANG_O".equals(rStat) && !"DANG_O".equalsIgnoreCase(b.status)) {
                            Util.error("Không thể nhận phòng! Phòng " + b.room.number + " hiện đang có khách lưu trú.");
                            return;
                        }
                    }
                    openCheckInDialog(b, loadCurrentPage);
                });

                checkOutBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận trả phòng");
                    alert.setHeaderText("Xác nhận trả phòng " + (b.room != null ? b.room.number : "") + "?");
                    alert.setContentText("Trạng thái phòng sẽ được chuyển sang 'Đang dọn'.");
                    Util.applyTheme(alert);
                    alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
                        Util.runAsync(() -> ApiService.checkOutBooking(b.id), res -> loadCurrentPage.run());
                    });
                });

                editBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    openEditDialog(b, loadCurrentPage);
                });

                delBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    confirmDelete(b, loadCurrentPage);
                });

                cancelBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận hủy đặt phòng");
                    String idInfo = b.id != null ? "ID: #" + b.id : "";
                    alert.setHeaderText("Hủy lượt đặt phòng " + idInfo + "?");
                    alert.setContentText("Bạn có chắc chắn muốn hủy đặt phòng này không?");
                    Util.applyTheme(alert);
                    alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
                        Util.runAsync(() -> ApiService.cancelBooking(b.id), res -> loadCurrentPage.run());
                    });
                });

                serviceBtn.setOnAction(e -> {
                    Booking b = getTableView().getItems().get(getIndex());
                    BookingServiceDetailsView.openAddDialog(b.id, loadCurrentPage);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Booking b = getTableView().getItems().get(getIndex());
                    box.getChildren().clear();

                    String st = b.status != null ? b.status.toUpperCase() : "DAT_TRUOC";
                    if ("DAT_TRUOC".equals(st) || "CHO_XAC_NHAN".equals(st)) {
                        box.getChildren().addAll(checkInBtn, serviceBtn, editBtn, delBtn, cancelBtn);
                    } else if ("DANG_O".equals(st) || "DANG_SU_DUNG".equals(st)) {
                        box.getChildren().addAll(checkOutBtn, serviceBtn, editBtn, delBtn);
                    } else if ("DA_TRA".equals(st) || "DA_TRA_PHONG".equals(st)) {
                        completedLbl.setText("✓ Đã hoàn tất");
                        completedLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981; -fx-font-weight: 600;");
                        box.getChildren().addAll(completedLbl, serviceBtn, editBtn, delBtn);
                    } else if ("DA_HUY".equals(st) || "HUY".equals(st)) {
                        completedLbl.setText("✕ Đã hủy");
                        completedLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444; -fx-font-weight: 600;");
                        box.getChildren().addAll(completedLbl, editBtn, delBtn);
                    }
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(idCol, custCol, roomCol, guestsCol, inCol, outCol, actInCol, actOutCol, statusCol,
                noteCol, actionCol);

        // Reload logic
        Runnable refreshAll = () -> {
            doSearch.run();
            Util.runAsync(ApiService::bookings, list -> {
                masterData.setAll(list);
                updateKpi.run();
            });
        };
        refreshAll.run();

        refreshBtn.setOnAction(e -> {
            search.clear();
            statusFilter.setValue("Tất cả trạng thái");
            sortOrder.setValue("ID giảm dần");
            refreshAll.run();
        });

        topAddBtn.setOnAction(e -> openAddDialog(refreshAll));

        container.getChildren().addAll(headerRow, kpiGrid, toolbar, table, paginationBar);
        root.setCenter(container);
        return root;
    }

    private static GridPane buildKpiRow(Label totalLbl, Label stayingLbl, Label reservedLbl, Label completedLbl) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25.0);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        grid.add(
                buildKpiCard("TỔNG ĐẶT PHÒNG", totalLbl, "Tất cả lượt đặt",
                        IconUtils.icon("document", 13, Color.web("#F59E0B")), "rgba(245, 158, 11, 0.15)", "#F59E0B"),
                0, 0);
        grid.add(buildKpiCard("ĐANG LƯU TRÚ", stayingLbl, "Khách đang ở phòng",
                IconUtils.icon("bed", 13, Color.web("#8B5CF6")), "rgba(139, 92, 246, 0.15)", "#A78BFA"), 1, 0);
        grid.add(
                buildKpiCard("ĐÃ ĐẶT TRƯỚC", reservedLbl, "Chờ khách nhận phòng",
                        IconUtils.icon("clock", 13, Color.web("#38BDF8")), "rgba(56, 189, 248, 0.15)", "#38BDF8"),
                2, 0);
        grid.add(
                buildKpiCard("ĐÃ TRẢ PHÒNG", completedLbl, "Lưu trú hoàn tất",
                        IconUtils.icon("check", 13, Color.web("#10B981")), "rgba(16, 185, 129, 0.15)", "#34D399"),
                3, 0);

        return grid;
    }

    private static VBox buildKpiCard(String title, Label valLbl, String subtext, Node icon, String badgeBg,
            String valColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 12, 16));
        card.setStyle(
                "-fx-background-color: #121824; -fx-border-color: #1E293B; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(title);
        l.setStyle(
                "-fx-text-fill: #8C9CB2; -fx-font-size: 11px; -fx-font-weight: 700; -fx-font-family: 'Inter', sans-serif;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        StackPane badge = new StackPane(icon);
        badge.setPrefSize(26, 26);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-background-radius: 6;");

        top.getChildren().addAll(l, sp, badge);

        valLbl.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: "
                        + valColor + ";");

        Label sub = new Label(subtext);
        sub.setStyle("-fx-text-fill: #8C9CB2; -fx-font-size: 11px; -fx-font-family: 'Inter', sans-serif;");

        card.getChildren().addAll(top, valLbl, sub);
        return card;
    }

    private static void reload(ObservableList<Booking> masterData, Runnable applyFilter) {
        Util.runAsync(ApiService::bookings, list -> {
            masterData.setAll(list);
            applyFilter.run();
        });
    }

    private static void openAddDialog(Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đặt phòng & Đăng ký lưu trú mới");
        dialog.setHeaderText("Tạo lượt đặt phòng mới cho khách hàng");
        dialog.getDialogPane().getStylesheets().add(
                BookingsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());

        // Choice: Khách hàng có sẵn hoặc khách hàng mới
        RadioButton rbExisting = new RadioButton("Khách hàng đã có trong hệ thống");
        RadioButton rbNew = new RadioButton("Nhập thông tin khách hàng mới");
        rbExisting.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        rbNew.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        ToggleGroup custGroup = new ToggleGroup();
        rbExisting.setToggleGroup(custGroup);
        rbNew.setToggleGroup(custGroup);
        rbExisting.setSelected(true);
        rbExisting.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");
        rbNew.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600;");

        HBox custTypeRow = new HBox(16, rbExisting, rbNew);
        custTypeRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Customer> customerBox = new ComboBox<>();
        customerBox.setPromptText("— Chọn khách hàng có sẵn —");
        customerBox.setMaxWidth(Double.MAX_VALUE);
        Util.runAsync(() -> ApiService.customers(null), customerBox.getItems()::setAll);

        TextField newName = new TextField();
        newName.setPromptText("Họ và tên khách hàng (*)");
        TextField newPhone = new TextField();
        newPhone.setPromptText("Số điện thoại khách hàng (*)");
        TextField newCitizenId = new TextField();
        newCitizenId.setPromptText("Số CCCD / Hộ chiếu (12 số)");

        VBox existingBox = new VBox(6, customerBox);
        VBox newBox = new VBox(8, newName, newPhone, newCitizenId);
        newBox.setVisible(false);
        newBox.setManaged(false);

        custGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isNew = newVal == rbNew;
            existingBox.setVisible(!isNew);
            existingBox.setManaged(!isNew);
            newBox.setVisible(isNew);
            newBox.setManaged(isNew);
        });

        ComboBox<Room> roomBox = new ComboBox<>();
        roomBox.setPromptText("— Chọn phòng trống —");
        roomBox.setMaxWidth(Double.MAX_VALUE);
        Util.runAsync(ApiService::rooms, rooms -> rooms.stream()
                .filter(r -> "TRONG".equals(r.status))
                .forEach(roomBox.getItems()::add));

        DatePicker checkIn = new DatePicker(LocalDate.now());
        DatePicker checkOut = new DatePicker(LocalDate.now().plusDays(1));
        checkIn.setPrefWidth(200);
        checkOut.setPrefWidth(200);

        Spinner<Integer> guestsSpinner = new Spinner<>(1, 10, 1);
        guestsSpinner.setEditable(true);
        guestsSpinner.setPrefWidth(120);

        CheckBox checkInNow = new CheckBox("Nhận phòng ngay (Khách làm thủ tục Check-in trực tiếp)");
        checkInNow.setSelected(true);
        checkInNow.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 700;");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Ghi chú đặc biệt (yêu cầu phòng tầng cao, giường phụ...)");
        notesArea.setPrefRowCount(2);

        VBox form = new VBox(12,
                labeledBox("LỰA CHỌN KHÁCH HÀNG", custTypeRow),
                existingBox,
                newBox,
                labeledBox("PHÒNG TRỐNG (*)", roomBox),
                new HBox(12, labeledBox("NGÀY NHẬN (*)", checkIn), labeledBox("NGÀY TRẢ DỰ KIẾN (*)", checkOut),
                        labeledBox("SỐ KHÁCH (*)", guestsSpinner)),
                checkInNow,
                labeledBox("GHI CHÚ", notesArea));
        form.setPadding(new Insets(18));
        form.setPrefWidth(600);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Xác nhận đặt phòng");
        okBtn.getStyleClass().add("btn-gold");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Hủy bỏ");
        }

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                if (roomBox.getValue() == null) {
                    Util.error("Vui lòng chọn phòng trống.");
                    return null;
                }

                Room selRoom = roomBox.getValue();
                String rStatus = selRoom.status != null ? selRoom.status.toUpperCase() : "";
                if ("BAO_TRI".equals(rStatus) || "DANG_BAO_TRI".equals(rStatus)) {
                    Util.error("Không thể đặt phòng! Phòng " + selRoom.number + " hiện đang bảo trì.");
                    return null;
                }
                if (checkInNow.isSelected() && !"TRONG".equals(rStatus)) {
                    Util.error("Không thể nhận phòng ngay! Phòng " + selRoom.number + " chưa sẵn sàng (phải ở trạng thái trống).");
                    return null;
                }

                Long customerId = null;
                String cName = null;
                String cPhone = null;
                String cCccd = null;

                if (rbExisting.isSelected()) {
                    if (customerBox.getValue() == null) {
                        Util.error("Vui lòng chọn khách hàng có sẵn hoặc chuyển sang nhập khách mới.");
                        return null;
                    }
                    customerId = customerBox.getValue().id;
                } else {
                    cName = newName.getText().trim();
                    cPhone = newPhone.getText().trim();
                    cCccd = newCitizenId.getText().trim();
                    if (cName.isBlank()) {
                        Util.error("Họ và tên khách hàng không được để trống.");
                        return null;
                    }
                    if (cPhone.isBlank()) {
                        Util.error("Số điện thoại không được để trống.");
                        return null;
                    }
                }

                if (checkIn.getValue() == null || checkOut.getValue() == null) {
                    Util.error("Vui lòng chọn ngày nhận và ngày trả phòng.");
                    return null;
                }

                if (checkOut.getValue().isBefore(checkIn.getValue())) {
                    Util.error("Ngày trả phòng dự kiến phải bằng hoặc sau ngày nhận phòng.");
                    return null;
                }

                if (selRoom != null && selRoom.roomType != null && selRoom.roomType.maxGuests != null && guestsSpinner.getValue() != null) {
                    if (guestsSpinner.getValue() > selRoom.roomType.maxGuests) {
                        Util.error("Số lượng khách (" + guestsSpinner.getValue() + " người) vượt quá số người tối đa cho phép của phòng " + selRoom.number + " (" + selRoom.roomType.maxGuests + " người)!");
                        return null;
                    }
                }

                final Long finalCustomerId = customerId;
                final String finalName = cName;
                final String finalPhone = cPhone;
                final String finalCccd = cCccd;
                final Long roomId = roomBox.getValue().id;
                final String inStr = checkIn.getValue().toString();
                final String outStr = checkOut.getValue().toString();
                final Integer guests = guestsSpinner.getValue();
                final String notes = notesArea.getText().trim();
                final boolean isNow = checkInNow.isSelected();

                Util.runAsync(() -> ApiService.createBooking(
                        finalCustomerId, finalName, finalPhone, finalCccd,
                        roomId, inStr, outStr, guests, notes, isNow), b -> reload.run());
            }
            return null;
        });
        dialog.showAndWait();
    }

    private static void confirmDelete(Booking b, Runnable reload) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa đặt phòng");
        String idInfo = b.id != null ? "ID: #" + b.id : "";
        alert.setHeaderText("Xác nhận xóa đơn đặt phòng " + idInfo + "?");
        alert.setContentText("Bạn có chắc chắn muốn xóa đơn đặt phòng này không? Dữ liệu đã xóa sẽ không thể phục hồi.");
        Util.applyTheme(alert);
        alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            Util.runAsyncVoid(() -> ApiService.deleteBooking(b.id), reload);
        });
    }

    private static void openEditDialog(Booking existing, Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cập nhật thông tin đặt phòng");
        String rName = (existing.room != null && existing.room.number != null) ? " — Phòng " + existing.room.number : "";
        String cName = (existing.customer != null && existing.customer.fullName != null) ? " — " + existing.customer.fullName : "";
        dialog.setHeaderText("Chỉnh sửa thông tin đơn đặt phòng" + rName + cName);
        dialog.getDialogPane().getStylesheets().add(
                BookingsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());

        ComboBox<Customer> customerBox = new ComboBox<>();
        customerBox.setPromptText("— Chọn khách hàng —");
        customerBox.setMaxWidth(Double.MAX_VALUE);
        customerBox.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
        Util.runAsync(() -> ApiService.customers(null), customers -> {
            customerBox.getItems().setAll(customers);
            if (existing.customer != null && existing.customer.id != null) {
                customers.stream()
                        .filter(c -> c.id != null && c.id.equals(existing.customer.id))
                        .findFirst()
                        .ifPresent(customerBox::setValue);
            }
        });

        ComboBox<Room> roomBox = new ComboBox<>();
        roomBox.setPromptText("— Chọn phòng —");
        roomBox.setMaxWidth(Double.MAX_VALUE);
        roomBox.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
        Util.runAsync(ApiService::rooms, rooms -> {
            List<Room> selectable = rooms.stream()
                    .filter(r -> "TRONG".equals(r.status) || (existing.room != null && existing.room.id != null && existing.room.id.equals(r.id)))
                    .collect(Collectors.toList());
            roomBox.getItems().setAll(selectable);
            if (existing.room != null && existing.room.id != null) {
                selectable.stream()
                        .filter(r -> r.id != null && r.id.equals(existing.room.id))
                        .findFirst()
                        .ifPresent(roomBox::setValue);
            }
        });

        DatePicker checkIn = new DatePicker(parseDateSafe(existing.checkIn));
        DatePicker checkOut = new DatePicker(parseDateSafe(existing.checkOutPlanned));
        checkIn.setPrefWidth(210);
        checkOut.setPrefWidth(210);
        checkIn.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
        checkOut.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        Spinner<Integer> guestsSpinner = new Spinner<>(1, 10, existing.numberOfGuests != null ? existing.numberOfGuests : 1);
        guestsSpinner.setEditable(true);
        guestsSpinner.setPrefWidth(130);
        guestsSpinner.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Đã đặt trước", "Đang lưu trú", "Đã trả phòng", "Đã hủy");
        statusBox.setValue(existing.statusLabel());
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        TextArea notesArea = new TextArea(existing.notes != null ? existing.notes : "");
        notesArea.setPromptText("Ghi chú đặc biệt (yêu cầu phòng tầng cao, giường phụ...)");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #FFFFFF;");

        VBox form = new VBox(14,
                labeledBox("KHÁCH HÀNG (*)", customerBox),
                labeledBox("PHÒNG LƯU TRÚ (*)", roomBox),
                new HBox(12, labeledBox("NGÀY NHẬN DỰ KIẾN (*)", checkIn), labeledBox("NGÀY TRẢ DỰ KIẾN (*)", checkOut),
                        labeledBox("SỐ KHÁCH (*)", guestsSpinner)),
                labeledBox("TRẠNG THÁI", statusBox),
                labeledBox("GHI CHÚ", notesArea));
        form.setPadding(new Insets(20));
        form.setPrefWidth(640);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Lưu thay đổi");
        okBtn.getStyleClass().add("btn-gold");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Hủy bỏ");
        }

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Long customerId = customerBox.getValue() != null ? customerBox.getValue().id : (existing.customer != null ? existing.customer.id : null);
                if (customerId == null) {
                    Util.error("Vui lòng chọn khách hàng.");
                    return null;
                }

                Long roomId = roomBox.getValue() != null ? roomBox.getValue().id : (existing.room != null ? existing.room.id : null);
                if (roomId == null) {
                    Util.error("Vui lòng chọn phòng.");
                    return null;
                }

                Room selRoomEdit = roomBox.getValue();
                if (selRoomEdit != null) {
                    String rStat = selRoomEdit.status != null ? selRoomEdit.status.toUpperCase() : "";
                    if ("BAO_TRI".equals(rStat) || "DANG_BAO_TRI".equals(rStat)) {
                        Util.error("Không thể gán phòng " + selRoomEdit.number + " vì phòng đang trong trạng thái bảo trì!");
                        return null;
                    }
                }

                if (checkIn.getValue() == null || checkOut.getValue() == null) {
                    Util.error("Vui lòng chọn ngày nhận và ngày trả phòng.");
                    return null;
                }

                if (checkOut.getValue().isBefore(checkIn.getValue())) {
                    Util.error("Ngày trả phòng dự kiến phải bằng hoặc sau ngày nhận phòng.");
                    return null;
                }

                if (selRoomEdit != null && selRoomEdit.roomType != null && selRoomEdit.roomType.maxGuests != null && guestsSpinner.getValue() != null) {
                    if (guestsSpinner.getValue() > selRoomEdit.roomType.maxGuests) {
                        Util.error("Số lượng khách (" + guestsSpinner.getValue() + " người) vượt quá số người tối đa cho phép của phòng " + selRoomEdit.number + " (" + selRoomEdit.roomType.maxGuests + " người)!");
                        return null;
                    }
                }

                String stKey = switch (statusBox.getValue() != null ? statusBox.getValue() : "") {
                    case "Đang lưu trú" -> "DANG_O";
                    case "Đã trả phòng" -> "DA_TRA";
                    case "Đã hủy" -> "DA_HUY";
                    default -> "DAT_TRUOC";
                };

                final Long finalCustId = customerId;
                final Long finalRoomId = roomId;
                final String inStr = checkIn.getValue().toString();
                final String outStr = checkOut.getValue().toString();
                final Integer guests = guestsSpinner.getValue();
                final String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

                if ("DANG_O".equals(stKey) && !"DANG_O".equalsIgnoreCase(existing.status) && selRoomEdit != null) {
                    String rStat = selRoomEdit.status != null ? selRoomEdit.status.toUpperCase() : "";
                    if (!"TRONG".equals(rStat)) {
                        Util.error("Không thể chuyển sang trạng thái Đang lưu trú! Phòng " + selRoomEdit.number + " chưa sẵn sàng (phải ở trạng thái trống).");
                        return null;
                    }
                }

                Util.runAsync(() -> ApiService.updateBooking(
                        existing.id, finalCustId, finalRoomId, inStr, outStr, guests, notes, stKey),
                        b -> reload.run());
            }
            return null;
        });
        dialog.showAndWait();
    }

    private static void openCheckInDialog(Booking existing, Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận nhận phòng & Bàn giao phòng");
        String rNum = (existing.room != null && existing.room.number != null) ? "Phòng " + existing.room.number : "";
        String cName = (existing.customer != null && existing.customer.fullName != null) ? " — " + existing.customer.fullName : "";
        dialog.setHeaderText("Làm thủ tục nhận phòng " + rNum + cName);
        dialog.getDialogPane().getStylesheets().add(
                BookingsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());

        // Khách hàng: Không được sửa khi nhận phòng
        String custInfo = (existing.customer != null)
                ? (existing.customer.fullName != null ? existing.customer.fullName : "Khách vãng lai")
                  + ((existing.customer.phone != null && !existing.customer.phone.isBlank()) ? " (" + existing.customer.phone + ")" : "")
                : "Khách vãng lai";
        TextField customerField = new TextField(custInfo);
        customerField.setDisable(true);
        customerField.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF; -fx-background-color: #141C2A; -fx-border-color: #2D3A4F; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-opacity: 1.0;");

        // Phòng: Không được sửa khi nhận phòng
        String roomInfo = (existing.room != null)
                ? "Phòng " + existing.room.number + (existing.room.roomType != null ? " — " + existing.room.roomType.name : "")
                : "Chưa chỉ định phòng";
        TextField roomField = new TextField(roomInfo);
        roomField.setDisable(true);
        roomField.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #FCD34D; -fx-background-color: #141C2A; -fx-border-color: #2D3A4F; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-opacity: 1.0;");

        // Ngày nhận phòng thực tế: Lấy ngày và giờ hiện tại khi ấn nút nhận phòng
        LocalDateTime now = LocalDateTime.now();
        String formattedNow = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        TextField actualInField = new TextField(formattedNow);
        actualInField.setDisable(true);
        actualInField.setPrefWidth(220);
        actualInField.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #34D399; -fx-background-color: #0E2520; -fx-border-color: #059669; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-opacity: 1.0;");

        // Ngày trả thực tế: Cho phép chọn ngày trả
        LocalDate defaultOut = parseDateSafe(existing.checkOutPlanned);
        if (defaultOut == null || defaultOut.isBefore(now.toLocalDate())) {
            defaultOut = now.toLocalDate().plusDays(1);
        }
        DatePicker actualOutPicker = new DatePicker(defaultOut);
        actualOutPicker.setPrefWidth(220);
        actualOutPicker.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        // Số khách: Không được sửa khi nhận phòng
        Spinner<Integer> guestsSpinner = new Spinner<>(1, 20, existing.numberOfGuests != null ? existing.numberOfGuests : 1);
        guestsSpinner.setDisable(true);
        guestsSpinner.setPrefWidth(130);
        guestsSpinner.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-opacity: 1.0;");

        // Trạng thái mới
        TextField statusField = new TextField("Đang lưu trú (Đang ở)");
        statusField.setDisable(true);
        statusField.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #C4B5FD; -fx-background-color: #1E1B4B; -fx-border-color: #6D28D9; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-opacity: 1.0;");

        // Ghi chú
        TextArea notesArea = new TextArea(existing.notes != null ? existing.notes : "");
        notesArea.setPromptText("Ghi chú khi làm thủ tục nhận phòng (tiền cọc, giấy tờ tùy thân, yêu cầu riêng...)");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #FFFFFF;");

        VBox form = new VBox(14,
                labeledBox("KHÁCH HÀNG (Cố định)", customerField),
                labeledBox("PHÒNG LƯU TRÚ (Cố định)", roomField),
                new HBox(12,
                        labeledBox("NGÀY NHẬN PHÒNG THỰC TẾ (*)", actualInField),
                        labeledBox("NGÀY TRẢ THỰC TẾ (*)", actualOutPicker),
                        labeledBox("SỐ KHÁCH (Cố định)", guestsSpinner)),
                labeledBox("TRẠNG THÁI MỚI", statusField),
                labeledBox("GHI CHÚ KHI NHẬN PHÒNG", notesArea));
        form.setPadding(new Insets(20));
        form.setPrefWidth(640);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        submitBtn.setText("Nhận phòng");
        submitBtn.getStyleClass().add("btn-gold");
        submitBtn.setGraphic(IconUtils.icon("check", 13, Color.web("#0A0D14")));
        submitBtn.setGraphicTextGap(6);

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Hủy bỏ");
        }

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                if (actualOutPicker.getValue() == null) {
                    Util.error("Vui lòng chọn ngày trả thực tế.");
                    return null;
                }
                if (actualOutPicker.getValue().isBefore(now.toLocalDate())) {
                    Util.error("Ngày trả thực tế phải bằng hoặc sau ngày nhận phòng hôm nay (" + now.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
                    return null;
                }

                String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

                Util.runAsync(() -> ApiService.checkInBooking(existing.id, now, actualOutPicker.getValue(), notes),
                        res -> {
                            Util.success("Nhận phòng " + (res.room != null ? res.room.number : (existing.room != null ? existing.room.number : "")) + " thành công!");
                            reload.run();
                        });
            }
            return null;
        });

        dialog.showAndWait();
    }

    private static LocalDate parseDateSafe(String str) {
        if (str == null || str.isBlank() || "—".equals(str)) return LocalDate.now();
        try {
            if (str.contains("/")) {
                String[] p = str.split("/");
                if (p.length == 3) {
                    return LocalDate.of(Integer.parseInt(p[2].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[0].trim()));
                }
            } else if (str.contains("-")) {
                return LocalDate.parse(str.trim());
            }
        } catch (Exception ignored) {}
        return LocalDate.now();
    }

    private static VBox labeledBox(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #E2E8F0;");
        l.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        l.setWrapText(false);
        return new VBox(5, l, field);
    }
}

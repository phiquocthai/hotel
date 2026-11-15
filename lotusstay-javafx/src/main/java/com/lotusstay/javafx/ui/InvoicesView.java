package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.*;
import com.lotusstay.javafx.net.ApiService;
import com.lotusstay.javafx.net.PageResponse;
import com.lotusstay.javafx.net.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class InvoicesView {

    private static final int DEFAULT_PAGE_SIZE = 10;

    public static BorderPane build() {
        final Runnable[] reloadPageRef = new Runnable[1];
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(16);
        card.getStyleClass().add("panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Quản lý hóa đơn & Tài chính");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");

        Label subtitle = new Label("Theo dõi doanh thu phòng, thanh toán dịch vụ & đối soát công nợ khách hàng");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8;");
        titleBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button exportBtn = new Button("📊 Xuất CSV");
        exportBtn.getStyleClass().add("btn-outline");

        Button addBtn = new Button("+ Tạo hóa đơn");
        addBtn.getStyleClass().add("btn-gold");

        headerRow.getChildren().addAll(titleBox, topSpacer, exportBtn, addBtn);

        // Data collection
        ObservableList<Invoice> tableData = FXCollections.observableArrayList();

        // Top Toolbar
        TextField search = new TextField();
        search.setPromptText("Tìm ID, tên khách, nhân viên, phòng, SĐT, CCCD...");
        search.setMinWidth(320);
        search.setPrefWidth(340);
        search.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #FFFFFF; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả thanh toán", "Đã thanh toán", "Thanh toán một phần", "Chưa thanh toán");
        statusFilter.setValue("Tất cả thanh toán");
        statusFilter.setPrefWidth(215);
        statusFilter.setMinWidth(205);
        statusFilter.setStyle("-fx-background-color: #0F172A; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        DatePicker fromDate = new DatePicker();
        fromDate.setPromptText("Từ ngày");
        fromDate.setMinWidth(150);
        fromDate.setPrefWidth(155);

        DatePicker toDate = new DatePicker();
        toDate.setPromptText("Đến ngày");
        toDate.setMinWidth(150);
        toDate.setPrefWidth(155);

        Button searchBtn = new Button("🔍 Tìm kiếm");
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setMinWidth(115);

        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setMinWidth(110);

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID giảm dần", "ID tăng dần");
        sortOrder.setValue("ID giảm dần");

        HBox toolbar = new HBox(10, search, statusFilter, fromDate, toDate, searchBtn, refreshBtn, filterSpacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<Invoice> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Chưa có hóa đơn nào trong hệ thống."));

        TableColumn<Invoice, String> bookCodeCol = new TableColumn<>();
        bookCodeCol.setMinWidth(75);
        bookCodeCol.setPrefWidth(85);
        Label bookIdHeader = new Label("ID ▼");
        bookIdHeader.setStyle("-fx-font-weight: 800; -fx-cursor: hand; -fx-text-fill: #38BDF8; -fx-font-size: 13px;");
        bookCodeCol.setGraphic(bookIdHeader);
        bookCodeCol.setSortable(false);
        bookCodeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().booking != null && d.getValue().booking.id != null ? String.valueOf(d.getValue().booking.id) : "—"));
        bookCodeCol.setStyle("-fx-font-weight: 700; -fx-text-fill: #38BDF8; -fx-font-size: 13px;");

        bookIdHeader.setOnMouseClicked(e -> {
            if ("ID giảm dần".equals(sortOrder.getValue())) {
                sortOrder.setValue("ID tăng dần");
            } else {
                sortOrder.setValue("ID giảm dần");
            }
        });
        sortOrder.valueProperty().addListener((obs, oldV, newV) -> {
            bookIdHeader.setText("ID " + ("ID giảm dần".equals(newV) ? "▼" : "▲"));
            if (reloadPageRef[0] != null) reloadPageRef[0].run();
        });

        TableColumn<Invoice, String> nameCol = new TableColumn<>("Khách hàng");
        nameCol.setMinWidth(180);
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(d -> {
            String name = d.getValue().customer != null && d.getValue().customer.fullName != null
                    ? d.getValue().customer.fullName : "Khách vãng lai";
            String phone = d.getValue().customer != null && d.getValue().customer.phone != null && !d.getValue().customer.phone.isBlank()
                    ? " (" + d.getValue().customer.phone + ")" : "";
            return new SimpleStringProperty(name + phone);
        });
        nameCol.setStyle("-fx-font-weight: 600;");

        TableColumn<Invoice, String> roomCol = new TableColumn<>("Phòng");
        roomCol.setMinWidth(95);
        roomCol.setPrefWidth(105);
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().room != null && d.getValue().room.number != null ? "P" + d.getValue().room.number : "—"));
        roomCol.setStyle("-fx-font-weight: 700; -fx-text-fill: #FCD34D; -fx-alignment: CENTER_LEFT;");

        TableColumn<Invoice, String> roomFeeCol = new TableColumn<>("Tiền phòng");
        roomFeeCol.setMinWidth(125);
        roomFeeCol.setPrefWidth(135);
        roomFeeCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().roomFee)));
        roomFeeCol.setStyle("-fx-font-weight: 600;");

        TableColumn<Invoice, String> svcFeeCol = new TableColumn<>("Dịch vụ");
        svcFeeCol.setMinWidth(115);
        svcFeeCol.setPrefWidth(125);
        svcFeeCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().serviceFee)));
        svcFeeCol.setStyle("-fx-font-weight: 600;");

        TableColumn<Invoice, String> penaltyCol = new TableColumn<>("Phụ thu/Phạt");
        penaltyCol.setMinWidth(115);
        penaltyCol.setPrefWidth(125);
        penaltyCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().penaltyFee)));

        TableColumn<Invoice, String> discountCol = new TableColumn<>("Giảm giá");
        discountCol.setMinWidth(115);
        discountCol.setPrefWidth(125);
        discountCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().discountFee)));
        discountCol.setStyle("-fx-text-fill: #F87171;");

        TableColumn<Invoice, String> totalCol = new TableColumn<>("Tổng cộng");
        totalCol.setMinWidth(135);
        totalCol.setPrefWidth(145);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().total)));
        totalCol.setStyle("-fx-font-weight: 800; -fx-text-fill: #10B981; -fx-font-size: 14px;");

        TableColumn<Invoice, String> paidCol = new TableColumn<>("Đã thu");
        paidCol.setMinWidth(125);
        paidCol.setPrefWidth(135);
        paidCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().paidAmount)));
        paidCol.setStyle("-fx-font-weight: 700; -fx-text-fill: #38BDF8;");

        TableColumn<Invoice, String> remainingCol = new TableColumn<>("Còn lại");
        remainingCol.setMinWidth(125);
        remainingCol.setPrefWidth(135);
        remainingCol.setCellValueFactory(d -> new SimpleStringProperty(Util.money(d.getValue().remainingAmount)));
        remainingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    Invoice inv = getTableView().getItems().get(getIndex());
                    if (inv.remainingAmount != null && inv.remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 700;");
                    } else {
                        setStyle("-fx-text-fill: #94A3B8;");
                    }
                }
            }
        });

        TableColumn<Invoice, String> paymentCol = new TableColumn<>("Trạng thái");
        paymentCol.setMinWidth(185);
        paymentCol.setPrefWidth(195);
        paymentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().paymentStatusLabel()));
        paymentCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", inv.badgeClass());
                    badge.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 6;");
                    badge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                    badge.setWrapText(false);
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<Invoice, String> staffCol = new TableColumn<>("Nhân viên");
        staffCol.setMinWidth(160);
        staffCol.setPrefWidth(175);
        staffCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().staffName != null && !d.getValue().staffName.isBlank() ? d.getValue().staffName : "—"));
        staffCol.setStyle("-fx-font-weight: 700; -fx-text-fill: #A78BFA;");

        TableColumn<Invoice, String> createdCol = new TableColumn<>("Ngày tạo");
        createdCol.setMinWidth(170);
        createdCol.setPrefWidth(180);
        createdCol.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().createdAt)));

        TableColumn<Invoice, String> modifiedCol = new TableColumn<>("Ngày sửa");
        modifiedCol.setMinWidth(165);
        modifiedCol.setPrefWidth(175);
        modifiedCol.setCellValueFactory(d -> new SimpleStringProperty(formatDateTime(d.getValue().modifiedAt)));

        // Pagination Bar
        PaginationHelper.PaginationBar paginationBar = new PaginationHelper.PaginationBar("hóa đơn");

        // State phân trang
        int[] currentPage = new int[]{1};
        int[] currentPageSize = new int[]{DEFAULT_PAGE_SIZE};

        // Reload action holder
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<Integer>[] loadPageHolder = new java.util.function.Consumer[1];

        // Column Thao tác
        TableColumn<Invoice, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setMinWidth(275);
        actionCol.setPrefWidth(285);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Chi tiết");
            private final Button payBtn = new Button("Thu tiền");
            private final Button delBtn = new Button("Xóa");
            private final HBox box = new HBox(6, viewBtn, payBtn, delBtn);

            {
                viewBtn.getStyleClass().add("btn-outline");
                viewBtn.setGraphic(IconUtils.icon("print", 12, Color.web("#93C5FD")));
                viewBtn.setGraphicTextGap(5);
                viewBtn.setMinWidth(Region.USE_PREF_SIZE);
                viewBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");

                payBtn.getStyleClass().add("btn-gold");
                payBtn.setMinWidth(Region.USE_PREF_SIZE);
                payBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");

                delBtn.getStyleClass().add("btn-danger");
                delBtn.setGraphic(IconUtils.icon("trash", 12, Color.WHITE));
                delBtn.setMinWidth(Region.USE_PREF_SIZE);
                delBtn.setStyle("-fx-padding: 4 10; -fx-font-size: 12px; -fx-font-weight: 600; -fx-cursor: hand;");

                box.setAlignment(Pos.CENTER_LEFT);
                setAlignment(Pos.CENTER_LEFT);

                viewBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    openInvoiceDetailDialog(inv.id, () -> loadPageHolder[0].accept(currentPage[0]));
                });

                payBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    openPaymentDialog(inv, () -> loadPageHolder[0].accept(currentPage[0]));
                });

                delBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận xóa hóa đơn");
                    alert.setHeaderText("Xóa hóa đơn #" + inv.id + "?");
                    alert.setContentText("Hành động này sẽ xóa hóa đơn và tất cả giao dịch thanh toán liên quan. Không thể khôi phục.");
                    alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
                        Util.runAsyncVoid(() -> ApiService.deleteInvoice(inv.id),
                                () -> loadPageHolder[0].accept(currentPage[0]));
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    boolean needPay = inv.remainingAmount != null && inv.remainingAmount.compareTo(BigDecimal.ZERO) > 0;
                    payBtn.setVisible(needPay);
                    payBtn.setManaged(needPay);
                    delBtn.setVisible(Session.canDeleteInvoice());
                    delBtn.setManaged(Session.canDeleteInvoice());
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(bookCodeCol, nameCol, roomCol, roomFeeCol, svcFeeCol, penaltyCol, discountCol, totalCol, paidCol, remainingCol, paymentCol, staffCol, createdCol, modifiedCol, actionCol);

        // Footer Summary
        HBox summaryFooter = new HBox(24);
        summaryFooter.setAlignment(Pos.CENTER_RIGHT);
        summaryFooter.setPadding(new Insets(10, 16, 4, 16));
        summaryFooter.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 8; -fx-border-color: #1E293B; -fx-border-radius: 8;");

        Label countLabel = new Label("Tổng: 0 hóa đơn");
        countLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: 600;");

        Label totalPaidLabel = new Label("TỔNG ĐÃ THU: 0 ₫");
        totalPaidLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #10B981; -fx-font-size: 14px;");

        Label totalPendingLabel = new Label("CÔNG NỢ CẦN THU: 0 ₫");
        totalPendingLabel.setStyle("-fx-font-weight: 800; -fx-text-fill: #EF4444; -fx-font-size: 14px;");

        summaryFooter.getChildren().addAll(countLabel, totalPaidLabel, totalPendingLabel);

        // Load page function
        reloadPageRef[0] = () -> loadPageHolder[0].accept(1);
        loadPageHolder[0] = targetPage -> {
            currentPage[0] = Math.max(1, targetPage);
            String q = search.getText() == null ? "" : search.getText().trim();
            String st = statusFilter.getValue();
            LocalDate fDate = fromDate.getValue();
            LocalDate tDate = toDate.getValue();

            String sDir = "ID tăng dần".equals(sortOrder.getValue()) ? "asc" : "desc";
            Util.runAsync(() -> ApiService.invoicePage(currentPage[0], currentPageSize[0], q, st, fDate, tDate, sDir),
                    (PageResponse<Invoice> page) -> {
                        List<Invoice> list = page != null && page.getContent() != null ? page.getContent() : Collections.emptyList();
                        tableData.setAll(list);
                        table.scrollTo(0);

                        long totalElements = page != null ? page.totalElements : 0;
                        paginationBar.update(currentPage[0], currentPageSize[0], totalElements, newPage -> loadPageHolder[0].accept(newPage));

                        // Cập nhật summary footer
                        countLabel.setText("Tổng: " + totalElements + " hóa đơn");

                        BigDecimal sumPaid = list.stream()
                                .map(i -> i.paidAmount != null ? i.paidAmount : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        totalPaidLabel.setText("TỔNG ĐÃ THU TRANG: " + Util.money(sumPaid) + " ₫");

                        BigDecimal sumPending = list.stream()
                                .map(i -> i.remainingAmount != null ? i.remainingAmount : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        totalPendingLabel.setText("CÔNG NỢ TRANG: " + Util.money(sumPending) + " ₫");
                    });
        };

        // Gán sự kiện đổi pageSize
        paginationBar.getPageSizeCombo().setOnAction(e -> {
            Integer selectedSize = paginationBar.getPageSizeCombo().getValue();
            if (selectedSize != null && selectedSize != currentPageSize[0]) {
                currentPageSize[0] = selectedSize;
                loadPageHolder[0].accept(1);
            }
        });

        // Trigger search / filter
        Runnable executeSearch = () -> loadPageHolder[0].accept(1);
        searchBtn.setOnAction(e -> executeSearch.run());
        search.setOnAction(e -> executeSearch.run());
        statusFilter.setOnAction(e -> executeSearch.run());
        fromDate.setOnAction(e -> executeSearch.run());
        toDate.setOnAction(e -> executeSearch.run());

        refreshBtn.setOnAction(e -> {
            search.clear();
            statusFilter.setValue("Tất cả thanh toán");
            sortOrder.setValue("ID giảm dần");
            fromDate.setValue(null);
            toDate.setValue(null);
            executeSearch.run();
        });

        addBtn.setOnAction(e -> openCreateInvoiceDialog(() -> loadPageHolder[0].accept(currentPage[0])));

        exportBtn.setOnAction(e -> {
            exportCsv(tableData);
        });

        // First load
        loadPageHolder[0].accept(1);

        card.getChildren().addAll(headerRow, toolbar, table, paginationBar, summaryFooter);
        root.setCenter(card);
        return root;
    }

    // =========================================================================
    // MODAL 1: XEM CHI TIẾT & IN HÓA ĐƠN KHÁCH SẠN
    // =========================================================================
    private static void openInvoiceDetailDialog(Long invoiceId, Runnable onUpdate) {
        Util.runAsync(() -> ApiService.getInvoiceById(invoiceId), inv -> {
            if (inv == null) {
                Util.info("Không tìm thấy thông tin hóa đơn lưu trú.");
                return;
            }

            String cName = inv.customer != null && inv.customer.fullName != null ? inv.customer.fullName : "Khách vãng lai";
            String rNum = inv.room != null && inv.room.number != null ? "Phòng " + inv.room.number : "—";
            String rType = inv.room != null && inv.room.roomType != null && inv.room.roomType.name != null ? inv.room.roomType.name : "Tiêu chuẩn";

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Chi tiết hóa đơn lưu trú — " + rNum + " (" + cName + ")");
            dialog.getDialogPane().getStylesheets().add(
                    InvoicesView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
            dialog.getDialogPane().setStyle("-fx-background-color: #0A0D14;");

            VBox folio = new VBox(14);
            folio.setPadding(new Insets(20));
            folio.setPrefWidth(910);
            folio.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12; -fx-border-color: #212936; -fx-border-radius: 12;");

            // 1. Header Khách Sạn
            HBox hotelHeader = new HBox(16);
            hotelHeader.setAlignment(Pos.CENTER_LEFT);

            VBox hotelInfo = new VBox(4);
            Label hotelName = new Label("LOTUSSTAY HOTEL & SUITES");
            hotelName.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 19px; -fx-font-weight: 900; -fx-text-fill: #F59E0B;");
            hotelName.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            Label hotelSub = new Label("123 Luxury Boulevard, TP. Hồ Chí Minh · Hotline: 1900 6868 · contact@lotusstay.vn");
            hotelSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
            hotelInfo.getChildren().addAll(hotelName, hotelSub);

            Region hSpacer = new Region();
            HBox.setHgrow(hSpacer, Priority.ALWAYS);

            VBox invMeta = new VBox(4);
            invMeta.setAlignment(Pos.TOP_RIGHT);
            invMeta.setMinWidth(Region.USE_PREF_SIZE);

            HBox invCodeRow = new HBox(10);
            invCodeRow.setAlignment(Pos.CENTER_RIGHT);
            invCodeRow.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            Label invCodeLbl = new Label("HÓA ĐƠN THANH TOÁN");
            invCodeLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #FFFFFF;");
            invCodeLbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            invCodeLbl.setWrapText(false);

            Label statusBadge = new Label(inv.paymentStatusLabel().toUpperCase());
            if ("DA_THANH_TOAN".equalsIgnoreCase(inv.paymentStatus)) {
                statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34D399; -fx-border-color: #10B981; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-weight: 800; -fx-font-size: 11.5px;");
            } else if ("THANH_TOAN_MOT_PHAN".equalsIgnoreCase(inv.paymentStatus)) {
                statusBadge.setStyle("-fx-background-color: rgba(167, 139, 250, 0.2); -fx-text-fill: #C4B5FD; -fx-border-color: #8B5CF6; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-weight: 800; -fx-font-size: 11.5px;");
            } else {
                statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #F87171; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 4 12; -fx-font-weight: 800; -fx-font-size: 11.5px;");
            }
            statusBadge.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            invCodeRow.getChildren().addAll(invCodeLbl, statusBadge);

            String dateText = "Ngày tạo: " + formatDateTime(inv.createdAt != null && !inv.createdAt.isBlank() ? inv.createdAt : inv.issuedDate);
            if (inv.modifiedAt != null && !inv.modifiedAt.isBlank()) {
                dateText += " | Sửa: " + formatDateTime(inv.modifiedAt);
            }
            Label invDateLbl = new Label(dateText);
            invDateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
            invDateLbl.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            invMeta.getChildren().addAll(invCodeRow, invDateLbl);

            hotelHeader.getChildren().addAll(hotelInfo, hSpacer, invMeta);

            Separator sep1 = new Separator();

            // 2. Hai cột: Thông tin Khách & Thông tin Lưu trú
            HBox infoSection = new HBox(16);
            infoSection.setAlignment(Pos.TOP_LEFT);

            // Cột Trái: Khách hàng
            VBox customerCard = new VBox(10);
            customerCard.setStyle("-fx-background-color: #0F141F; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #242E42; -fx-border-radius: 10;");
            HBox.setHgrow(customerCard, Priority.ALWAYS);

            Label custTitle = new Label("👤  THÔNG TIN KHÁCH HÀNG");
            custTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #38BDF8; -fx-padding: 0 0 2 0;");
            String cPhone = inv.customer != null && inv.customer.phone != null ? inv.customer.phone : "Chưa có SĐT";
            String cId = inv.customer != null && inv.customer.citizenId != null ? inv.customer.citizenId : "—";

            customerCard.getChildren().addAll(
                    custTitle,
                    infoLine("Họ và tên khách hàng:", cName, "#38BDF8"),
                    infoLine("Số điện thoại:", cPhone, "#F8FAFC"),
                    infoLine("Số CCCD / Hộ chiếu:", cId, "#F8FAFC"),
                    infoLine("Nhân viên lập hóa đơn:", (inv.staffName != null && !inv.staffName.isBlank() ? inv.staffName : "—"), "#A78BFA")
            );

            // Cột Phải: Phòng & Lưu trú (ĐÃ BỎ HOÀN TOÀN ID ĐẶT PHÒNG)
            VBox stayCard = new VBox(10);
            stayCard.setStyle("-fx-background-color: #0F141F; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #242E42; -fx-border-radius: 10;");
            HBox.setHgrow(stayCard, Priority.ALWAYS);

            Label stayTitle = new Label("🏨  THÔNG TIN LƯU TRÚ");
            stayTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #FCD34D; -fx-padding: 0 0 2 0;");
            String inDate = inv.booking != null && inv.booking.checkIn != null ? inv.booking.checkIn : "—";
            String outDate = inv.booking != null && inv.booking.checkOutPlanned != null ? inv.booking.checkOutPlanned : "—";
            long nights = inv.stayNights != null ? inv.stayNights : 1;

            stayCard.getChildren().addAll(
                    stayTitle,
                    infoLine("Phòng lưu trú:", rNum, "#FCD34D", true),
                    infoLine("Hạng loại phòng:", rType, "#C084FC"),
                    infoLine("Thời gian lưu trú:", formatDateSafe(inDate) + "  →  " + formatDateSafe(outDate), "#F8FAFC"),
                    infoLine("Thời lượng lưu trú:", nights + " đêm lưu trú", "#38BDF8")
            );

            infoSection.getChildren().addAll(customerCard, stayCard);

            // 3. Bảng Chi Tiết Khoản Phí
            VBox costSection = new VBox(10);
            costSection.setStyle("-fx-background-color: #0F141F; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #242E42; -fx-border-radius: 10;");

            Label costTitle = new Label("🧾  CHI TIẾT THANH TOÁN (FOLIO BREAKDOWN)");
            costTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 900; -fx-text-fill: #E2E8F0;");
            costSection.getChildren().add(costTitle);

            // Row 1: Tiền phòng
            costSection.getChildren().add(chargeRow("🛏️ Tiền phòng lưu trú", Util.money(inv.roomFee), "#FCD34D"));

            // Row 2: Dịch vụ nếu có
            if (inv.services != null && !inv.services.isEmpty()) {
                Label svcHeader = new Label("🛎️ Dịch vụ phát sinh trong phòng:");
                svcHeader.setStyle("-fx-font-weight: 700; -fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-padding: 4 0 2 4;");
                costSection.getChildren().add(svcHeader);

                for (BookingServiceDetail s : inv.services) {
                    String sLine = "    • " + (s.serviceName != null ? s.serviceName : "Dịch vụ")
                            + " (SL: " + s.quantity + " x " + Util.money(s.unitPrice) + ")";
                    costSection.getChildren().add(chargeRow(sLine, Util.money(s.totalPrice), "#38BDF8"));
                }
            } else {
                costSection.getChildren().add(chargeRow("🛎️ Tiền dịch vụ khách sạn", Util.money(inv.serviceFee), "#38BDF8"));
            }

            // Row 3: Phụ thu/phạt
            if (inv.penaltyFee != null && inv.penaltyFee.compareTo(BigDecimal.ZERO) > 0) {
                costSection.getChildren().add(chargeRow("⚠️ Phụ thu / Phạt phát sinh", Util.money(inv.penaltyFee), "#FB923C"));
            }

            // Row 4: Giảm giá
            if (inv.discountFee != null && inv.discountFee.compareTo(BigDecimal.ZERO) > 0) {
                String promoNote = inv.promotionCode != null ? " (Voucher: " + inv.promotionCode + ")" : "";
                costSection.getChildren().add(chargeRow("🏷️ Giảm giá khuyến mãi" + promoNote, "– " + Util.money(inv.discountFee), "#F87171"));
            }

            Separator sep2 = new Separator();
            costSection.getChildren().add(sep2);

            // Summary Totals Box
            VBox summaryBox = new VBox(6);
            summaryBox.setPadding(new Insets(12, 16, 12, 16));
            summaryBox.setStyle("-fx-background-color: #0A0D14; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");

            String remText = (inv.remainingAmount != null && inv.remainingAmount.compareTo(BigDecimal.ZERO) == 0)
                    ? "0 ₫  (Đã quyết toán xong)"
                    : Util.money(inv.remainingAmount);
            String remColor = (inv.remainingAmount != null && inv.remainingAmount.compareTo(BigDecimal.ZERO) > 0) ? "#EF4444" : "#10B981";

            summaryBox.getChildren().addAll(
                    chargeRowBold("TỔNG TIỀN PHẢI TRẢ:", Util.money(inv.total), "#F59E0B", 16),
                    chargeRowBold("Đã thanh toán:", Util.money(inv.paidAmount), "#38BDF8", 14),
                    chargeRowBold("Còn lại cần thu:", remText, remColor, 15)
            );
            costSection.getChildren().add(summaryBox);

            // 4. Lịch sử giao dịch thanh toán
            VBox payHistoryBox = new VBox(8);
            payHistoryBox.setStyle("-fx-background-color: #0F141F; -fx-padding: 16; -fx-background-radius: 10; -fx-border-color: #242E42; -fx-border-radius: 10;");
            Label payHistTitle = new Label("💳  LỊCH SỬ GIAO DỊCH THANH TOÁN");
            payHistTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #A78BFA;");
            payHistoryBox.getChildren().add(payHistTitle);

            if (inv.payments != null && !inv.payments.isEmpty()) {
                for (PaymentTransaction pt : inv.payments) {
                    HBox pRow = new HBox(12);
                    pRow.setAlignment(Pos.CENTER_LEFT);
                    pRow.setPadding(new Insets(6, 12, 6, 12));
                    pRow.setStyle("-fx-background-color: #141A23; -fx-border-color: #242E42; -fx-border-radius: 6; -fx-background-radius: 6;");

                    Label pTime = new Label(formatDateTime(pt.paymentTime != null ? pt.paymentTime : "—"));
                    pTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-min-width: 140px;");
                    pTime.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

                    Label pMethod = new Label(pt.paymentMethodName != null ? pt.paymentMethodName : "Tiền mặt");
                    pMethod.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #C4B5FD; -fx-background-color: rgba(167, 139, 250, 0.15); -fx-padding: 3 8; -fx-background-radius: 4; -fx-min-width: 100px;");
                    pMethod.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

                    Label pCode = new Label(pt.transactionCode != null && !pt.transactionCode.isBlank() ? "Mã GD: " + pt.transactionCode : "");
                    pCode.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                    HBox.setHgrow(pCode, Priority.ALWAYS);

                    Label pAmt = new Label("+ " + Util.money(pt.amount));
                    pAmt.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #34D399;");
                    pAmt.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

                    pRow.getChildren().addAll(pTime, pMethod, pCode, pAmt);
                    payHistoryBox.getChildren().add(pRow);
                }
            } else {
                Label noPay = new Label("Chưa có giao dịch thanh toán nào được ghi nhận.");
                noPay.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-style: italic;");
                payHistoryBox.getChildren().add(noPay);
            }

            folio.getChildren().addAll(hotelHeader, sep1, infoSection, costSection, payHistoryBox);

            Button closeXBtn = new Button("✕");
            closeXBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: 800; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 3 10;");
            closeXBtn.setOnMouseEntered(e -> closeXBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 800; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 3 10;"));
            closeXBtn.setOnMouseExited(e -> closeXBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: 800; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 3 10;"));
            closeXBtn.setOnAction(e -> {
                dialog.setResult(null);
                dialog.close();
            });

            HBox topBar = new HBox(12);
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setPadding(new Insets(4, 10, 8, 10));
            Label dlgTitle = new Label("CHI TIẾT HÓA ĐƠN & QUYẾT TOÁN LƯU TRÚ");
            dlgTitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14.5px; -fx-font-weight: 900; -fx-text-fill: #F59E0B;");
            dlgTitle.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dlgTitle.setWrapText(false);
            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);
            topBar.getChildren().addAll(dlgTitle, topSpacer, closeXBtn);

            ScrollPane scrollPane = new ScrollPane(folio);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

            double screenH = Screen.getPrimary().getVisualBounds().getHeight();
            double maxH = Math.max(400, Math.min(620, screenH - 160));
            scrollPane.setPrefHeight(maxH);
            scrollPane.setMaxHeight(maxH);
            scrollPane.setPrefWidth(900);

            VBox dialogRoot = new VBox(0, topBar, scrollPane);
            dialogRoot.setStyle("-fx-background-color: #0A0D14;");

            dialog.getDialogPane().setContent(dialogRoot);
            ButtonType printBtnType = new ButtonType("🖨️ In hóa đơn", ButtonBar.ButtonData.LEFT);
            ButtonType payNowBtnType = new ButtonType("💳 Thu tiền ngay", ButtonBar.ButtonData.LEFT);
            ButtonType closeBtnType = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);

            boolean canPay = inv.remainingAmount != null && inv.remainingAmount.compareTo(BigDecimal.ZERO) > 0;
            if (canPay) {
                dialog.getDialogPane().getButtonTypes().addAll(printBtnType, payNowBtnType, closeBtnType);
            } else {
                dialog.getDialogPane().getButtonTypes().addAll(printBtnType, closeBtnType);
            }

            dialog.setResultConverter(bt -> {
                if (bt == printBtnType) {
                    Util.info("Lệnh in hóa đơn thanh toán đã được gửi tới máy in thành công!");
                } else if (bt == payNowBtnType) {
                    openPaymentDialog(inv, () -> {
                        if (onUpdate != null) onUpdate.run();
                        openInvoiceDetailDialog(invoiceId, onUpdate);
                    });
                }
                return null;
            });

            dialog.showAndWait();
        });
    }

    // =========================================================================
    // MODAL 2: THU TIỀN / THANH TOÁN HÓA ĐƠN
    // =========================================================================
    private static void openPaymentDialog(Invoice inv, Runnable onUpdate) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ghi nhận thanh toán lưu trú");
        dialog.getDialogPane().getStylesheets().add(
                InvoicesView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #0A0D14;");

        VBox form = new VBox(12);
        form.setPadding(new Insets(18));
        form.setPrefWidth(460);

        Label header = new Label("Thanh toán lưu trú & dịch vụ");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");
        header.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        header.setWrapText(false);

        Label custLbl = new Label("Khách hàng: " + (inv.customer != null && inv.customer.fullName != null ? inv.customer.fullName : "Khách vãng lai")
                + " — Phòng " + (inv.room != null ? inv.room.number : "—"));
        custLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #E2E8F0;");

        Label balanceLbl = new Label("Số tiền còn phải thanh toán: " + Util.money(inv.remainingAmount) + " ₫");
        balanceLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #EF4444; -fx-padding: 0 0 6 0;");

        ComboBox<PaymentMethodItem> methodBox = new ComboBox<>();
        methodBox.setPromptText("— Chọn phương thức thanh toán —");
        methodBox.setMaxWidth(Double.MAX_VALUE);
        Util.runAsync(ApiService::paymentMethods, list -> {
            methodBox.getItems().setAll(list);
            if (!list.isEmpty()) methodBox.setValue(list.get(0));
        });

        TextField amountField = new TextField(inv.remainingAmount != null ? inv.remainingAmount.toPlainString() : "0");
        TextField transCodeField = new TextField();
        transCodeField.setPromptText("Mã giao dịch / số tham chiếu (nếu có)");

        form.getChildren().addAll(
                header, custLbl, balanceLbl,
                labeledBox("Phương thức thanh toán (*)", methodBox),
                labeledBox("Số tiền thanh toán (VNĐ) (*)", amountField),
                labeledBox("Mã giao dịch ngân hàng / POS", transCodeField)
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                PaymentMethodItem pm = methodBox.getValue();
                if (pm == null) {
                    Util.info("Vui lòng chọn phương thức thanh toán.");
                    return null;
                }
                BigDecimal amt = parse(amountField.getText());
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    Util.info("Số tiền thanh toán phải lớn hơn 0.");
                    return null;
                }
                String tCode = transCodeField.getText() != null ? transCodeField.getText().trim() : "";

                Util.runAsync(() -> ApiService.payInvoice(inv.id, pm.id, amt, tCode), res -> {
                    Util.info("Ghi nhận thanh toán " + Util.money(amt) + " ₫ thành công!");
                    if (onUpdate != null) onUpdate.run();
                });
            }
            return null;
        });

        dialog.showAndWait();
    }

    // =========================================================================
    // MODAL 3: TẠO HÓA ĐƠN MỚI
    // =========================================================================
    private static void openCreateInvoiceDialog(Runnable onUpdate) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lập hóa đơn thanh toán mới");
        dialog.getDialogPane().getStylesheets().add(
                InvoicesView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
        dialog.getDialogPane().setStyle("-fx-background-color: #0A0D14;");

        VBox form = new VBox(12);
        form.setPadding(new Insets(18));
        form.setPrefWidth(560);

        Label title = new Label("Tạo hóa đơn & Quyết toán lưu trú");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");
        title.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        title.setWrapText(false);

        ComboBox<Booking> bookingBox = new ComboBox<>();
        bookingBox.setPromptText("— Chọn đơn đặt phòng để lập hóa đơn —");
        bookingBox.setMaxWidth(Double.MAX_VALUE);

        TextField roomFee = new TextField("0");
        TextField serviceFee = new TextField("0");
        TextField penaltyFee = new TextField("0");
        DatePicker date = new DatePicker(LocalDate.now());
        date.setDisable(true);
        date.setStyle("-fx-opacity: 0.9;");

        ComboBox<Promotion> promoBox = new ComboBox<>();
        promoBox.setPromptText("— Chọn mã khuyến mãi (nếu có) —");
        promoBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<PaymentMethodItem> methodBox = new ComboBox<>();
        methodBox.setPromptText("— Chọn phương thức thanh toán ngay —");
        methodBox.setMaxWidth(Double.MAX_VALUE);

        CheckBox autoCheckoutCheck = new CheckBox("Đồng thời thực hiện Trả phòng (chuyển trạng thái phòng sang Đang dọn)");
        autoCheckoutCheck.setSelected(true);
        autoCheckoutCheck.setStyle("-fx-text-fill: #E2E8F0; -fx-font-weight: 600; -fx-padding: 4 0;");

        // Load Bookings (ưu tiên phòng ĐANG_O và DAT_TRUOC)
        Util.runAsync(ApiService::bookings, list -> {
            bookingBox.getItems().setAll(list);
        });

        // Load Promotions
        Util.runAsync(ApiService::promotions, promoBox.getItems()::setAll);

        // Load Payment Methods
        Util.runAsync(ApiService::paymentMethods, list -> {
            methodBox.getItems().setAll(list);
            if (!list.isEmpty()) methodBox.setValue(list.get(0));
        });

        // Khi chọn một đơn đặt phòng: tự động tính tiền phòng và tiền dịch vụ
        bookingBox.setOnAction(e -> {
            Booking b = bookingBox.getValue();
            if (b != null) {
                // Tiền phòng cơ bản
                BigDecimal basePrice = (b.room != null && b.room.roomType != null && b.room.roomType.basePrice != null)
                        ? b.room.roomType.basePrice : BigDecimal.valueOf(500000);
                roomFee.setText(basePrice.toPlainString());

                // Tính tiền dịch vụ từ chi tiết dịch vụ phòng
                if (b.id != null) {
                    Util.runAsync(() -> ApiService.bookingServiceDetailsByBooking(b.id), details -> {
                        BigDecimal svTotal = details.stream()
                                .map(d -> d.totalPrice != null ? d.totalPrice : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        serviceFee.setText(svTotal.toPlainString());
                    });
                }
            }
        });

        form.getChildren().addAll(
                title,
                labeledBox("Đơn đặt phòng (*)", bookingBox),
                new HBox(12, labeledBox("Tiền phòng (VNĐ)", roomFee), labeledBox("Tiền dịch vụ (VNĐ)", serviceFee)),
                new HBox(12, labeledBox("Phụ thu / Phạt phát sinh (VNĐ)", penaltyFee), labeledBox("Ngày tạo (Tự động ghi nhận)", date)),
                labeledBox("Khuyến mãi áp dụng", promoBox),
                labeledBox("Phương thức thanh toán ngay", methodBox),
                autoCheckoutCheck
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Booking b = bookingBox.getValue();
                if (b == null || b.id == null) {
                    Util.info("Vui lòng chọn đơn đặt phòng.");
                    return null;
                }

                BigDecimal rFee = parse(roomFee.getText());
                BigDecimal sFee = parse(serviceFee.getText());
                BigDecimal pFee = parse(penaltyFee.getText());
                Promotion promo = promoBox.getValue();
                Long promoId = promo != null ? promo.id : null;
                String promoCode = promo != null ? promo.code : null;

                PaymentMethodItem pm = methodBox.getValue();
                Long pmId = pm != null ? pm.id : null;

                String dateStr = date.getValue() != null ? date.getValue().toString() : LocalDate.now().toString();
                boolean autoCo = autoCheckoutCheck.isSelected();

                Util.runAsync(() -> ApiService.createInvoice(
                        b.id, rFee, sFee, pFee, null, promoId, promoCode,
                        pmId, null, null, dateStr, autoCo
                ), res -> {
                    
                    if (onUpdate != null) onUpdate.run();
                });
            }
            return null;
        });

        dialog.showAndWait();
    }

    // =========================================================================
    // EXPORT CSV
    // =========================================================================
    private static void exportCsv(List<Invoice> list) {
        if (list == null || list.isEmpty()) {
            Util.info("Không có dữ liệu hóa đơn để xuất.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file danh sách hóa đơn");
        fileChooser.setInitialFileName("DanhSachHoaDon_LotusStay_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                // BOM UTF-8 for Excel
                writer.write('\ufeff');
                writer.println("ID đặt phòng,Khách hàng,Số điện thoại,Số phòng,Tiền phòng,Tiền dịch vụ,Phụ thu,Giảm giá,Tổng cộng,Đã thu,Còn lại,Trạng thái,Nhân viên,Ngày tạo,Ngày sửa");
                for (Invoice inv : list) {
                    String bCode = inv.booking != null && inv.booking.id != null ? String.valueOf(inv.booking.id) : "";
                    String cName = inv.customer != null && inv.customer.fullName != null ? escapeCsv(inv.customer.fullName) : "Khách vãng lai";
                    String cPhone = inv.customer != null && inv.customer.phone != null ? inv.customer.phone : "";
                    String room = inv.room != null && inv.room.number != null ? "P" + inv.room.number : "";
                    String rFee = inv.roomFee != null ? inv.roomFee.toPlainString() : "0";
                    String sFee = inv.serviceFee != null ? inv.serviceFee.toPlainString() : "0";
                    String penFee = inv.penaltyFee != null ? inv.penaltyFee.toPlainString() : "0";
                    String discFee = inv.discountFee != null ? inv.discountFee.toPlainString() : "0";
                    String total = inv.total != null ? inv.total.toPlainString() : "0";
                    String paid = inv.paidAmount != null ? inv.paidAmount.toPlainString() : "0";
                    String rem = inv.remainingAmount != null ? inv.remainingAmount.toPlainString() : "0";
                    String status = inv.paymentStatusLabel();
                    String staffName = inv.staffName != null && !inv.staffName.isBlank() ? escapeCsv(inv.staffName) : "";
                    String created = formatDateTime(inv.createdAt != null && !inv.createdAt.isBlank() ? inv.createdAt : inv.issuedDate);
                    String modified = formatDateTime(inv.modifiedAt);

                    writer.println(String.join(",",
                            bCode, cName, cPhone, room, rFee, sFee, penFee, discFee, total, paid, rem, status, staffName, created, modified));
                }
                Util.info("Xuất file CSV thành công tại: " + file.getAbsolutePath());
            } catch (Exception ex) {
                Util.info("Lỗi khi xuất file: " + ex.getMessage());
            }
        }
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static HBox infoLine(String label, String value) {
        return infoLine(label, value, "#FFFFFF", false);
    }

    private static HBox infoLine(String label, String value, String valColor) {
        return infoLine(label, value, valColor, false);
    }

    private static HBox infoLine(String label, String value, String valColor, boolean highlight) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 0, 2, 0));

        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #94A3B8; -fx-font-weight: 600;");
        l.setMinWidth(180);
        l.setPrefWidth(180);
        l.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        l.setWrapText(false);

        Label v = new Label(value);
        if (highlight) {
            v.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: " + valColor + "; -fx-background-color: rgba(252, 211, 77, 0.12); -fx-padding: 3 10; -fx-background-radius: 6; -fx-border-color: rgba(252, 211, 77, 0.35); -fx-border-radius: 6;");
        } else {
            v.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + valColor + ";");
        }
        v.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        v.setWrapText(false);

        box.getChildren().addAll(l, v);
        return box;
    }

    private static String formatDateSafe(String isoOrSlash) {
        if (isoOrSlash == null || isoOrSlash.isBlank() || "—".equals(isoOrSlash)) return "—";
        try {
            if (isoOrSlash.contains("-")) {
                String[] p = isoOrSlash.trim().split("T")[0].split("-");
                if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0];
            }
        } catch (Exception ignored) {}
        return isoOrSlash;
    }

    private static HBox chargeRow(String name, String amount) {
        return chargeRow(name, amount, "#FFFFFF");
    }

    private static HBox chargeRow(String name, String amount, String amtColor) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 12, 6, 12));
        row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 6;");

        Label n = new Label(name);
        n.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #E2E8F0;");

        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);

        Label a = new Label(amount);
        a.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: " + amtColor + ";");

        row.getChildren().addAll(n, s, a);
        return row;
    }

    private static HBox chargeRowBold(String name, String amount, String color, int fontSize) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));

        Label n = new Label(name);
        n.setStyle("-fx-font-size: " + (fontSize - 1.5) + "px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");

        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);

        Label a = new Label(amount);
        a.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");

        row.getChildren().addAll(n, s, a);
        return row;
    }

    private static BigDecimal parse(String s) {
        try {
            return new BigDecimal(s.trim().isEmpty() ? "0" : s.trim().replaceAll("[,.]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static VBox labeledBox(String label, Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0; -fx-padding: 0 0 3 0;");
        l.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        l.setWrapText(false);
        VBox box = new VBox(4, l, field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static String formatDateTime(String raw) {
        if (raw == null || raw.isBlank() || "—".equals(raw)) return "—";
        try {
            int dot = raw.indexOf('.');
            String clean = dot > 0 ? raw.substring(0, dot) : raw;
            LocalDateTime ldt = LocalDateTime.parse(clean);
            return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e1) {
            try {
                LocalDate ld = LocalDate.parse(raw);
                return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}
        }
        return raw;
    }
}

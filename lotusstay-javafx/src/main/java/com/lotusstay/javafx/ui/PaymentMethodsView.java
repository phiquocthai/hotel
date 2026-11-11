package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.PaymentMethodItem;
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

import java.util.List;

public class PaymentMethodsView {

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

        Label title = new Label("Danh mục phương thức thanh toán");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(title, topSpacer);

        // Data collection
        ObservableList<PaymentMethodItem> tableData = FXCollections.observableArrayList();

        // Toolbar
        TextField search = new TextField();
        search.setPromptText("🔍 Tìm theo ID, tên phương thức, ngân hàng, STK...");
        search.setPrefWidth(330);
        search.setMinWidth(260);
        search.setPrefHeight(38);

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả trạng thái", "Đang sử dụng", "Tạm dừng");
        statusFilter.setValue("Tất cả trạng thái");
        statusFilter.setMinWidth(180);
        statusFilter.setPrefWidth(180);
        statusFilter.setPrefHeight(38);

        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setGraphic(IconUtils.icon("search", 13, Color.web("#94A3B8")));
        searchBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setGraphic(IconUtils.icon("refresh", 13, Color.web("#94A3B8")));
        refreshBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button addBtn = new Button("Thêm phương thức");
        addBtn.getStyleClass().add("btn-gold");
        addBtn.setGraphic(IconUtils.icon("plus", 14, Color.web("#0A0D14")));
        addBtn.setMinWidth(Region.USE_PREF_SIZE);
        addBtn.setPrefWidth(185);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID tăng dần", "ID giảm dần");
        sortOrder.setValue("ID tăng dần");

        HBox toolbar = new HBox(10, search, statusFilter, searchBtn, refreshBtn, spacer, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<PaymentMethodItem> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setItems(tableData);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Chưa có phương thức thanh toán nào trong hệ thống."));

        // Pagination Bar
        int[] currentPage = new int[]{1};
        int[] currentPageSize = new int[]{PaginationHelper.DEFAULT_PAGE_SIZE};
        PaginationHelper.PaginationBar paginationBar = new PaginationHelper.PaginationBar("phương thức thanh toán");

        final Runnable[] loadPageRef = new Runnable[1];
        reloadPageRef[0] = () -> { currentPage[0] = 1; loadPageRef[0].run(); };
        loadPageRef[0] = () -> {
            String q = search.getText() == null ? "" : search.getText().trim();
            String stVal = statusFilter.getValue();
            Boolean active = null;
            if ("Đang sử dụng".equals(stVal)) {
                active = true;
            } else if ("Tạm dừng".equals(stVal)) {
                active = false;
            }

            final Boolean fActive = active;
            String sDir = "ID giảm dần".equals(sortOrder.getValue()) ? "desc" : "asc";
            Util.runAsync(() -> ApiService.paymentMethodsPage(currentPage[0], currentPageSize[0], q, fActive, sDir), page -> {
                tableData.setAll(page.getContent() != null ? page.getContent() : List.of());
                table.scrollTo(0);

                int totalPages = Math.max(1, page.totalPages);
                long totalElements = page.totalElements;
                currentPage[0] = page.pageNo;

                paginationBar.update(currentPage[0], currentPageSize[0], totalElements, newPage -> {
                    currentPage[0] = newPage;
                    loadPageRef[0].run();
                });
            });
        };
        Runnable reload = () -> loadPageRef[0].run();
        Runnable doSearch = () -> {
            currentPage[0] = 1;
            reload.run();
        };

        // Bắt buộc ấn nút Tìm kiếm hoặc Enter mới truy vấn xuống backend
        searchBtn.setOnAction(e -> doSearch.run());
        search.setOnAction(e -> doSearch.run());

        paginationBar.getPageSizeCombo().setOnAction(e -> {
            Integer selectedSize = paginationBar.getPageSizeCombo().getValue();
            if (selectedSize != null && selectedSize != currentPageSize[0]) {
                currentPageSize[0] = selectedSize;
                currentPage[0] = 1;
                reload.run();
            }
        });

        refreshBtn.setOnAction(e -> {
            search.clear();
            statusFilter.setValue("Tất cả trạng thái");
            doSearch.run();
        });

        TableColumn<PaymentMethodItem, String> idCol = new TableColumn<>();
        idCol.setMinWidth(75);
        idCol.setPrefWidth(85);
        Label idHeader = new Label("ID ▲");
        idHeader.setStyle("-fx-font-weight: 800; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
        idCol.setGraphic(idHeader);
        idCol.setSortable(false);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().id)));
        idCol.setStyle("-fx-font-weight: 800; -fx-alignment: CENTER_LEFT; -fx-font-size: 13.5px; -fx-text-fill: #FFFFFF;");

        idHeader.setOnMouseClicked(e -> {
            if ("ID tăng dần".equals(sortOrder.getValue())) {
                sortOrder.setValue("ID giảm dần");
            } else {
                sortOrder.setValue("ID tăng dần");
            }
        });
        sortOrder.valueProperty().addListener((obs, oldV, newV) -> {
            idHeader.setText("ID " + ("ID giảm dần".equals(newV) ? "▼" : "▲"));
            if (reloadPageRef[0] != null) reloadPageRef[0].run();
        });

        TableColumn<PaymentMethodItem, String> nameCol = new TableColumn<>("Tên phương thức");
        nameCol.setMinWidth(220);
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        nameCol.setStyle("-fx-font-weight: 800; -fx-font-size: 13.5px;");

        TableColumn<PaymentMethodItem, String> bankCol = new TableColumn<>("Ngân hàng liên kết");
        bankCol.setMinWidth(170);
        bankCol.setPrefWidth(185);
        bankCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().bankName != null && !d.getValue().bankName.isBlank() ? d.getValue().bankName : "—"));
        bankCol.setStyle("-fx-font-weight: 700; -fx-text-fill: #38BDF8; -fx-font-size: 13.5px;");

        TableColumn<PaymentMethodItem, String> accNumCol = new TableColumn<>("Số tài khoản");
        accNumCol.setMinWidth(170);
        accNumCol.setPrefWidth(185);
        accNumCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().accountNumber != null && !d.getValue().accountNumber.isBlank() ? d.getValue().accountNumber : "—"));
        accNumCol.setStyle("-fx-font-weight: 800; -fx-text-fill: #F59E0B; -fx-font-size: 13.5px;");

        TableColumn<PaymentMethodItem, String> holderCol = new TableColumn<>("Chủ tài khoản");
        holderCol.setMinWidth(200);
        holderCol.setPrefWidth(220);
        holderCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().accountHolderName != null && !d.getValue().accountHolderName.isBlank() ? d.getValue().accountHolderName : "—"));
        holderCol.setStyle("-fx-font-weight: 600; -fx-text-fill: #CBD5E1; -fx-font-size: 13px;");

        TableColumn<PaymentMethodItem, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setMinWidth(135);
        statusCol.setPrefWidth(145);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().statusDisplay()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    PaymentMethodItem pm = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", pm.badgeClass());
                    badge.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 4 12;");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<PaymentMethodItem, Void> actionCol = new TableColumn<>("Thao tác");
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
                    PaymentMethodItem pm = getTableView().getItems().get(getIndex());
                    openPaymentMethodDialog(pm, reload);
                });

                delBtn.setOnAction(e -> {
                    PaymentMethodItem pm = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Xác nhận xóa phương thức");
                    alert.setHeaderText("Xóa phương thức thanh toán " + pm.name + "?");
                    alert.setContentText("Hành động này sẽ ngưng sử dụng phương thức thanh toán này.");
                    alert.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
                        Util.runAsyncVoid(() -> ApiService.deletePaymentMethod(pm.id), reload);
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(idCol, nameCol, bankCol, accNumCol, holderCol, statusCol, actionCol);

        // Reload logic
        reload.run();

        addBtn.setOnAction(e -> openPaymentMethodDialog(null, reload));

        card.getChildren().addAll(headerRow, toolbar, table, paginationBar);
        root.setCenter(card);
        return root;
    }

    private static void openPaymentMethodDialog(PaymentMethodItem existing, Runnable reload) {
        boolean isEdit = existing != null;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Cập nhật phương thức thanh toán" : "Thêm mới phương thức thanh toán");
        dialog.getDialogPane().getStylesheets().add(
                PaymentMethodsView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());

        TextField name = new TextField(isEdit ? existing.name : "");
        name.setPromptText("Tên phương thức (VD: Chuyển khoản Vietcombank, Tiền mặt)");

        TextField bankName = new TextField(isEdit && existing.bankName != null ? existing.bankName : "");
        bankName.setPromptText("Tên ngân hàng (VD: Vietcombank, MB Bank, Techcombank)");

        TextField accNum = new TextField(isEdit && existing.accountNumber != null ? existing.accountNumber : "");
        accNum.setPromptText("Số tài khoản nhận tiền (VD: 1029384756)");

        TextField holder = new TextField(isEdit && existing.accountHolderName != null ? existing.accountHolderName : "");
        holder.setPromptText("Tên chủ tài khoản (VD: KHACH SAN LOTUSSTAY)");

        TextField qr = new TextField(isEdit && existing.qrImage != null ? existing.qrImage : "");
        qr.setPromptText("Đường dẫn ảnh mã QR (tùy chọn)");

        CheckBox activeCb = new CheckBox("Kích hoạt sử dụng phương thức này");
        activeCb.setSelected(!isEdit || (existing.active != null && existing.active));
        activeCb.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 600;");

        VBox form = new VBox(10,
                labeledBox("Tên phương thức thanh toán (*)", name),
                new HBox(12,
                        wrapFlex(labeledBox("Ngân hàng liên kết", bankName)),
                        wrapFlex(labeledBox("Số tài khoản", accNum))
                ),
                labeledBox("Tên chủ tài khoản thụ hưởng", holder),
                labeledBox("Ảnh mã QR / Ghi chú thanh toán", qr),
                activeCb
        );
        form.setPadding(new Insets(16));
        form.setPrefWidth(460);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String nameVal = name.getText().trim();
                if (nameVal.isBlank()) {
                    Util.error("Tên phương thức thanh toán không được để trống.");
                    return null;
                }
                String bankVal = bankName.getText().trim();
                String accVal = accNum.getText().trim();
                String holderVal = holder.getText().trim().toUpperCase();
                String qrVal = qr.getText().trim();
                boolean act = activeCb.isSelected();

                if (isEdit) {
                    Util.runAsync(() -> ApiService.updatePaymentMethod(existing.id, nameVal, bankVal, accVal, holderVal, qrVal, act),
                            pm -> reload.run());
                } else {
                    Util.runAsync(() -> ApiService.createPaymentMethod(nameVal, bankVal, accVal, holderVal, qrVal, act),
                            pm -> reload.run());
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

    private static VBox wrapFlex(VBox box) {
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }
}

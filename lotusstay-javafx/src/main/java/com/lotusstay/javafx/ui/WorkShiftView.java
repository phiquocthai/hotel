package com.lotusstay.javafx.ui;

import com.lotusstay.javafx.model.WorkShift;
import com.lotusstay.javafx.net.ApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.List;

/**
 * Giao diện Ca làm việc — Chuẩn Dark Luxury Theme (Nền đen - than chì, chữ
 * trắng sáng, điểm nhấn vàng kim):
 * Montserrat (Tiêu đề, Heading) & Inter (Bảng biểu, Dữ liệu).
 * Nút Sửa / Xóa tinh tế, chuẩn nét tương phản cao.
 */
public class WorkShiftView {

    public static BorderPane build() {
        final Runnable[] reloadPageRef = new Runnable[1];
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(20);
        card.getStyleClass().add("panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // ── Header row ────────────────────────────────────────────────────
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Ca làm việc");
        title.getStyleClass().add("page-heading");

        Label sub = new Label("Quản lý danh mục các ca trực định kỳ, quy định giờ bắt đầu và kết thúc trong khách sạn");
        sub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button addBtn = new Button("Thêm ca mới");
        addBtn.setGraphic(IconUtils.icon("plus", 14, Color.web("#0A0D14")));
        addBtn.setGraphicTextGap(8);
        addBtn.getStyleClass().add("btn-gold");
        addBtn.setMinWidth(Region.USE_PREF_SIZE);
        addBtn.setPrefWidth(160);
        addBtn.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 800; -fx-padding: 9 22; -fx-background-color: #F59E0B; -fx-text-fill: #0A0D14; -fx-background-radius: 8; -fx-cursor: hand;");

        headerRow.getChildren().addAll(titleBox, headerSpacer, addBtn);

        // ── Toolbar: Search & Refresh ──────────────────────────────────────
        TextField search = new TextField();
        search.setPromptText("🔍 Tìm theo tên ca làm việc...");
        search.setPrefWidth(280);
        search.setMinWidth(220);
        search.setPrefHeight(38);

        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.getStyleClass().add("btn-outline");
        searchBtn.setGraphic(IconUtils.icon("search", 13, Color.web("#94A3B8")));
        searchBtn.setMinWidth(Region.USE_PREF_SIZE);

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setGraphic(IconUtils.icon("refresh", 13, Color.web("#94A3B8")));
        refreshBtn.setMinWidth(Region.USE_PREF_SIZE);

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        ComboBox<String> sortOrder = new ComboBox<>();
        sortOrder.getItems().addAll("ID tăng dần", "ID giảm dần");
        sortOrder.setValue("ID tăng dần");

        HBox toolbar = new HBox(8, search, searchBtn, refreshBtn, toolbarSpacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // ── Table ─────────────────────────────────────────────────────────
        ObservableList<WorkShift> data = FXCollections.observableArrayList();

        TableView<WorkShift> table = new TableView<>(data);
        table.getStyleClass().add("table-view");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Chưa có ca làm việc nào."));

        TableColumn<WorkShift, String> idCol = new TableColumn<>();
        idCol.setMinWidth(70);
        idCol.setPrefWidth(80);
        Label idHeader = new Label("ID ▲");
        idHeader.setStyle("-fx-font-weight: 800; -fx-cursor: hand; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");
        idCol.setGraphic(idHeader);
        idCol.setSortable(false);
        idCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().id != null ? String.valueOf(d.getValue().id) : "—"));
        idCol.setStyle("-fx-font-weight: 800; -fx-alignment: CENTER_LEFT;");

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
        idCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-font-family: 'Inter', sans-serif; -fx-font-weight: 800; -fx-text-fill: #F59E0B; -fx-font-size: 13.5px; -fx-alignment: CENTER_LEFT;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<WorkShift, String> nameCol = new TableColumn<>("Tên ca");
        nameCol.setMinWidth(200);
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().shiftName));
        nameCol.setStyle("-fx-font-weight: 700; -fx-alignment: CENTER_LEFT;");
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-font-family: 'Inter', sans-serif; -fx-font-weight: 800; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-alignment: CENTER_LEFT;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<WorkShift, String> startCol = new TableColumn<>("Giờ bắt đầu");
        startCol.setMinWidth(150);
        startCol.setPrefWidth(180);
        startCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().startTime));
        startCol.setStyle("-fx-alignment: CENTER_LEFT;");
        startCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 14px; -fx-text-fill: #F1F5F9; -fx-font-weight: 600; -fx-alignment: CENTER_LEFT;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<WorkShift, String> endCol = new TableColumn<>("Giờ kết thúc");
        endCol.setMinWidth(150);
        endCol.setPrefWidth(180);
        endCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().endTime));
        endCol.setStyle("-fx-alignment: CENTER_LEFT;");
        endCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 14px; -fx-text-fill: #F1F5F9; -fx-font-weight: 600; -fx-alignment: CENTER_LEFT;");
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        // Cột Trạng thái (Entity field: dang_hoat_dong / active)
        TableColumn<WorkShift, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setMinWidth(180);
        statusCol.setPrefWidth(200);
        statusCol.setStyle("-fx-alignment: CENTER_LEFT;");
        statusCol
                .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().active ? "Đang áp dụng" : "Tạm ngưng"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    boolean act = "Đang áp dụng".equalsIgnoreCase(item);
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", act ? "badge-trong" : "badge-bao_tri");
                    badge.setStyle(
                            "-fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 4 14; -fx-min-width: 120px;");
                    badge.setMinWidth(Region.USE_PREF_SIZE);
                    badge.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    badge.setTextOverrun(OverrunStyle.CLIP);
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        TableColumn<WorkShift, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setMinWidth(180);
        actionCol.setPrefWidth(200);
        actionCol.setStyle("-fx-alignment: CENTER_LEFT;");

        table.getColumns().addAll(idCol, nameCol, startCol, endCol, statusCol, actionCol);

        // ── THANH PHÂN TRANG HIỆN ĐẠI ──────────────────────────────────────────
        HBox paginationBar = new HBox(12);
        paginationBar.setAlignment(Pos.CENTER_LEFT);
        paginationBar.setPadding(new Insets(10, 8, 4, 8));
        paginationBar.setStyle("-fx-background-color: transparent;");

        Label recordInfoLabel = new Label("Hiển thị 0 ca làm việc");
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

            String sDir = "ID giảm dần".equals(sortOrder.getValue()) ? "desc" : "asc";
            Util.runAsync(() -> ApiService.workShiftPage(currentPage[0], currentPageSize[0], q, sDir), page -> {
                data.setAll(page.getContent() != null ? page.getContent() : List.of());
                table.scrollTo(0);

                int totalPages = Math.max(1, page.totalPages);
                long totalElements = page.totalElements;
                currentPage[0] = page.pageNo;

                if (totalElements == 0) {
                    recordInfoLabel.setText("Hiển thị 0 ca làm việc");
                } else {
                    int from = (page.pageNo - 1) * page.pageSize + 1;
                    int to = (int) Math.min((long) page.pageNo * page.pageSize, totalElements);
                    recordInfoLabel
                            .setText("Hiển thị " + from + " – " + to + " trên tổng " + totalElements + " ca làm việc");
                }

                renderPageButtons(pageButtonsBox, currentPage[0], totalPages,
                        newPage -> loadPageHolder[0].accept(newPage));
            });
        };

        Runnable loadCurrentPage = () -> loadPageHolder[0].accept(currentPage[0]);
        Runnable doSearch = () -> loadPageHolder[0].accept(1);

        searchBtn.setOnAction(e -> doSearch.run());
        search.setOnAction(e -> doSearch.run());

        refreshBtn.setOnAction(e -> {
            search.clear();
            doSearch.run();
        });

        pageSizeCombo.setOnAction(e -> {
            Integer sz = pageSizeCombo.getValue();
            if (sz != null && sz != currentPageSize[0]) {
                currentPageSize[0] = sz;
                doSearch.run();
            }
        });

        // ── Action cell ───────────────────────────────────────────────────
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button delBtn = new Button("Xóa");
            private final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-action-edit");
                delBtn.getStyleClass().add("btn-action-del");

                // Thêm vector icon Bootstrap
                editBtn.setGraphic(IconUtils.icon("edit", 12, Color.WHITE));
                editBtn.setGraphicTextGap(6);
                editBtn.setMinWidth(68);
                editBtn.setStyle("-fx-padding: 4 11; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 68px;");

                delBtn.setGraphic(IconUtils.icon("trash", 12, Color.WHITE));
                delBtn.setGraphicTextGap(6);
                delBtn.setMinWidth(68);
                delBtn.setStyle("-fx-padding: 4 11; -fx-font-size: 12px; -fx-font-weight: 600; -fx-min-width: 68px;");

                box.setAlignment(Pos.CENTER_LEFT);
                setStyle("-fx-padding: 6 14;");
                setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    WorkShift ws = getTableView().getItems().get(getIndex());
                    openEditDialog(ws, loadCurrentPage);
                });

                delBtn.setOnAction(e -> {
                    WorkShift ws = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Bạn có chắc chắn muốn xóa ca \"" + ws.shiftName + "\" (" + ws.startTime + " - "
                                    + ws.endTime + ")?");
                    confirm.setHeaderText("Xác nhận xóa ca làm việc");
                    confirm.setTitle("LotusStay — Xác nhận");
                    applyTheme(confirm);
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            Util.runAsyncVoid(() -> ApiService.deleteWorkShift(ws.id), loadCurrentPage);
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        addBtn.setOnAction(e -> openAddDialog(loadCurrentPage));
        doSearch.run();

        card.getChildren().addAll(headerRow, toolbar, table, paginationBar);
        root.setCenter(card);
        return root;
    }

    // ── Dialog Thêm ca (Modern Dark Modal) ───────────────────────────────
    private static void openAddDialog(Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("LotusStay — Thêm ca làm việc");
        applyTheme(dialog);

        TextField nameField = new TextField();
        nameField.setPromptText("VD: Ca sáng, Ca chiều, Ca đêm...");
        nameField.setPrefHeight(38);

        TextField startField = new TextField();
        startField.setPromptText("HH:mm (VD: 06:00)");
        startField.setPrefHeight(38);

        TextField endField = new TextField();
        endField.setPromptText("HH:mm (VD: 14:00)");
        endField.setPrefHeight(38);

        VBox modalContent = new VBox(16);
        modalContent.setPadding(new Insets(24));
        modalContent.setPrefWidth(480);
        modalContent.setStyle("-fx-background-color: #141A24;");

        HBox modalHeader = new HBox(12);
        modalHeader.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🕒");
        iconBadge.setStyle(
                "-fx-font-size: 20px; -fx-background-color: rgba(245, 158, 11, 0.18); -fx-padding: 8 12; -fx-background-radius: 10;");

        VBox titleArea = new VBox(3);
        Label hTitle = new Label("Thêm ca làm việc mới");
        hTitle.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        Label hSub = new Label("Định nghĩa khung giờ làm việc chuẩn cho khách sạn");
        hSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-text-fill: #94A3B8;");
        titleArea.getChildren().addAll(hTitle, hSub);
        modalHeader.getChildren().addAll(iconBadge, titleArea);

        // Form fields
        VBox formBody = new VBox(12);
        formBody.getChildren().addAll(
                buildFormField("Tên ca làm việc (*)", nameField),
                buildFormField("Giờ bắt đầu (*)", startField),
                buildFormField("Giờ kết thúc (*)", endField));

        modalContent.getChildren().addAll(modalHeader, new Separator(), formBody);
        dialog.getDialogPane().setContent(modalContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setText("Lưu ca làm việc");
            okBtn.getStyleClass().add("btn-gold");
            okBtn.setStyle(
                    "-fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 800; -fx-background-color: #F59E0B; -fx-text-fill: #0A0D14;");
        }
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Hủy bỏ");
            cancelBtn.setStyle("-fx-text-fill: #94A3B8;");
        }

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String name = nameField.getText().trim();
                String start = startField.getText().trim();
                String end = endField.getText().trim();
                if (name.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    Util.info("Vui lòng nhập đầy đủ tên ca, giờ bắt đầu và giờ kết thúc.");
                    return null;
                }
                Util.runAsync(() -> ApiService.createWorkShift(name, start, end), w -> reload.run());
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ── Dialog Sửa ca (Modern Dark Modal) ───────────────────────────────
    private static void openEditDialog(WorkShift ws, Runnable reload) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("LotusStay — Cập nhật ca làm việc");
        applyTheme(dialog);

        TextField nameField = new TextField(ws.shiftName);
        nameField.setPrefHeight(38);
        TextField startField = new TextField(ws.startTime);
        startField.setPrefHeight(38);
        TextField endField = new TextField(ws.endTime);
        endField.setPrefHeight(38);

        VBox modalContent = new VBox(16);
        modalContent.setPadding(new Insets(24));
        modalContent.setPrefWidth(480);
        modalContent.setStyle("-fx-background-color: #141A24;");

        HBox modalHeader = new HBox(12);
        modalHeader.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("✏️");
        iconBadge.setStyle(
                "-fx-font-size: 20px; -fx-background-color: rgba(245, 158, 11, 0.18); -fx-padding: 8 12; -fx-background-radius: 10;");

        VBox titleArea = new VBox(3);
        Label hTitle = new Label("Chỉnh sửa ca làm việc");
        hTitle.setStyle(
                "-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        Label hSub = new Label("Thay đổi tên hoặc thời gian làm việc của " + ws.shiftName);
        hSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-text-fill: #94A3B8;");
        titleArea.getChildren().addAll(hTitle, hSub);
        modalHeader.getChildren().addAll(iconBadge, titleArea);

        VBox formBody = new VBox(12);
        formBody.getChildren().addAll(
                buildFormField("Tên ca làm việc (*)", nameField),
                buildFormField("Giờ bắt đầu (*)", startField),
                buildFormField("Giờ kết thúc (*)", endField));

        modalContent.getChildren().addAll(modalHeader, new Separator(), formBody);
        dialog.getDialogPane().setContent(modalContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setText("Lưu thay đổi");
            okBtn.getStyleClass().add("btn-gold");
            okBtn.setStyle(
                    "-fx-font-family: 'Montserrat', sans-serif; -fx-font-weight: 800; -fx-background-color: #F59E0B; -fx-text-fill: #0A0D14;");
        }
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setText("Hủy bỏ");
            cancelBtn.setStyle("-fx-text-fill: #94A3B8;");
        }

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String name = nameField.getText().trim();
                String start = startField.getText().trim();
                String end = endField.getText().trim();
                if (name.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    Util.info("Vui lòng nhập đầy đủ thông tin.");
                    return null;
                }
                Util.runAsync(() -> ApiService.updateWorkShift(ws.id, name, start, end), w -> reload.run());
            }
            return null;
        });
        dialog.showAndWait();
    }

    private static VBox buildFormField(String labelText, Control inputControl) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        label.setStyle(
                "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        box.getChildren().addAll(label, inputControl);
        return box;
    }

    private static void applyTheme(Dialog<?> d) {
        d.getDialogPane().getStylesheets().add(
                WorkShiftView.class.getResource("/com/lotusstay/javafx/theme.css").toExternalForm());
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

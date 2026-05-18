package com.warehouseware.ui;

import com.warehouseware.application.*;
import com.warehouseware.domain.*;
import com.warehouseware.persistence.DatabaseManager;
import com.warehouseware.persistence.WarehouseWareRepository;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseWareApp extends Application {
    private WarehouseFacade facade;

    private User currentUser;
    private PermissionStrategy permissionStrategy;

    private Stage stage;

    private final TableView<Item> itemsTable = new TableView<>();
    private final TableView<Category> categoriesTable = new TableView<>();
    private final TableView<Warehouse> warehousesTable = new TableView<>();
    private final TableView<TransactionLog> transactionsTable = new TableView<>();
    private final TableView<Item> lowStockTable = new TableView<>();
    private final TableView<RestockRequest> restockTable = new TableView<>();

    private final Label totalItemsLabel = new Label("0");
    private final Label totalUnitsLabel = new Label("0");
    private final Label totalValueLabel = new Label("0.00");
    private final Label lowStockLabel = new Label("0");

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        loadCustomFonts();

        WarehouseWareRepository repository = new WarehouseWareRepository(DatabaseManager.getInstance());
        repository.initializeSchemaAndSeedData();

        this.facade = new WarehouseFacade(repository);
        this.facade.addStockAlertListener(new AutoRestockListener(repository));

        stage.setTitle("WarehouseWare - Inventory Management System");
        stage.setScene(buildLoginScene());
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.show();
    }

    private void loadCustomFonts() {
        loadFontIfPresent("/fonts/Oswald-Regular.ttf");
        loadFontIfPresent("/fonts/Oswald-VariableFont_wght.ttf");
        loadFontIfPresent("/fonts/oswald.ttf");
        loadFontIfPresent("/fonts/NotoSans-Regular.ttf");
        loadFontIfPresent("/fonts/NotoSans-VariableFont_wdth,wght.ttf");
        loadFontIfPresent("/fonts/NotoSans-VariableFont_wght.ttf");
        loadFontIfPresent("/fonts/Noto Sans-Regular.ttf");
        loadFontIfPresent("/fonts/noto-sans.ttf");
        loadFontIfPresent("/fonts/BubblegumSans-Regular.ttf");
        loadFontIfPresent("/fonts/Bubblegum Sans-Regular.ttf");
        loadFontIfPresent("/fonts/BubblegumSans.ttf");
        loadFontIfPresent("/fonts/bubblegum-sans.ttf");
        loadFontIfPresent("/fonts/RubikBubbles-Regular.ttf");
        loadFontIfPresent("/fonts/BubblerOne-Regular.ttf");
    }

    private void loadFontIfPresent(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (Exception ignored) {
        }
    }

    private Scene buildLoginScene() {
        Label title = new Label("WarehouseWare");
        title.getStyleClass().add("login-brand-title");
        title.setStyle("-fx-font-family: 'Oswald'; -fx-font-size: 48px; -fx-font-weight: 700;");
        title.setTextOverrun(OverrunStyle.CLIP);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("bubble-input");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("bubble-input");

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("bubble-button");
        loginBtn.setOnAction(e -> {
            try {
                currentUser = facade.login(usernameField.getText(), passwordField.getText());
                permissionStrategy = PermissionStrategy.forRole(currentUser.getRole());
                stage.setScene(buildMainScene());
                stage.centerOnScreen();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        VBox box = new VBox(14, title, usernameField, passwordField, loginBtn);
        box.setPadding(new Insets(28));
        box.setMaxWidth(600);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("login-card");

        StackPane root = new StackPane(box);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("login-root");
        Scene scene = new Scene(root, 900, 550);
        scene.getStylesheets().add(getClass().getResource("/styles/warehouseware.css").toExternalForm());
        return scene;
    }

    private Scene buildMainScene() {
        Label appTitle = new Label("WAREHOUSEWARE");
        appTitle.getStyleClass().add("brand-title");
        appTitle.setTextOverrun(OverrunStyle.CLIP);
        appTitle.setMaxWidth(Double.MAX_VALUE);

        Label userBadge = new Label(currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userBadge.getStyleClass().add("user-badge");

        Button createAccountBtn = new Button("Create Account");
        createAccountBtn.getStyleClass().add("bubble-button");
        createAccountBtn.setDisable(currentUser.getRole() != Role.MANAGER);
        createAccountBtn.setOnAction(e -> showCreateAccountDialog());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("bubble-button");
        logoutBtn.setOnAction(e -> {
            currentUser = null;
            permissionStrategy = null;
            stage.setScene(buildLoginScene());
            stage.centerOnScreen();
        });

        VBox sideBar = new VBox(12);
        sideBar.getStyleClass().add("sidebar");
        sideBar.setPadding(new Insets(22));
        sideBar.setPrefWidth(430);

        TabPane tabs = new TabPane();
        List<Button> navButtons = new ArrayList<>();

        tabs.getTabs().add(createDashboardTab());
        navButtons.add(navBubble("Dashboard", tabs.getTabs().size() - 1, tabs));

        if (currentUser.getRole() == Role.MANAGER) {
            tabs.getTabs().add(createCategoriesTab());
            navButtons.add(navBubble("Categories", tabs.getTabs().size() - 1, tabs));

            tabs.getTabs().add(createWarehousesTab());
            navButtons.add(navBubble("Warehouses", tabs.getTabs().size() - 1, tabs));

            tabs.getTabs().add(createItemsTab());
            navButtons.add(navBubble("Items", tabs.getTabs().size() - 1, tabs));
        }

        tabs.getTabs().add(createTransactionsTab());
        navButtons.add(navBubble("Transactions", tabs.getTabs().size() - 1, tabs));

        tabs.getTabs().add(createAlertsTab());
        navButtons.add(navBubble("Alerts", tabs.getTabs().size() - 1, tabs));

        tabs.getTabs().add(createAccountTab());
        navButtons.add(navBubble("Manage Account", tabs.getTabs().size() - 1, tabs));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("bubble-tabs");
        sideBar.getChildren().add(appTitle);
        sideBar.getChildren().addAll(navButtons);
        sideBar.getChildren().add(new Separator());
        sideBar.getChildren().add(userBadge);
        if (currentUser.getRole() == Role.MANAGER) {
            sideBar.getChildren().add(createAccountBtn);
        }
        sideBar.getChildren().add(logoutBtn);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-root");
        root.setLeft(sideBar);

        VBox center = new VBox(12, tabs);
        center.setPadding(new Insets(18));
        root.setCenter(center);

        refreshAll();
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/warehouseware.css").toExternalForm());
        return scene;
    }

    private Tab createDashboardTab() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(20));

        grid.add(statCard("Total Unique Items", totalItemsLabel), 0, 0);
        grid.add(statCard("Total Units In Stock", totalUnitsLabel), 1, 0);
        grid.add(statCard("Total Inventory Value", totalValueLabel), 0, 1);
        grid.add(statCard("Low Stock Items", lowStockLabel), 1, 1);

        Button refreshBtn = new Button("Refresh Dashboard");
        refreshBtn.setOnAction(e -> refreshDashboard());

        refreshBtn.getStyleClass().add("bubble-button");

        VBox layout = new VBox(16, grid, refreshBtn);
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");
        return new Tab("Dashboard", layout);
    }

    private VBox statCard(String title, Label value) {
        Label head = new Label(title);
        head.getStyleClass().add("subtle-text");
        value.getStyleClass().add("stat-value");
        VBox card = new VBox(8, head, value);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(320);
        return card;
    }

    private Tab createCategoriesTab() {
        TableColumn<Category, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        shrinkIdColumn(idCol);

        TableColumn<Category, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        categoriesTable.getColumns().setAll(idCol, nameCol);
        categoriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        categoriesTable.getStyleClass().add("bubble-table");

        TextField nameField = new TextField();
        nameField.setPromptText("Category Name");
        nameField.getStyleClass().add("bubble-input");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("bubble-button");
        addBtn.setOnAction(e -> {
            runAction(() -> facade.addCategory(nameField.getText()), this::refreshAll);
            nameField.clear();
        });

        Button updateBtn = new Button("Update Selected");
        updateBtn.getStyleClass().add("bubble-button");
        updateBtn.setOnAction(e -> {
            Category selected = categoriesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Select a category first.");
                return;
            }
            runAction(() -> facade.updateCategory(selected.getId(), nameField.getText()), this::refreshAll);
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("bubble-button");
        deleteBtn.setDisable(!permissionStrategy.canDeleteMasterData());
        deleteBtn.setOnAction(e -> {
            Category selected = categoriesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Select a category first.");
                return;
            }
            runAction(() -> facade.deleteCategory(selected.getId()), this::refreshAll);
        });

        categoriesTable.setOnMouseClicked(e -> {
            Category selected = categoriesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nameField.setText(selected.getName());
            }
        });

        HBox form = new HBox(10, nameField, addBtn, updateBtn, deleteBtn);
        form.setPadding(new Insets(10));
        ComboBox<Category> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("View items for category");
        categoryFilter.getStyleClass().add("bubble-input");

        TableView<Item> categoryItemsTable = new TableView<>();
        categoryItemsTable.getColumns().setAll(
                strCol("SKU", Item::getSku),
                strCol("Name", Item::getName),
                strCol("Warehouse", Item::getWarehouseName),
                intCol("Qty", Item::getQuantity)
        );
        categoryItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        categoryItemsTable.getStyleClass().add("bubble-table");

        Button viewCategoryBtn = new Button("View Category Items");
        viewCategoryBtn.getStyleClass().add("bubble-button");
        viewCategoryBtn.setOnAction(e -> {
            Category c = categoryFilter.getValue();
            if (c == null) {
                showError("Select a category to view.");
                return;
            }
            categoryItemsTable.setItems(FXCollections.observableArrayList(
                    facade.listItems().stream().filter(i -> i.getCategoryId() == c.getId()).toList()
            ));
        });

        HBox viewRow = new HBox(10, categoryFilter, viewCategoryBtn);
        VBox layout = new VBox(10, categoriesTable, form, new Separator(), viewRow, categoryItemsTable);
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");

        Tab tab = new Tab("Categories", layout);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                categoryFilter.setItems(FXCollections.observableArrayList(facade.listCategories()));
            }
        });
        return tab;
    }

    private Tab createWarehousesTab() {
        TableColumn<Warehouse, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        shrinkIdColumn(idCol);

        TableColumn<Warehouse, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Warehouse, Number> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCapacity()));

        warehousesTable.getColumns().setAll(idCol, nameCol, capacityCol);
        warehousesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        warehousesTable.getStyleClass().add("bubble-table");

        TextField nameField = new TextField();
        nameField.setPromptText("Warehouse Name");
        nameField.getStyleClass().add("bubble-input");

        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity");
        capacityField.getStyleClass().add("bubble-input");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("bubble-button");
        addBtn.setOnAction(e -> {
            runAction(() -> facade.addWarehouse(nameField.getText(), parseInt(capacityField.getText(), "Invalid capacity")), this::refreshAll);
            nameField.clear();
            capacityField.clear();
        });

        Button updateBtn = new Button("Update Selected");
        updateBtn.getStyleClass().add("bubble-button");
        updateBtn.setOnAction(e -> {
            Warehouse selected = warehousesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Select a warehouse first.");
                return;
            }
            runAction(() -> facade.updateWarehouse(
                    selected.getId(),
                    nameField.getText(),
                    parseInt(capacityField.getText(), "Invalid capacity")
            ), this::refreshAll);
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("bubble-button");
        deleteBtn.setDisable(!permissionStrategy.canDeleteMasterData());
        deleteBtn.setOnAction(e -> {
            Warehouse selected = warehousesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Select a warehouse first.");
                return;
            }
            runAction(() -> facade.deleteWarehouse(selected.getId()), this::refreshAll);
        });

        warehousesTable.setOnMouseClicked(e -> {
            Warehouse selected = warehousesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                nameField.setText(selected.getName());
                capacityField.setText(String.valueOf(selected.getCapacity()));
            }
        });

        HBox form = new HBox(10, nameField, capacityField, addBtn, updateBtn, deleteBtn);
        form.setPadding(new Insets(10));
        ComboBox<Warehouse> warehouseFilter = new ComboBox<>();
        warehouseFilter.setPromptText("View items for warehouse");
        warehouseFilter.getStyleClass().add("bubble-input");

        TableView<Item> warehouseItemsTable = new TableView<>();
        warehouseItemsTable.getColumns().setAll(
                strCol("SKU", Item::getSku),
                strCol("Name", Item::getName),
                strCol("Category", Item::getCategoryName),
                intCol("Qty", Item::getQuantity)
        );
        warehouseItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        warehouseItemsTable.getStyleClass().add("bubble-table");

        Button viewWarehouseBtn = new Button("View Warehouse Items");
        viewWarehouseBtn.getStyleClass().add("bubble-button");
        viewWarehouseBtn.setOnAction(e -> {
            Warehouse w = warehouseFilter.getValue();
            if (w == null) {
                showError("Select a warehouse to view.");
                return;
            }
            warehouseItemsTable.setItems(FXCollections.observableArrayList(
                    facade.listItems().stream().filter(i -> i.getWarehouseId() == w.getId()).toList()
            ));
        });

        HBox viewRow = new HBox(10, warehouseFilter, viewWarehouseBtn);
        VBox layout = new VBox(10, warehousesTable, form, new Separator(), viewRow, warehouseItemsTable);
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");

        Tab tab = new Tab("Warehouses", layout);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                warehouseFilter.setItems(FXCollections.observableArrayList(facade.listWarehouses()));
            }
        });
        return tab;
    }

    private Tab createItemsTab() {
        TableColumn<Item, Number> idCol = intCol("ID", Item::getId);
        shrinkIdColumn(idCol);

        itemsTable.getColumns().setAll(
                idCol,
                strCol("SKU", Item::getSku),
                strCol("Name", Item::getName),
                strCol("Category", Item::getCategoryName),
                strCol("Warehouse", Item::getWarehouseName),
                intCol("Qty", Item::getQuantity),
                intCol("Notification Qty", Item::getNotificationQty),
                intCol("Auto Restock Qty", Item::getAutoRestockQty),
                intCol("Qty To Auto-Restock", Item::getAutoRestockOrderQty),
                doubleCol("Unit Price", Item::getUnitPrice)
        );
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemsTable.getStyleClass().add("bubble-table");

        TextField skuField = new TextField();
        skuField.setPromptText("SKU");
        skuField.getStyleClass().add("bubble-input");

        TextField nameField = new TextField();
        nameField.setPromptText("Item Name");
        nameField.getStyleClass().add("bubble-input");

        ComboBox<Category> categoryBox = new ComboBox<>();
        categoryBox.setPromptText("Category");
        categoryBox.getStyleClass().add("bubble-input");

        ComboBox<Warehouse> warehouseBox = new ComboBox<>();
        warehouseBox.setPromptText("Warehouse");
        warehouseBox.getStyleClass().add("bubble-input");

        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity");
        qtyField.getStyleClass().add("bubble-input");

        TextField reorderLevelField = new TextField();
        reorderLevelField.setPromptText("Notification Qty");
        reorderLevelField.getStyleClass().add("bubble-input");

        TextField reorderQtyField = new TextField();
        reorderQtyField.setPromptText("Auto Restock Qty");
        reorderQtyField.getStyleClass().add("bubble-input");

        TextField autoRestockOrderQtyField = new TextField();
        autoRestockOrderQtyField.setPromptText("Qty To Auto-Restock");
        autoRestockOrderQtyField.getStyleClass().add("bubble-input");

        TextField unitPriceField = new TextField();
        unitPriceField.setPromptText("Unit Price");
        unitPriceField.getStyleClass().add("bubble-input");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("bubble-button");
        addBtn.setOnAction(e -> runAction(() -> facade.addItem(
                skuField.getText(),
                nameField.getText(),
                requireSelection(categoryBox, "Select category").getId(),
                requireSelection(warehouseBox, "Select warehouse").getId(),
                parseInt(qtyField.getText(), "Invalid quantity"),
                parseInt(reorderLevelField.getText(), "Invalid reorder level"),
                parseInt(reorderQtyField.getText(), "Invalid reorder qty"),
                parseInt(autoRestockOrderQtyField.getText(), "Invalid qty to auto-restock"),
                parseDouble(unitPriceField.getText(), "Invalid unit price")
        ), this::refreshAll));

        Button updateBtn = new Button("Update By SKU");
        updateBtn.getStyleClass().add("bubble-button");
        updateBtn.setOnAction(e -> {
            Item selected = facade.findItemBySku(skuField.getText());
            if (selected == null) {
                showError("Item not found by SKU.");
                return;
            }
            runAction(() -> facade.updateItem(
                    selected.getId(),
                    skuField.getText(),
                    nameField.getText(),
                    requireSelection(categoryBox, "Select category").getId(),
                    requireSelection(warehouseBox, "Select warehouse").getId(),
                    parseInt(qtyField.getText(), "Invalid quantity"),
                    parseInt(reorderLevelField.getText(), "Invalid reorder level"),
                    parseInt(reorderQtyField.getText(), "Invalid reorder qty"),
                    parseInt(autoRestockOrderQtyField.getText(), "Invalid qty to auto-restock"),
                    parseDouble(unitPriceField.getText(), "Invalid unit price")
            ), this::refreshAll);
        });

        Button deleteBtn = new Button("Delete By SKU");
        deleteBtn.getStyleClass().add("bubble-button");
        deleteBtn.setDisable(!permissionStrategy.canDeleteMasterData());
        deleteBtn.setOnAction(e -> {
            Item selected = facade.findItemBySku(skuField.getText());
            if (selected == null) {
                showError("Item not found by SKU.");
                return;
            }
            runAction(() -> facade.deleteItem(selected.getId()), this::refreshAll);
        });

        skuField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }
            Item item = facade.findItemBySku(newValue.trim());
            if (item != null) {
                nameField.setText(item.getName());
                qtyField.setText(String.valueOf(item.getQuantity()));
                reorderLevelField.setText(String.valueOf(item.getNotificationQty()));
                reorderQtyField.setText(String.valueOf(item.getAutoRestockQty()));
                autoRestockOrderQtyField.setText(String.valueOf(item.getAutoRestockOrderQty()));
                unitPriceField.setText(String.valueOf(item.getUnitPrice()));

                categoryBox.getItems().stream()
                        .filter(c -> c.getId() == item.getCategoryId())
                        .findFirst().ifPresent(categoryBox::setValue);

                warehouseBox.getItems().stream()
                        .filter(w -> w.getId() == item.getWarehouseId())
                        .findFirst().ifPresent(warehouseBox::setValue);
            }
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(8));

        form.addRow(0, new Label("SKU"), skuField, new Label("Name"), nameField);
        form.addRow(1, new Label("Category"), categoryBox, new Label("Warehouse"), warehouseBox);
        form.addRow(2, new Label("Qty"), qtyField, new Label("Notification Qty"), reorderLevelField);
        form.addRow(3, new Label("Auto Restock Qty"), reorderQtyField, new Label("Qty To Auto-Restock"), autoRestockOrderQtyField);
        form.addRow(4, new Label("Unit Price"), unitPriceField);

        HBox actions = new HBox(10, addBtn, updateBtn, deleteBtn);
        actions.setPadding(new Insets(0, 8, 8, 8));

        VBox layout = new VBox(10, itemsTable, form, actions);
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");

        Tab tab = new Tab("Items", layout);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                categoryBox.setItems(FXCollections.observableArrayList(facade.listCategories()));
                warehouseBox.setItems(FXCollections.observableArrayList(facade.listWarehouses()));
            }
        });

        return tab;
    }

    private Tab createTransactionsTab() {
        TableColumn<TransactionLog, Number> idCol = intCol("ID", TransactionLog::getId);
        shrinkIdColumn(idCol);

        transactionsTable.getColumns().setAll(
                idCol,
                strCol("Item", TransactionLog::getItemName),
                strCol("Warehouse", TransactionLog::getWarehouseName),
                strCol("Type", tx -> tx.getType().name()),
                intCol("Qty", TransactionLog::getQuantity),
                strCol("Actor", TransactionLog::getActor),
                strCol("Created At", TransactionLog::getCreatedAt)
        );
        transactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        transactionsTable.getStyleClass().add("bubble-table");

        ComboBox<Item> itemBox = new ComboBox<>();
        itemBox.setPromptText("Select Item");
        itemBox.getStyleClass().add("bubble-input");

        ChoiceBox<TransactionType> typeBox = new ChoiceBox<>(FXCollections.observableArrayList(TransactionType.values()));
        typeBox.setValue(TransactionType.STOCK_IN);
        typeBox.getStyleClass().add("bubble-input");

        TextField qtyField = new TextField();
        qtyField.setPromptText("Qty");
        qtyField.getStyleClass().add("bubble-input");

        Button postBtn = new Button("Post Transaction");
        postBtn.getStyleClass().add("bubble-button");
        postBtn.setOnAction(e -> runAction(() -> facade.postTransaction(
                requireSelection(itemBox, "Select item").getId(),
                typeBox.getValue(),
                parseInt(qtyField.getText(), "Invalid quantity"),
                currentUser.getUsername()
        ), this::refreshAll));

        ComboBox<Warehouse> reportWarehouseBox = new ComboBox<>();
        reportWarehouseBox.setPromptText("Report Warehouse (optional)");
        reportWarehouseBox.getStyleClass().add("bubble-input");
        Button generateReportBtn = new Button("Generate Stock Report PDF");
        generateReportBtn.getStyleClass().add("bubble-button");
        generateReportBtn.setOnAction(e -> {
            Warehouse w = reportWarehouseBox.getValue();
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Stock Report PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("stock-report.pdf");
            java.io.File out = fc.showSaveDialog(stage);
            if (out == null) return;

            runAction(() -> {
                Path generated = facade.generateStockReport(
                        w == null ? null : w.getId(),
                        w == null ? null : w.getName(),
                        out.toPath()
                );
                Alert info = new Alert(Alert.AlertType.INFORMATION, "Stock report generated:\n" + generated, ButtonType.OK);
                info.showAndWait();
            }, () -> {});
        });

        HBox form = new HBox(10, itemBox, typeBox, qtyField, postBtn);
        form.setPadding(new Insets(8));
        HBox reportRow = new HBox(10, reportWarehouseBox, generateReportBtn);
        reportRow.setPadding(new Insets(0, 8, 8, 8));

        VBox layout = new VBox(10, transactionsTable, form, reportRow);
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");

        Tab tab = new Tab("Transactions", layout);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                itemBox.setItems(FXCollections.observableArrayList(facade.listItems()));
                reportWarehouseBox.setItems(FXCollections.observableArrayList(facade.listWarehouses()));
            }
        });
        return tab;
    }

    private Tab createAlertsTab() {
        lowStockTable.getColumns().setAll(
                strCol("SKU", Item::getSku),
                strCol("Item", Item::getName),
                intCol("Qty", Item::getQuantity),
                intCol("Notification Qty", Item::getNotificationQty),
                strCol("Warehouse", Item::getWarehouseName)
        );
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        lowStockTable.getStyleClass().add("bubble-table");

        TableColumn<RestockRequest, Number> idCol = intCol("ID", RestockRequest::getId);
        shrinkIdColumn(idCol);

        restockTable.getColumns().setAll(
                idCol,
                strCol("Item", RestockRequest::getItemName),
                strCol("Warehouse", RestockRequest::getWarehouseName),
                intCol("Requested Qty", RestockRequest::getRequestedQuantity),
                strCol("Status", RestockRequest::getStatus),
                strCol("Created At", RestockRequest::getCreatedAt),
                strCol("Note", RestockRequest::getNote)
        );
        restockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        restockTable.getStyleClass().add("bubble-table");

        Button manualRestockBtn = new Button("Create Manual Restock");
        manualRestockBtn.getStyleClass().add("bubble-button");
        manualRestockBtn.setDisable(!permissionStrategy.canCreateRestockOrders());

        ComboBox<Item> manualItemBox = new ComboBox<>();
        manualItemBox.setPromptText("Select Item");
        manualItemBox.getStyleClass().add("bubble-input");

        TextField manualSkuSearchField = new TextField();
        manualSkuSearchField.setPromptText("Search SKU");
        manualSkuSearchField.getStyleClass().add("bubble-input");
        manualSkuSearchField.textProperty().addListener((obs, oldV, newV) -> {
            String query = newV == null ? "" : newV.trim().toLowerCase();
            manualItemBox.setItems(FXCollections.observableArrayList(
                    facade.listLowStockItems().stream()
                            .filter(i -> query.isBlank() || i.getSku().toLowerCase().contains(query))
                            .toList()
            ));
        });

        TextField manualQtyField = new TextField();
        manualQtyField.setPromptText("Restock Qty");
        manualQtyField.getStyleClass().add("bubble-input");

        manualRestockBtn.setOnAction(e -> {
            Item selected = manualItemBox.getValue();
            if (selected == null) {
                showError("Select an item first.");
                return;
            }
            runAction(() -> facade.createManualRestock(
                    selected.getId(),
                    selected.getWarehouseId(),
                    parseInt(manualQtyField.getText(), "Invalid restock quantity"),
                    "Manually created by " + currentUser.getUsername()
            ), () -> {
                // Also post a stock-in transaction so this action is reflected in Transactions tab.
                runAction(() -> facade.postTransaction(
                        selected.getId(),
                        TransactionType.STOCK_IN,
                        parseInt(manualQtyField.getText(), "Invalid restock quantity"),
                        currentUser.getUsername()
                ), this::refreshAll);
                manualQtyField.clear();
            });
        });

        VBox layout = new VBox(10,
                new Label("Low Stock Items"),
                lowStockTable,
                new HBox(8, manualSkuSearchField, manualItemBox, manualQtyField, manualRestockBtn)
        );
        layout.setPadding(new Insets(10));
        layout.getStyleClass().add("bubble-panel");

        Tab tab = new Tab("Alerts & Restock", layout);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                manualItemBox.setItems(FXCollections.observableArrayList(facade.listLowStockItems()));
                manualSkuSearchField.clear();
            }
        });
        return tab;
    }

    private Tab createAccountTab() {
        Label title = new Label("Update Password");
        title.getStyleClass().add("page-heading");

        PasswordField currentPwd = new PasswordField();
        currentPwd.setPromptText("Current Password");
        currentPwd.getStyleClass().add("bubble-input");

        PasswordField newPwd = new PasswordField();
        newPwd.setPromptText("New Password");
        newPwd.getStyleClass().add("bubble-input");

        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Confirm New Password");
        confirmPwd.getStyleClass().add("bubble-input");

        Button updateBtn = new Button("Update Password");
        updateBtn.getStyleClass().add("bubble-button");
        updateBtn.setOnAction(e -> runAction(
                () -> facade.updatePassword(
                        currentUser.getUsername(),
                        currentPwd.getText(),
                        newPwd.getText(),
                        confirmPwd.getText()
                ),
                () -> {
                    currentPwd.clear();
                    newPwd.clear();
                    confirmPwd.clear();
                }
        ));

        VBox layout = new VBox(12, title, currentPwd, newPwd, confirmPwd, updateBtn);
        layout.setPadding(new Insets(16));
        layout.getStyleClass().add("bubble-panel");
        return new Tab("Manage Account", layout);
    }

    private void showCreateAccountDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Create new user account");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/warehouseware.css").toExternalForm());

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("bubble-input");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("bubble-input");

        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.MANAGER, Role.STAFF));
        roleBox.setPromptText("Role");
        roleBox.getStyleClass().add("bubble-input");

        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(8);
        gp.addRow(0, new Label("Username"), usernameField);
        gp.addRow(1, new Label("Password"), passwordField);
        gp.addRow(2, new Label("Role"), roleBox);
        dialog.getDialogPane().setContent(gp);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            runAction(() -> facade.createUser(usernameField.getText(), passwordField.getText(), requireSelection(roleBox, "Select role")), () -> {
            });
        }
    }

    private Button navBubble(String title, int tabIndex, TabPane tabs) {
        Button btn = new Button(title);
        btn.getStyleClass().add("nav-bubble");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> tabs.getSelectionModel().select(tabIndex));
        return btn;
    }

    private void refreshAll() {
        refreshDashboard();
        categoriesTable.setItems(FXCollections.observableArrayList(facade.listCategories()));
        warehousesTable.setItems(FXCollections.observableArrayList(facade.listWarehouses()));
        itemsTable.setItems(FXCollections.observableArrayList(facade.listItems()));
        transactionsTable.setItems(FXCollections.observableArrayList(facade.listTransactions(100)));
        lowStockTable.setItems(FXCollections.observableArrayList(facade.listLowStockItems()));
        restockTable.setItems(FXCollections.observableArrayList(facade.listRestockRequests()));
    }

    private void refreshDashboard() {
        DashboardStats stats = facade.loadStats();
        totalItemsLabel.setText(String.valueOf(stats.getTotalUniqueItems()));
        totalUnitsLabel.setText(String.valueOf(stats.getTotalUnitsInStock()));
        totalValueLabel.setText(String.format("%.2f", stats.getTotalInventoryValue()));
        lowStockLabel.setText(String.valueOf(stats.getLowStockCount()));
    }

    private void openSwingSummary() {
        DashboardStats stats = facade.loadStats();
        String message = "Total items: " + stats.getTotalUniqueItems() + "\n"
                + "Total units: " + stats.getTotalUnitsInStock() + "\n"
                + "Inventory value: " + String.format("%.2f", stats.getTotalInventoryValue()) + "\n"
                + "Low stock count: " + stats.getLowStockCount();
        JOptionPane.showMessageDialog(null, message, "WarehouseWare Summary (Swing)", JOptionPane.INFORMATION_MESSAGE);
    }

    private <T> T requireSelection(ComboBox<T> box, String message) {
        T value = box.getValue();
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int parseInt(String raw, String err) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(err);
        }
    }

    private double parseDouble(String raw, String err) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(err);
        }
    }

    private void runAction(Runnable action, Runnable onSuccess) {
        try {
            action.run();
            onSuccess.run();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE, message, ButtonType.OK);
        alert.setTitle("Action Failed");
        alert.setHeaderText("WAREHOUSEWARE");
        alert.setGraphic(null);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/warehouseware.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("neon-alert");
        alert.showAndWait();
    }

    private void shrinkIdColumn(TableColumn<?, Number> idCol) {
        idCol.setMinWidth(56);
        idCol.setPrefWidth(64);
        idCol.setMaxWidth(78);
        idCol.setResizable(false);
    }

    private <T> TableColumn<T, Number> doubleCol(String title, java.util.function.ToDoubleFunction<T> getter) {
        TableColumn<T, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleDoubleProperty(getter.applyAsDouble(c.getValue())));
        return col;
    }

    private <T> TableColumn<T, Number> intCol(String title, java.util.function.ToIntFunction<T> getter) {
        TableColumn<T, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleIntegerProperty(getter.applyAsInt(c.getValue())));
        return col;
    }

    private <T> TableColumn<T, String> strCol(String title, java.util.function.Function<T, String> getter) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleStringProperty(getter.apply(c.getValue())));
        return col;
    }
}

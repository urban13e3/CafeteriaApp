package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class ManagerDashboardController implements Initializable {

    @FXML private TableView<MenuItem> menuTable;
    @FXML private TableColumn<MenuItem, String> colName;
    @FXML private TableColumn<MenuItem, String> colCategory;
    @FXML private TableColumn<MenuItem, Double> colPrice;
    @FXML private TableColumn<MenuItem, Integer> colStock;
    @FXML private TableColumn<MenuItem, String> colDescription;
    @FXML private TableColumn<MenuItem, Void> colActions;
    @FXML private TextField searchField;

    private final ObservableList<MenuItem> allItems = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> filteredItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadItems();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.trim().toLowerCase();
            if (query.isEmpty()) {
                filteredItems.setAll(allItems);
            } else {
                filteredItems.setAll(allItems.filtered(item ->
                        item.getName().toLowerCase().contains(query) ||
                        item.getCategory().toLowerCase().contains(query)
                ));
            }
        });
    }

    private void setupColumns() {
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colPrice.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrice()).asObject());
        colStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStock()).asObject());
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        // Format price column
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : "₦" + String.format("%,.2f", price));
            }
        });

        // Actions column: Edit + Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    MenuItem item = getTableView().getItems().get(getIndex());
                    showItemDialog(item);
                });

                deleteBtn.setOnAction(e -> {
                    MenuItem item = getTableView().getItems().get(getIndex());
                    handleDelete(item);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadItems() {
        allItems.clear();
        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, category, price, quantity_in_stock, description FROM menu_items ORDER BY category, name"
            );
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allItems.add(new MenuItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("quantity_in_stock"),
                    rs.getString("description") != null ? rs.getString("description") : ""
                ));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ManagerDashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
        filteredItems.setAll(allItems);
        menuTable.setItems(filteredItems);
    }

    @FXML
    private void handleAdd() {
        showItemDialog(null);
    }

    private void showItemDialog(MenuItem existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Menu Item" : "Edit Menu Item");
        dialog.setHeaderText(existing == null ? "Enter details for the new item" : "Update item details");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Form fields
        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        nameField.setPromptText("Item name");

        ChoiceBox<String> categoryBox = new ChoiceBox<>();
        categoryBox.getItems().addAll("Drinks", "Main Meal", "Swallow", "Protein", "Snacks");
        categoryBox.setValue(existing != null ? existing.getCategory() : "Drinks");

        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        priceField.setPromptText("Price");

        TextField stockField = new TextField(existing != null ? String.valueOf(existing.getStock()) : "");
        stockField.setPromptText("Quantity in stock");

        TextField descField = new TextField(existing != null ? existing.getDescription() : "");
        descField.setPromptText("Description");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));

        grid.add(new Label("Name:"), 0, 0);        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);    grid.add(categoryBox, 1, 1);
        grid.add(new Label("Price (₦):"), 0, 2);   grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);       grid.add(stockField, 1, 3);
        grid.add(new Label("Description:"), 0, 4); grid.add(descField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            String name = nameField.getText().trim();
            String category = categoryBox.getValue();
            String description = descField.getText().trim();

            if (name.isEmpty() || priceField.getText().isEmpty() || stockField.getText().isEmpty()) {
                showError("Name, price and stock are required.");
                return;
            }

            try {
                double price = Double.parseDouble(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());

                try (Connection conn = DatabaseConnection.getConnect()) {
                    if (existing == null) {
                        // Check for duplicate
                        PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT id FROM menu_items WHERE LOWER(name) = LOWER(?) AND LOWER(category) = LOWER(?)"
                        );
                        checkStmt.setString(1, name);
                        checkStmt.setString(2, category);
                        ResultSet checkRs = checkStmt.executeQuery();
                        if (checkRs.next()) {
                            showError("An item with this name already exists in the \"" + category + "\" category.");
                            return;
                        }

                        // INSERT
                        PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO menu_items (name, category, price, quantity_in_stock, description) VALUES (?,?,?,?,?)"
                        );
                        stmt.setString(1, name);
                        stmt.setString(2, category);
                        stmt.setDouble(3, price);
                        stmt.setInt(4, stock);
                        stmt.setString(5, description);
                        stmt.executeUpdate();
                    } else {
                        // UPDATE
                        PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE menu_items SET name=?, category=?, price=?, quantity_in_stock=?, description=? WHERE id=?"
                        );
                        stmt.setString(1, name);
                        stmt.setString(2, category);
                        stmt.setDouble(3, price);
                        stmt.setInt(4, stock);
                        stmt.setString(5, description);
                        stmt.setInt(6, existing.getId());
                        stmt.executeUpdate();
                    }
                }
                loadItems();

            } catch (NumberFormatException ex) {
                showError("Price must be a number and stock must be a whole number.");
            } catch (SQLException ex) {
                Logger.getLogger(ManagerDashboardController.class.getName()).log(Level.SEVERE, null, ex);
                showError("Database error: " + ex.getMessage());
            }
        }
    }

    private void handleDelete(MenuItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + item.getName() + "\"? This cannot be undone.",
                ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseConnection.getConnect()) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM menu_items WHERE id = ?"
                    );
                    stmt.setInt(1, item.getId());
                    stmt.executeUpdate();
                    loadItems();
                } catch (SQLException ex) {
                    Logger.getLogger(ManagerDashboardController.class.getName()).log(Level.SEVERE, null, ex);
                    showError("Could not delete item: " + ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        try {
            Session.getInstance().logout();
            SceneManager.switchTo("resources/views/login.fxml", false);
        } catch (IOException ex) {
            Logger.getLogger(ManagerDashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    // ── Inner model ───────────────────────────────────────────────────
    public static class MenuItem {
        private final int id;
        private String name, category, description;
        private double price;
        private int stock;

        public MenuItem(int id, String name, String category, double price, int stock, String description) {
            this.id = id; this.name = name; this.category = category;
            this.price = price; this.stock = stock; this.description = description;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
        public String getDescription() { return description; }
    }
}
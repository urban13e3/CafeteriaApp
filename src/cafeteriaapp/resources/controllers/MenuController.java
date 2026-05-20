package cafeteriaapp.resources.controllers;

import cafeteriaapp.Cart;
import cafeteriaapp.DatabaseConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MenuController implements Initializable {
    
    @FXML
    private HBox filterPillsBox;

    private String activeCategory = "All";
   
    @FXML
    private VBox menuListContainer;
    
    @FXML
    private TextField searchField;

    private final List<MenuItem> allItems = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadFromDatabase();
        renderGrouped(allItems);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String query = newVal.trim().toLowerCase();
                if (query.isEmpty()) {
                    renderGrouped(allItems);
                } else {
                    List<MenuItem> filtered = new ArrayList<>();
                    for (MenuItem item : allItems) {
                        if (item.getName().toLowerCase().contains(query)
                                || item.getCategory().toLowerCase().contains(query)) {
                            filtered.add(item);
                        }
                    }
                    renderGrouped(filtered);
                }
            });
        }
    }

    private void loadFromDatabase() {
        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name, category, price, quantity_in_stock FROM menu_items ORDER BY category, name"
            );
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allItems.add(new MenuItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity_in_stock")
                ));
            }
        } catch (SQLException ex) {
            Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void renderGrouped(List<MenuItem> items) {
        menuListContainer.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("No items found.");
            empty.getStyleClass().add("empty-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            menuListContainer.getChildren().add(empty);
            return;
        }

        Map<String, List<MenuItem>> grouped = new LinkedHashMap<>();
        for (MenuItem item : items) {
            grouped.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<MenuItem>> entry : grouped.entrySet()) {
            Label categoryLabel = new Label(entry.getKey());
            categoryLabel.getStyleClass().add("category-header");
            categoryLabel.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(categoryLabel, new javafx.geometry.Insets(16, 0, 8, 0));
            menuListContainer.getChildren().add(categoryLabel);

            for (MenuItem item : entry.getValue()) {
                menuListContainer.getChildren().add(buildCard(item));
            }
        }
    }

    private HBox buildCard(MenuItem item) {
        HBox card = new HBox(14);
        card.getStyleClass().add("menu-item-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // ── Info column ───────────────────────────────────────────────
        VBox info = new VBox(4);
        info.getStyleClass().add("item-info");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(item.getName());
        name.getStyleClass().add("item-name");
        name.setWrapText(true);

        Label stockLabel = new Label("In stock: " + item.getQuantity());
        stockLabel.getStyleClass().add("item-quantity");

        info.getChildren().addAll(name, stockLabel);

        // ── Right column ──────────────────────────────────────────────
        VBox rightCol = new VBox(8);
        rightCol.setAlignment(Pos.CENTER_RIGHT);

        // Price label — updates with quantity
        Label priceLabel = new Label("₦" + String.format("%,.2f", item.getPrice()));
        priceLabel.getStyleClass().add("item-price");

        // ── Quantity selector: [ - ] [ count ] [ + ] ─────────────────
        HBox qtyBox = new HBox(6);
        qtyBox.setAlignment(Pos.CENTER_RIGHT);

        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("qty-btn");

        Label countLabel = new Label("1");
        countLabel.getStyleClass().add("qty-count");
        countLabel.setMinWidth(24);
        countLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");

        // Minus disabled at 1 — can't go below 1
        minusBtn.setDisable(true);

        final int[] qty = {1};

        minusBtn.setOnAction(e -> {
            if (qty[0] > 1) {
                qty[0]--;
                countLabel.setText(String.valueOf(qty[0]));
                priceLabel.setText("₦" + String.format("%,.2f", item.getPrice() * qty[0]));
                if (qty[0] == 1) minusBtn.setDisable(true);
            }
        });

        plusBtn.setOnAction(e -> {
            if (qty[0] < item.getQuantity()) {
                qty[0]++;
                countLabel.setText(String.valueOf(qty[0]));
                priceLabel.setText("₦" + String.format("%,.2f", item.getPrice() * qty[0]));
                minusBtn.setDisable(false);
            }
        });

        qtyBox.getChildren().addAll(minusBtn, countLabel, plusBtn);

        // ── Add to cart button ────────────────────────────────────────
        Button addBtn = new Button("Add to Cart");
        addBtn.getStyleClass().add("add-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);

        addBtn.setOnAction(e -> {
        Cart cart = Cart.getInstance();
        cart.addItem(item.getId(), item.getName(), item.getPrice(), qty[0]);

        // Update displayed stock immediately
        int remaining = item.getQuantity() - qty[0];
        stockLabel.setText("In stock: " + remaining);

        // Disable add button if out of stock
        if (remaining <= 0) {
            addBtn.setText("Out of Stock");
            addBtn.setDisable(true);
            plusBtn.setDisable(true);
            return;
        }

        addBtn.setText("Added ✓");
        addBtn.setDisable(true);

        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ex) {}
            javafx.application.Platform.runLater(() -> {
                addBtn.setText("Add to Cart");
                addBtn.setDisable(false);
            });
        }).start();
    });

        rightCol.getChildren().addAll(priceLabel, qtyBox, addBtn);
        card.getChildren().addAll(info, rightCol);
        return card;
    }
    
    @FXML
    private void handleFilter(javafx.scene.input.MouseEvent event) {
        javafx.scene.control.Label clicked = (javafx.scene.control.Label) event.getSource();
        String category = clicked.getText();
        activeCategory = category;

        // Update pill styles
        for (javafx.scene.Node node : filterPillsBox.getChildren()) {
            if (node instanceof javafx.scene.control.Label pill) {
                if (pill.getText().equals(category)) {
                    pill.getStyleClass().setAll("filter-pill-active");
                } else {
                    pill.getStyleClass().setAll("filter-pill");
                }
            }
        }

        // Filter items
        if ("All".equals(category)) {
            renderGrouped(allItems);
        } else {
            List<MenuItem> filtered = new ArrayList<>();
            for (MenuItem item : allItems) {
                if (item.getCategory().equals(category)) {
                    filtered.add(item);
                }
            }
            renderGrouped(filtered);
        }
    }

    // ── Inner model ───────────────────────────────────────────────────
    public static class MenuItem {

        private final int id;
        private final String name;
        private final String category;
        private final double price;
        private final int quantity;

        public MenuItem(int id, String name, String category, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.quantity = quantity;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }
}
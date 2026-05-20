package cafeteriaapp.resources.controllers;

import cafeteriaapp.Cart;
import cafeteriaapp.CartItem;
import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class CartController implements Initializable {

    @FXML
    private VBox cartItemsContainer;
    @FXML
    private Label totalLabel;
    @FXML
    private Button placeOrderBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        renderCart();
    }

    private void renderCart() {
        cartItemsContainer.getChildren().clear();
        Cart cart = Cart.getInstance();

        if (cart.isEmpty()) {
            Label empty = new Label("Your cart is empty.\nBrowse the menu to add items.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888; -fx-text-alignment: center;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            cartItemsContainer.getChildren().add(empty);
            placeOrderBtn.setDisable(true);
        } else {
            placeOrderBtn.setDisable(false);
            for (CartItem item : cart.getItems()) {
                cartItemsContainer.getChildren().add(buildCartRow(item));
            }
        }

        totalLabel.setText("₦" + String.format("%,.2f", cart.getTotal()));
    }

    private HBox buildCartRow(CartItem item) {
        HBox row = new HBox(12);
        row.getStyleClass().add("cart-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Name + unit price
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(item.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        name.setWrapText(true);

        Label unitPrice = new Label("₦" + String.format("%,.2f", item.getPrice()) + " each");
        unitPrice.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");

        info.getChildren().addAll(name, unitPrice);

        // Qty controls
        HBox qtyBox = new HBox(6);
        qtyBox.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("qty-btn");

        Label countLabel = new Label(String.valueOf(item.getQuantity()));
        countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 24px;");
        countLabel.setAlignment(Pos.CENTER);

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");

        if (item.getQuantity() == 1) minusBtn.setDisable(true);

        minusBtn.setOnAction(e -> {
            Cart.getInstance().updateQuantity(item.getMenuItemId(), item.getQuantity() - 1);
            renderCart();
        });

        plusBtn.setOnAction(e -> {
            Cart.getInstance().updateQuantity(item.getMenuItemId(), item.getQuantity() + 1);
            renderCart();
        });

        qtyBox.getChildren().addAll(minusBtn, countLabel, plusBtn);

        // Subtotal
        Label subtotal = new Label("₦" + String.format("%,.2f", item.getSubtotal()));
        subtotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-min-width: 100px;");
        subtotal.setAlignment(Pos.CENTER_RIGHT);

        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53935; -fx-font-size: 16px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            Cart.getInstance().removeItem(item.getMenuItemId());
            renderCart();
        });

        row.getChildren().addAll(info, qtyBox, subtotal, removeBtn);
        return row;
    }

    @FXML
    private void handlePlaceOrder() {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) {
            showError("You must be logged in to place an order.");
            return;
        }

        Cart cart = Cart.getInstance();
        if (cart.isEmpty()) {
            showError("Your cart is empty.");
            return;
        }
        
        // Check if user has enough balance
        if (cart.getTotal() > user.getAccountBalance()) {
            showError("Insufficient wallet balance.\n\n" +
                      "Order total: ₦" + String.format("%,.2f", cart.getTotal()) + "\n" +
                      "Your balance: ₦" + String.format("%,.2f", user.getAccountBalance()) + "\n\n" +
                      "Please add funds in your Wallet.");
            return;
        }
        
        // Check live stock for all items before placing order
        try (Connection conn = DatabaseConnection.getConnect()) {
            for (CartItem item : cart.getItems()) {
                PreparedStatement stockCheck = conn.prepareStatement(
                    "SELECT quantity_in_stock, name FROM menu_items WHERE id = ?"
                );
                stockCheck.setInt(1, item.getMenuItemId());
                ResultSet stockRs = stockCheck.executeQuery();
                if (stockRs.next()) {
                    int available = stockRs.getInt("quantity_in_stock");
                    String name = stockRs.getString("name");
                    if (available <= 0) {
                        showError(name + " is out of stock. Please remove it from your cart.");
                        return;
                    }
                    if (item.getQuantity() > available) {
                        showError("Only " + available + " portion(s) of " + name +
                                 " available. Please reduce the quantity in your cart.");
                        return;
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
            showError("Failed to verify stock. Please try again.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnect()) {
            // Insert into orders
            PreparedStatement orderStmt = conn.prepareStatement(
                    "INSERT INTO orders (user_id, total_amount) VALUES (?, ?) RETURNING id"
            );
            orderStmt.setInt(1, user.getId());
            orderStmt.setDouble(2, cart.getTotal());
            ResultSet rs = orderStmt.executeQuery();

            if (rs.next()) {
                int orderId = rs.getInt("id");

                // Insert each order item
                PreparedStatement itemStmt = conn.prepareStatement(
                        "INSERT INTO order_items (order_id, menu_item_id, item_name, quantity, unit_price) VALUES (?,?,?,?,?)"
                );
                for (CartItem item : cart.getItems()) {
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, item.getMenuItemId());
                    itemStmt.setString(3, item.getName());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setDouble(5, item.getPrice());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
                
                // Reduce stock for each ordered item
                PreparedStatement stockStmt = conn.prepareStatement(
                    "UPDATE menu_items SET quantity_in_stock = quantity_in_stock - ? WHERE id = ?"
                );
                for (CartItem item : cart.getItems()) {
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getMenuItemId());
                    stockStmt.addBatch();
                }
                stockStmt.executeBatch();
                
                // Deduct balance from user account
                PreparedStatement balanceStmt = conn.prepareStatement(
                    "UPDATE users SET account_balance = account_balance - ? WHERE id = ?"
                );
                balanceStmt.setDouble(1, cart.getTotal());
                balanceStmt.setInt(2, user.getId());
                balanceStmt.executeUpdate();
                
                // Record transaction
                PreparedStatement txStmt = conn.prepareStatement(
                    "INSERT INTO transactions (user_id, type, amount, description) VALUES (?,?,?,?)"
                );
                txStmt.setInt(1, user.getId());
                txStmt.setString(2, "debit");
                txStmt.setDouble(3, cart.getTotal());
                txStmt.setString(4, "Order #" + orderId);
                txStmt.executeUpdate();

                // Update session balance
                user.setAccountBalance((float)(user.getAccountBalance() - cart.getTotal()));

                // Show receipt
                showReceipt(orderId, cart);

                // Clear cart and go back to dashboard
                cart.clear();
                Parent view = FXMLLoader.load(
                    getClass().getResource("../views/home-view.fxml")
                );
                javafx.scene.Scene scene = placeOrderBtn.getScene();
                BorderPane dashboard = (BorderPane) scene.getRoot();
                Pane centerPane = (Pane) dashboard.getCenter();
                centerPane.getChildren().setAll(view);
                if (view instanceof Region region) {
                    region.prefWidthProperty().bind(centerPane.widthProperty());
                    region.prefHeightProperty().bind(centerPane.heightProperty());
                }
            }

        } catch (SQLException | IOException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
            showError("Failed to place order. Please try again.");
        }
    }

    private void showReceipt(int orderId, Cart cart) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Order #").append(orderId).append(" confirmed!\n\n");
        receipt.append("─────────────────────────\n");
        for (CartItem item : cart.getItems()) {
            receipt.append(item.getName())
                    .append(" x").append(item.getQuantity())
                    .append("  ₦").append(String.format("%,.2f", item.getSubtotal()))
                    .append("\n");
        }
        receipt.append("─────────────────────────\n");
        receipt.append("TOTAL:  ₦").append(String.format("%,.2f", cart.getTotal()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Receipt");
        alert.setHeaderText("Thank you for your order!");
        alert.setContentText(receipt.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleContinueShopping() {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/menu.fxml")
            );
            javafx.scene.Scene scene = placeOrderBtn.getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
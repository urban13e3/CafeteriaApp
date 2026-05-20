package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.Session;
import cafeteriaapp.User;
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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OrderHistoryController implements Initializable {

    @FXML
    private VBox historyContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadHistory();
    }

    private void loadHistory() {
        historyContainer.getChildren().clear();
        User user = Session.getInstance().getCurrentUser();

        if (user == null) return;

        try (Connection conn = DatabaseConnection.getConnect()) {

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT o.id, o.total_amount, o.created_at " +
                "FROM orders o " +
                "WHERE o.user_id = ? " +
                "ORDER BY o.created_at DESC"
            );
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            boolean hasOrders = false;

            while (rs.next()) {
                hasOrders = true;
                int orderId = rs.getInt("id");
                double total = rs.getDouble("total_amount");
                String date = rs.getTimestamp("created_at").toString().substring(0, 16);

                // Build order card
                VBox orderCard = new VBox(8);
                orderCard.getStyleClass().add("order-card");

                // Order header row
                HBox header = new HBox();
                header.setAlignment(Pos.CENTER_LEFT);
                header.setSpacing(20);

                Label orderLabel = new Label("Order #" + orderId);
                orderLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                HBox.setHgrow(orderLabel, Priority.ALWAYS);

                Label dateLabel = new Label(date);
                dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");

                header.getChildren().addAll(orderLabel, dateLabel);

                // Load order items for this order
                PreparedStatement itemStmt = conn.prepareStatement(
                    "SELECT item_name, quantity, unit_price FROM order_items WHERE order_id = ?"
                );
                itemStmt.setInt(1, orderId);
                ResultSet itemRs = itemStmt.executeQuery();

                VBox itemsList = new VBox(4);
                while (itemRs.next()) {
                    String itemName = itemRs.getString("item_name");
                    int qty = itemRs.getInt("quantity");
                    double unitPrice = itemRs.getDouble("unit_price");

                    HBox itemRow = new HBox();
                    itemRow.setAlignment(Pos.CENTER_LEFT);
                    itemRow.setSpacing(20);

                    Label itemLabel = new Label(itemName + " x" + qty);
                    itemLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");
                    HBox.setHgrow(itemLabel, Priority.ALWAYS);

                    Label itemPrice = new Label("₦" + String.format("%,.2f", unitPrice * qty));
                    itemPrice.setStyle("-fx-font-size: 13px; -fx-text-fill: #444444;");

                    itemRow.getChildren().addAll(itemLabel, itemPrice);
                    itemsList.getChildren().add(itemRow);
                }

                // Total row
                HBox totalRow = new HBox();
                totalRow.setAlignment(Pos.CENTER_LEFT);
                totalRow.setSpacing(20);
                totalRow.setStyle("-fx-border-color: transparent transparent #e0e0e0 transparent; -fx-border-width: 1 0 0 0; -fx-padding: 8 0 0 0;");

                Label totalText = new Label("Total");
                totalText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                HBox.setHgrow(totalText, Priority.ALWAYS);

                Label totalAmount = new Label("₦" + String.format("%,.2f", total));
                totalAmount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

                totalRow.getChildren().addAll(totalText, totalAmount);

                orderCard.getChildren().addAll(header, itemsList, totalRow);
                historyContainer.getChildren().add(orderCard);
            }

            if (!hasOrders) {
                Label empty = new Label("You haven't placed any orders yet.");
                empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");
                empty.setMaxWidth(Double.MAX_VALUE);
                empty.setAlignment(Pos.CENTER);
                VBox.setMargin(empty, new javafx.geometry.Insets(40, 0, 0, 0));
                historyContainer.getChildren().add(empty);
            }

        } catch (SQLException ex) {
            Logger.getLogger(OrderHistoryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
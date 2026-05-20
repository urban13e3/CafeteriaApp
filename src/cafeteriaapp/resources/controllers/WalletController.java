package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private VBox transactionsContainer;

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = Session.getInstance().getCurrentUser();
        refreshBalance();
        loadTransactions();
    }

    private void refreshBalance() {
        if (currentUser != null) {
            balanceLabel.setText("₦" + String.format("%,.2f", currentUser.getAccountBalance()));
        }
    }

    @FXML
    private void handleAddFunds() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fund Wallet");
        dialog.setHeaderText("Add funds to your wallet");
        dialog.setContentText("Enter amount (₦):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.trim());
                if (amount <= 0) throw new NumberFormatException();

                try (Connection conn = DatabaseConnection.getConnect()) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET account_balance = account_balance + ? WHERE id = ?"
                    );
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, currentUser.getId());
                    stmt.executeUpdate();

                    currentUser.setAccountBalance((float)(currentUser.getAccountBalance() + amount));
                    refreshBalance();
                    loadTransactions();

                    Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "₦" + String.format("%,.2f", amount) + " added successfully!", ButtonType.OK);
                    success.setTitle("Success");
                    success.setHeaderText(null);
                    success.showAndWait();

                } catch (SQLException ex) {
                    Logger.getLogger(WalletController.class.getName()).log(Level.SEVERE, null, ex);
                    showError("Failed to add funds.");
                }

            } catch (NumberFormatException e) {
                showError("Please enter a valid positive amount.");
            }
        });
    }

    private void loadTransactions() {
        transactionsContainer.getChildren().clear();

        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT o.id, o.total_amount, o.created_at, " +
                "STRING_AGG(oi.item_name || ' (' || oi.quantity || ' × ' || oi.unit_price || ')', ', ') as items " +
                "FROM orders o " +
                "JOIN order_items oi ON o.id = oi.order_id " +
                "WHERE o.user_id = ? " +
                "GROUP BY o.id, o.total_amount, o.created_at " +
                "ORDER BY o.created_at DESC " +
                "LIMIT 10"
            );
            stmt.setInt(1, currentUser.getId());
            ResultSet rs = stmt.executeQuery();

            boolean hasTransactions = false;

            while (rs.next()) {
                hasTransactions = true;
                double total = rs.getDouble("total_amount");
                Timestamp ts = rs.getTimestamp("created_at");
                String items = rs.getString("items");
                String date = ts.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("d MMM, yyyy hh:mma"));

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("transaction-row");

                // Icon circle
                VBox iconBox = new VBox();
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setMinWidth(40);
                iconBox.setMinHeight(40);
                iconBox.setStyle("-fx-background-color: #f0f0f5; -fx-background-radius: 50;");
                Label arrow = new Label("↗");
                arrow.setStyle("-fx-font-size: 16px; -fx-text-fill: #1a1a2e;");
                iconBox.getChildren().add(arrow);

                // Info
                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);

                // Truncate items string if too long
                String displayItems = items.length() > 40 ? items.substring(0, 37) + "..." : items;
                Label itemsLabel = new Label(displayItems);
                itemsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

                Label dateLabel = new Label(date);
                dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

                info.getChildren().addAll(itemsLabel, dateLabel);

                // Amount
                Label amountLabel = new Label("-₦" + String.format("%,.0f", total));
                amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e53935;");

                row.getChildren().addAll(iconBox, info, amountLabel);
                transactionsContainer.getChildren().add(row);
            }

            if (!hasTransactions) {
                Label empty = new Label("No transactions yet.");
                empty.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");
                empty.setMaxWidth(Double.MAX_VALUE);
                empty.setAlignment(Pos.CENTER);
                transactionsContainer.getChildren().add(empty);
            }

        } catch (SQLException ex) {
            Logger.getLogger(WalletController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
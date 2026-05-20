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
                    
                    // Record transaction
                    PreparedStatement txStmt = conn.prepareStatement(
                        "INSERT INTO transactions (user_id, type, amount, description) VALUES (?,?,?,?)"
                    );
                    txStmt.setInt(1, currentUser.getId());
                    txStmt.setString(2, "credit");
                    txStmt.setDouble(3, amount);
                    txStmt.setString(4, "Wallet funded");
                    txStmt.executeUpdate();
                    
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
                "SELECT type, amount, description, created_at " +
                "FROM transactions WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT 20"
            );
            stmt.setInt(1, currentUser.getId());
            ResultSet rs = stmt.executeQuery();

            boolean hasTransactions = false;

            while (rs.next()) {
                hasTransactions = true;
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                String description = rs.getString("description");
                String date = rs.getTimestamp("created_at")
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("d MMM, yyyy hh:mma"));

                boolean isCredit = "credit".equals(type);

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("transaction-row");

                // Icon circle
                VBox iconBox = new VBox();
                iconBox.setAlignment(Pos.CENTER);
                iconBox.setMinWidth(40);
                iconBox.setMinHeight(40);
                iconBox.setStyle("-fx-background-color: " +
                    (isCredit ? "#e8f5e9" : "#fce4ec") +
                    "; -fx-background-radius: 50;");
                Label arrow = new Label(isCredit ? "↓" : "↑");
                arrow.setStyle("-fx-font-size: 16px; -fx-text-fill: " +
                    (isCredit ? "#4CAF50" : "#e53935") + ";");
                iconBox.getChildren().add(arrow);

                // Info
                VBox info = new VBox(3);
                HBox.setHgrow(info, Priority.ALWAYS);

                Label descLabel = new Label(description);
                descLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

                Label dateLabel = new Label(date);
                dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

                info.getChildren().addAll(descLabel, dateLabel);

                // Amount
                Label amountLabel = new Label(
                    (isCredit ? "+" : "-") + "₦" + String.format("%,.2f", amount)
                );
                amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " +
                    (isCredit ? "#4CAF50" : "#e53935") + ";");

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
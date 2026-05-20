package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.mindrot.jbcrypt.BCrypt;

public class ProfileDetailsController implements Initializable {

    @FXML private Label userIdLabel;
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordFeedbackLabel;

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            userIdLabel.setText("PAU-" + String.format("%04d", currentUser.getId()));
            firstNameLabel.setText(currentUser.getFirstName());
            lastNameLabel.setText(currentUser.getLastName());
            emailLabel.setText(currentUser.getEmail());
        }
    }

    @FXML
    private void handleBack(MouseEvent event) {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/profile-view.fxml")
            );
            javafx.scene.Scene scene = userIdLabel.getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (Exception ex) {
            Logger.getLogger(ProfileDetailsController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleChangePassword() {
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showFeedback("All fields are required.", false);
            return;
        }

        if (!newPass.equals(confirm)) {
            showFeedback("New passwords do not match.", false);
            return;
        }

        if (newPass.length() < 6) {
            showFeedback("New password must be at least 6 characters.", false);
            return;
        }

        // Fetch stored hash and verify current password
        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement fetchStmt = conn.prepareStatement(
                "SELECT password FROM users WHERE id = ?"
            );
            fetchStmt.setInt(1, currentUser.getId());
            var rs = fetchStmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (!BCrypt.checkpw(current, storedHash)) {
                    showFeedback("Current password is incorrect.", false);
                    return;
                }

                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE id = ?"
                );
                updateStmt.setString(1, newHash);
                updateStmt.setInt(2, currentUser.getId());
                updateStmt.executeUpdate();

                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
                showFeedback("Password updated successfully!", true);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ProfileDetailsController.class.getName()).log(Level.SEVERE, null, ex);
            showFeedback("Failed to update password.", false);
        }
    }

    private void showFeedback(String message, boolean success) {
        passwordFeedbackLabel.setText(message);
        passwordFeedbackLabel.setStyle(
            "-fx-font-size: 12px; -fx-text-fill: " + (success ? "#4CAF50" : "#e53935") + ";"
        );
    }
}
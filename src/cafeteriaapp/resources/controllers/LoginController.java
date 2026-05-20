/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.mindrot.jbcrypt.BCrypt;
//import cafeteriaapp.

/**
 *
 * @author DELL
 */
public class LoginController implements Initializable {

    @FXML
    private TextField emailTextField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button continueBtn;
    @FXML
    private FontAwesomeIconView eyeIcon;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private StackPane overlayPane;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField signUpEmailField;
    @FXML
    private PasswordField signUpPasswordField;
    @FXML
    private TextField signUpVisiblePasswordField;
    @FXML
    private PasswordField signUpConfirmPasswordField;
    @FXML
    private TextField signUpVisibleConfirmPasswordField;
    @FXML
    private FontAwesomeIconView signUpEyeIcon;
    @FXML
    private FontAwesomeIconView signUpConfirmEyeIcon;
    @FXML
    private Tab signUpTab;
    @FXML
    private Tab loginTab;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label signUpLabel;
    @FXML
    private Label passwordErrorLabel;
    @FXML
    Button submitBtn;

    private boolean showPassword;
    private boolean showSignUpPassword;
    private boolean showSignUpConfirmPassword;

    private ProgressIndicator spinner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        overlayPane.setVisible(false);
        passwordErrorLabel.setVisible(false);

//        // Keep both fields in sync as the user types
        visiblePasswordField.textProperty().bindBidirectional(
                passwordField.textProperty()
        );

        signUpPasswordField.textProperty().bindBidirectional(signUpVisiblePasswordField.textProperty());
        signUpConfirmPasswordField.textProperty().bindBidirectional(signUpVisibleConfirmPasswordField.textProperty());

//        // Disable login button until both fields have text
        continueBtn.disableProperty().bind(
                emailTextField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        submitBtn.disableProperty().bind(
                this.signUpEmailField.textProperty().isEmpty()
                        .or(this.signUpPasswordField.textProperty().isEmpty())
                        .or(this.signUpConfirmPasswordField.textProperty().isEmpty())
        );

        this.signUpConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!this.signUpPasswordField.getText().equals(newValue)) {
                passwordErrorLabel.setText("Passwords do not match");
                passwordErrorLabel.setVisible(true);
            } else {
                passwordErrorLabel.setText("");
                passwordErrorLabel.setVisible(false);
            }
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailTextField.getText().trim();
        String password = passwordField.getText();

        if (!isValidEmail(email)) {
            showFieldError(emailTextField, "Please enter a valid email");
            return;
        }

        overlayPane.setVisible(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {

                try (Connection connection = DatabaseConnection.getConnect()) {

                    PreparedStatement stmt
                            = connection.prepareStatement(
                                    "SELECT * FROM users WHERE email = ?"
                            );

                    stmt.setString(1, email);

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {

                        String retrievedPassword = rs.getString("password");

                        if (comparePassword(password, retrievedPassword)) {

                            String firstName = rs.getString("first_name");
                            String lastName = rs.getString("last_name");

                            int userId = rs.getInt("id");
                            String userRole = rs.getString("role");
                            float balance = rs.getFloat("account_balance");
                            return new User(userId, firstName, lastName, email, balance, userRole);
                        }
                    }

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {

            overlayPane.setVisible(false);

            User user = loginTask.getValue();

            if (user != null) {

                try {

                    Session session = Session.getInstance();
                    session.setCurrentUser(user);

                    String role = user.getRole();
                    if ("manager".equals(role)) {
                    SceneManager.switchTo("resources/views/manager-dashboard.fxml", true);
                    } else {
                    SceneManager.switchTo("resources/views/dashboard.fxml", true);
                }

                } catch (IOException ex) {

                    Logger.getLogger(LoginController.class.getName())
                            .log(Level.SEVERE, null, ex);
                }

            } else {
                showError("Invalid email or password.");
            }
        });

        loginTask.setOnFailed(e -> {

            overlayPane.setVisible(false);

            showError(
                    "Login failed: "
                    + loginTask.getException().getMessage()
            );
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleSignUp() {
        String firstName = this.firstNameField.getText();
        String lastName = this.lastNameField.getText();
        String email = this.signUpEmailField.getText();
        String password = this.signUpPasswordField.getText();

        if (!isValidEmail(email)) {
            showFieldError(signUpEmailField, "Please enter a valid email");
            return;
        }

        String hashedPassword = this.hash(password);

        overlayPane.setVisible(true);

        Task<Boolean> signUpTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection connection = DatabaseConnection.getConnect()) {
                    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE email = ?");

                    stmt.setString(1, email);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
//                        showError("User already exists");
//                        System.out.println("Here");
                        return false;
                    }

                    stmt = connection.prepareStatement("INSERT INTO users(first_name, last_name, email, password) VALUES (?,?,?,?)");

                    stmt.setString(1, firstName);
                    stmt.setString(2, lastName);
                    stmt.setString(3, email);
                    stmt.setString(4, hashedPassword);

                    return stmt.executeUpdate() > 0;

                } catch (SQLException ex) {
                    Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
                }

                return false;
            }
        };

        signUpTask.setOnSucceeded(e -> {
            overlayPane.setVisible(false);

            if (signUpTask.getValue()) {
                showSuccess("Signup successful");
                tabPane.getSelectionModel().select(loginTab);
            } else {
//                showError("Invalid email or password.");
                showError("User already exists");
            }
        });

        signUpTask.setOnFailed(e -> {
            overlayPane.setVisible(false);
            showError("Signup failed");
//            showError("User already exists");
        });

        new Thread(signUpTask).start();

    }

    @FXML
    private void handleShowPassword() {
        this.showPassword = !this.showPassword;

        if (this.showPassword) {
            this.passwordField.setVisible(false);
            this.passwordField.setManaged(false);
            this.visiblePasswordField.setVisible(true);
            this.visiblePasswordField.setManaged(true);
            this.eyeIcon.setGlyphName(FontAwesomeIcon.EYE_SLASH.name());
        } else {
            // Restore password field
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            this.visiblePasswordField.setVisible(false);
            this.visiblePasswordField.setManaged(false);
            eyeIcon.setGlyphName(FontAwesomeIcon.EYE.name());
        }
    }

    @FXML
    private void handleSignUpShowPassword() {
        this.showSignUpPassword = !this.showSignUpPassword;

        if (this.showSignUpPassword) {
            signUpPasswordField.setVisible(false);
            signUpPasswordField.setManaged(false);
            signUpVisiblePasswordField.setVisible(true);
            signUpVisiblePasswordField.setManaged(true);
            signUpEyeIcon.setGlyphName(FontAwesomeIcon.EYE_SLASH.name());
        } else {
            signUpPasswordField.setVisible(true);
            signUpPasswordField.setManaged(true);
            signUpVisiblePasswordField.setVisible(false);
            signUpVisiblePasswordField.setManaged(false);
            signUpEyeIcon.setGlyphName(FontAwesomeIcon.EYE.name());
        }
    }

    @FXML
    private void handleSignUpShowConfirmPassword() {
        this.showSignUpConfirmPassword = !this.showSignUpConfirmPassword;

        if (this.showSignUpConfirmPassword) {
            signUpConfirmPasswordField.setVisible(false);
            signUpConfirmPasswordField.setManaged(false);
            signUpVisibleConfirmPasswordField.setVisible(true);
            signUpVisibleConfirmPasswordField.setManaged(true);
            signUpConfirmEyeIcon.setGlyphName(FontAwesomeIcon.EYE_SLASH.name());
        } else {
            signUpConfirmPasswordField.setVisible(true);
            signUpConfirmPasswordField.setManaged(true);
            signUpVisibleConfirmPasswordField.setVisible(false);
            signUpVisibleConfirmPasswordField.setManaged(false);
            signUpConfirmEyeIcon.setGlyphName(FontAwesomeIcon.EYE.name());
        }
    }

    @FXML
    private void switchToSignUpTab() {
        tabPane.getSelectionModel().select(signUpTab);
    }

    private void showFieldError(TextField field, String message) {
        field.getStyleClass().add("field-error");
        field.setTooltip(new Tooltip(message));

        // Auto-clear the error style when user starts typing again
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            field.getStyleClass().remove("field-error");
            field.setTooltip(null);
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    private String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private boolean comparePassword(String plainText, String hashedPassword) {
        return BCrypt.checkpw(plainText, hashedPassword);
    }

}

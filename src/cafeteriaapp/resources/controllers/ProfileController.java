package cafeteriaapp.resources.controllers;

import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;

public class ProfileController implements Initializable {

    @FXML private Label initialsLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            String first = currentUser.getFirstName();
            String last = currentUser.getLastName();
            initialsLabel.setText(
                (first != null && !first.isEmpty() ? first.substring(0, 1) : "") +
                (last != null && !last.isEmpty() ? last.substring(0, 1) : "")
            );
            fullNameLabel.setText(first + " " + last);
            String role = currentUser.getRole();
            roleLabel.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
        }
    }

    @FXML
    private void handleProfileDetails(MouseEvent event) {
        loadIntoCenter("profile-details.fxml");
    }

    @FXML
    private void handleSecurity(MouseEvent event) {
        loadIntoCenter("profile-details.fxml");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Session.getInstance().logout();
            SceneManager.switchTo("resources/views/login.fxml", false);
        } catch (IOException ex) {
            Logger.getLogger(ProfileController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadIntoCenter(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/" + fxmlFile)
            );
            javafx.scene.Scene scene = fullNameLabel.getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(ProfileController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
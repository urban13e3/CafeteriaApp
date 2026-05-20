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
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class HomeTabController implements Initializable {

    @FXML
    private Label greetingsLabel;
    @FXML
    private Label accountBalanceLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.getInstance().getCurrentUser();
        if (user != null) {
            int hour = java.time.LocalTime.now().getHour();
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Good morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }
        greetingsLabel.setText(greeting + ", " + user.getFirstName() + "!");
            accountBalanceLabel.setText("₦" + String.format("%,.2f", user.getAccountBalance()));
        }
    }

    @FXML
    private void navigateToMenuPage(MouseEvent event) {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/menu.fxml")
            );
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(HomeTabController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void navigateToCart(MouseEvent event) {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/cart.fxml")
            );
            // Get the dashboard's centerPane and load into it
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(HomeTabController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void navigateToOrders(MouseEvent event) {
        try {
            Parent view = FXMLLoader.load(
                getClass().getResource("../views/order-history.fxml")
            );
            javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
            BorderPane dashboard = (BorderPane) scene.getRoot();
            Pane centerPane = (Pane) dashboard.getCenter();
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(HomeTabController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
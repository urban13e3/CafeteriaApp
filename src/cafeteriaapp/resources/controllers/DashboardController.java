package cafeteriaapp.resources.controllers;

import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;

public class DashboardController implements Initializable {

    @FXML private Pane centerPane;
    @FXML private FontAwesomeIconView homeBtn, walletBtn, ordersBtn, walletTabBtn, profileBtn;

    private FontAwesomeIconView activeBtn = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Always start fresh on home
        centerPane.getChildren().clear();
        activeBtn = null;
        loadView("home-view.fxml");
        setActiveButton(homeBtn);
    }

    public void setActiveButton(FontAwesomeIconView btn) {
        if (activeBtn != null) {
            activeBtn.setFill(Paint.valueOf("#888888"));
        }
        btn.setFill(Paint.valueOf("#FFFFFF"));
        activeBtn = btn;
    }

    public void loadView(String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("../views/" + fxmlFile)
            );
            centerPane.getChildren().setAll(view);
            if (view instanceof Region region) {
                region.prefWidthProperty().bind(centerPane.widthProperty());
                region.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException e) {
            System.out.println("FAILED TO LOAD: " + fxmlFile);
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("UNEXPECTED ERROR loading: " + fxmlFile);
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHomeBtn(MouseEvent event) {
        loadView("home-view.fxml");
        setActiveButton(homeBtn);
    }

    @FXML
    private void handleWalletBtn(MouseEvent event) {
        loadView("cart.fxml");
        setActiveButton(walletBtn);
    }

    @FXML
    private void handleOrdersBtn(MouseEvent event) {
        loadView("order-history.fxml");
        setActiveButton(ordersBtn);
    }

    @FXML
    private void handleWalletTabBtn(MouseEvent event) {
        loadView("wallet-view.fxml");
        setActiveButton(walletTabBtn);
    }

    @FXML
    private void handleProfileBtn(MouseEvent event) {
        loadView("profile-view.fxml");
        setActiveButton(profileBtn);
    }
}
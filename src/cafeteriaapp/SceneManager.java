/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cafeteriaapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

/**
 *
 * @author DELL
 */
public class SceneManager {

    private static Stage primaryStage;
    private static final Map<String, Parent> cache = new HashMap<>();

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxmlPath, boolean resizable) throws IOException {
        if (!cache.containsKey(fxmlPath)) {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
            cache.put(fxmlPath, root);
        }
        primaryStage.setResizable(resizable);
        primaryStage.getScene().setRoot(cache.get(fxmlPath));
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen(); // re-center after resize so it doesn't jump to a corner
    }
}

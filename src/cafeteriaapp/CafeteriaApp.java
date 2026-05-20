/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package cafeteriaapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
//import javafx.stage.StageStyle;

/**
 *
 * @author DELL
 */
public class CafeteriaApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root  = FXMLLoader.load(getClass().getResource("resources/views/login.fxml"));
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(
                getClass().getResource("resources/css/styles.css").toExternalForm()
        );
        SceneManager.setStage(primaryStage);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        launch(args);
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cafeteriaapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author DELL
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/paucafe";
    private static final String USER = "kolawoleagbolade";
    private static final String PASSWORD = "root";

    public static Connection getConnect() throws SQLException {
        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }
}

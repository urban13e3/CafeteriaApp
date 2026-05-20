/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cafeteriaapp;

import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author DELL
 */
public class Session {

    private static final Session instance = new Session();

    private final SimpleObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    // Private constructor prevents external instantiation
    private Session() {
    }

    public static Session getInstance() {
        return instance;
    }

    public SimpleObjectProperty<User> currentUserProperty() {
        return this.currentUser;
    }

    public User getCurrentUser() {
        return this.currentUser.get();
    }

    public void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public void logout() {
        currentUser.set(null);
    }
}


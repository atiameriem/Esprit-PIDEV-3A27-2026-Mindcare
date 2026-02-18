package utils;

import models.User;

public class UserSession {

    private static UserSession instance;
    private User user;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void cleanUserSession() {
        user = null;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "user=" + user +
                '}';
    }
}

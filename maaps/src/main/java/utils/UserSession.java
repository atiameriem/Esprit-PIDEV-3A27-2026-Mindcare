package utils;

import models.AppUser;

/**
 * Simple session in memory for tests.
 */
public class UserSession {
    private static AppUser currentUser;

    public static AppUser getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(AppUser user) {
        currentUser = user;
    }

    public static void logout() { currentUser = null; }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean hasRole(String role) {
        return currentUser != null && role != null && role.equalsIgnoreCase(currentUser.getRole());
    }

    public static boolean isPatient() { return hasRole("patient"); }
    public static boolean isResponsableCentre() { return hasRole("responsable_centre") || hasRole("responsable"); }
    public static boolean isAdmin() { return hasRole("admin"); }

    public static boolean canManageLocaux() {
        return isAdmin() || isResponsableCentre();
    }

    public static boolean canManageReservationsStatus() {
        return isAdmin() || isResponsableCentre();
    }
}

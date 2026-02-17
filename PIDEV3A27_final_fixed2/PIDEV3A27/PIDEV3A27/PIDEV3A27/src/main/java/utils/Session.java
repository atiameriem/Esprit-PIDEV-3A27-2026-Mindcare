package utils;

public class Session {

    public enum Role { PATIENT, PSYCHOLOGUE }

    private static int userId;
    private static Role role;
    private static String fullName;

    public static void login(int id, Role r, String name) {
        userId = id;
        role = r;
        fullName = name;
    }

    public static int getUserId() { return userId; }
    public static Role getRole() { return role; }
    public static String getFullName() { return fullName; }

    public static boolean isPatient() { return role == Role.PATIENT; }
    public static boolean isPsychologue() { return role == Role.PSYCHOLOGUE; }

    public static void logout() {
        userId = 0;
        role = null;
        fullName = null;
    }
}

package utils;

/**
 * Session simple utilisée dans tout le projet.
 * Remplie au moment du login.
 */
public class Session {
    public static int idUsers = 0;
    public static String fullname = "";
    public static String email = "";
    /** admin | patient | psychologue | responsable_centre */
    public static String role = "";

    private Session() {}

    public static void clear() {
        idUsers = 0;
        fullname = "";
        email = "";
        role = "";
    }
}

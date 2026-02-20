package utils;
//Stocker les informations de l’utilisateur connecté
public class Session {
    // static pour dire Ces variables appartiennent à la classe
    public enum Role { PATIENT, PSYCHOLOGUE }

    private static int userId;
    private static Role role;
    private static String fullName;

    //Méthode login()
    //Quand l’utilisateur se connecte :
    //On stocke :
    //son id
    //son rôle
    //son nom
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

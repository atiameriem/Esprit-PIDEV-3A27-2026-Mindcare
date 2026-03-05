package utils.forum;

import models.User;
import services.UserService;
import utils.UserSession;

/**
 * Règles d'autorisation (admin, propriétaire, etc.).
 * Utilise UserSession officielle — ne dépend plus de l'ancienne Session statique.
 */
public final class Permissions {

    private Permissions() {}

    private static final UserService userService = new UserService();

    // Cache simple pour éviter une requête DB à chaque carte.
    private static Boolean cachedIsAdmin = null;
    private static int cachedUserId = -1;

    // ---- Helpers session ------------------------------------------------

    public static int currentUserId() {
        User u = getSessionUser();
        return u != null ? u.getId() : 0;
    }

    public static String currentEmail() {
        User u = getSessionUser();
        return u != null && u.getEmail() != null ? u.getEmail().trim() : "";
    }

    public static String currentRole() {
        User u = getSessionUser();
        if (u == null || u.getRole() == null) return "";
        return u.getRole().name();
    }

    public static String currentFullname() {
        User u = getSessionUser();
        return u != null ? u.getFullname() : "";
    }

    private static User getSessionUser() {
        return UserSession.getInstance().getUser();
    }

    // ---- Vérification admin ---------------------------------------------

    public static boolean isAdmin() {
        int sid = currentUserId();
        String srole = currentRole();

        boolean cacheOk = cachedIsAdmin != null && cachedUserId == sid;
        if (cacheOk) return cachedIsAdmin;

        boolean admin = isAdminRole(srole);

        // Fallback DB si rôle session invalide
        if (!admin && sid > 0) {
            String dbRole = userService.fetchRoleFromDb(sid, currentEmail());
            if (dbRole != null && !dbRole.isBlank()) {
                admin = isAdminRole(dbRole.trim());
            }
        }

        cachedUserId = sid;
        cachedIsAdmin = admin;
        return admin;
    }

    public static boolean isAdminRole(String role) {
        if (role == null) return false;
        String r = role.trim();
        if (r.isEmpty()) return false;
        return r.equalsIgnoreCase("Admin")
                || r.equalsIgnoreCase("admin")
                || r.equalsIgnoreCase("ROLE_ADMIN")
                || r.toLowerCase().contains("admin");
    }

    public static boolean isOwner(int ownerId) {
        return ownerId == currentUserId();
    }

    /** Admin ou propriétaire => peut modifier */
    public static boolean canEdit(int ownerId) {
        return isAdmin() || isOwner(ownerId);
    }

    /** Admin ou propriétaire => peut supprimer */
    public static boolean canDelete(int ownerId) {
        return isAdmin() || isOwner(ownerId);
    }

    /** Admin ne peut pas signaler. Les non-propriétaires peuvent signaler. */
    public static boolean canReport(int ownerId) {
        return !isAdmin() && !isOwner(ownerId);
    }

    /** Invalide le cache (à appeler après changement de session). */
    public static void invalidateCache() {
        cachedIsAdmin = null;
        cachedUserId = -1;
    }
}

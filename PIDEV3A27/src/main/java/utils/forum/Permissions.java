package utils.forum;

import services.UserService;
import utils.Session;

/**
 * Règles d'autorisation (admin, propriétaire, etc.).
 * Le controller ne doit pas contenir de logique métier.
 */
public final class Permissions {

    private Permissions() {}

    private static final UserService userService = new UserService();

    // Cache simple pour éviter de retaper la DB à chaque carte.
    private static Boolean cachedIsAdmin = null;
    private static int cachedUserId = -1;
    private static String cachedEmail = null;
    private static String cachedRole = null;

    public static boolean isAdmin() {
        int sid = Session.idUsers;
        String semail = (Session.email == null) ? "" : Session.email.trim();
        String srole = (Session.role == null) ? "" : Session.role.trim();

        boolean cacheOk = cachedIsAdmin != null
                && cachedUserId == sid
                && sameIgnoreCaseOrEmpty(cachedEmail, semail)
                && sameIgnoreCaseOrEmpty(cachedRole, srole);

        if (cacheOk) return cachedIsAdmin;

        boolean admin = isAdminRole(srole);

        // Fallback DB si Session.role est vide/incorrect
        if (!admin) {
            String dbRole = userService.fetchRoleFromDb(sid, semail);
            if (dbRole != null && !dbRole.isBlank()) {
                Session.role = dbRole.trim();
                admin = isAdminRole(Session.role);
            }
        }

        cachedUserId = sid;
        cachedEmail = semail.isEmpty() ? null : semail;
        cachedRole = srole.isEmpty() ? null : srole;
        cachedIsAdmin = admin;
        return admin;
    }

    public static boolean isAdminRole(String role) {
        if (role == null) return false;
        String r = role.trim();
        if (r.isEmpty()) return false;
        return r.equalsIgnoreCase("admin")
                || r.equalsIgnoreCase("ROLE_ADMIN")
                || r.toLowerCase().contains("admin");
    }

    public static boolean isOwner(int ownerId) {
        return ownerId == Session.idUsers;
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

    private static boolean sameIgnoreCaseOrEmpty(String cached, String current) {
        if (cached == null) return current == null || current.isEmpty();
        if (current == null) return cached.isEmpty();
        return cached.equalsIgnoreCase(current);
    }
}

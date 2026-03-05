package utils;

import models.Users;

/**
 * Session globale — 4 rôles, 2 interfaces :
 *
 *   USER + RESPONSABLE_CENTRE  →  /MindCareLayout.fxml
 *   ADMIN + PSYCHOLOGUE        →  /MindCareLayoutPsy.fxml
 */
public class Session {

    // ── Enum des 4 rôles ──────────────────────────────────────────
    public enum Role {
        USER,
        RESPONSABLEC,
        ADMIN,
        PSYCHOLOGUE
    }

    // ── État statique ─────────────────────────────────────────────
    private static int    idConnecte   = -1;
    private static Role   roleConnecte = null;
    private static String nomConnecte  = null;
    private static Users  utilisateur  = null;

    // ══════════════════════════════════════════════════════════════
    //  CONNEXION / DÉCONNEXION
    // ══════════════════════════════════════════════════════════════

    public static void login(int id, Role role, String nomComplet) {
        idConnecte   = id;
        roleConnecte = role;
        nomConnecte  = nomComplet != null ? nomComplet.trim() : "";

        utilisateur = new Users();
        utilisateur.setIdUsers(id);
        utilisateur.setRole(role.name());

        if (nomConnecte.contains(" ")) {
            String[] parts = nomConnecte.split(" ", 2);
            utilisateur.setPrenom(parts[0]);
            utilisateur.setNom(parts[1]);
        } else {
            utilisateur.setPrenom(nomConnecte);
            utilisateur.setNom("");
        }
    }

    public static void logout() {
        idConnecte   = -1;
        roleConnecte = null;
        nomConnecte  = null;
        utilisateur  = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTERS PRINCIPAUX
    // ══════════════════════════════════════════════════════════════

    public static int     getIdConnecte()         { return idConnecte;   }
    public static Role    getRoleConnecte()        { return roleConnecte; }
    public static String  getNomConnecte()         { return nomConnecte;  }
    public static Users   getUtilisateurConnecte() { return utilisateur;  }
    public static boolean isConnecte()             { return idConnecte != -1; }

    // ══════════════════════════════════════════════════════════════
    //  ALIAS — compatibilité avec tous les contrôleurs
    // ══════════════════════════════════════════════════════════════

    /** Alias de getIdConnecte() */
    public static int getUserId() {
        return idConnecte;
    }

    /** Alias de getNomConnecte() — nom complet "Prénom Nom" */
    public static String getFullName() {
        return nomConnecte != null ? nomConnecte : "";
    }

    /**
     * Nom de famille seul.
     * Utilisé dans AvatarPersonnalisationController et autres contrôleurs.
     * Ex : "Alice Dupont" → "Dupont"
     */
    public static String getNom() {
        if (utilisateur != null && utilisateur.getNom() != null
                && !utilisateur.getNom().isBlank()) {
            return utilisateur.getNom();
        }
        // Fallback : extrait la partie après le premier espace
        if (nomConnecte != null && nomConnecte.contains(" ")) {
            return nomConnecte.split(" ", 2)[1];
        }
        return nomConnecte != null ? nomConnecte : "";
    }

    /**
     * Prénom seul.
     * Ex : "Alice Dupont" → "Alice"
     */
    public static String getPrenom() {
        if (utilisateur != null && utilisateur.getPrenom() != null
                && !utilisateur.getPrenom().isBlank()) {
            return utilisateur.getPrenom();
        }
        // Fallback : extrait la partie avant le premier espace
        if (nomConnecte != null && nomConnecte.contains(" ")) {
            return nomConnecte.split(" ", 2)[0];
        }
        return nomConnecte != null ? nomConnecte : "";
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS DE RÔLE — un helper par rôle + helpers groupés
    // ══════════════════════════════════════════════════════════════

    /** true si rôle = USER */
    public static boolean isUser() {
        return roleConnecte == Role.USER;
    }

    /**
     * true si rôle = USER ou RESPONSABLE_CENTRE.
     * Utilisé dans MindCareLayoutController (ex : isPatient()).
     */
    public static boolean isPatient() {
        return roleConnecte == Role.USER
                || roleConnecte == Role.RESPONSABLEC;
    }

    /** true si rôle = RESPONSABLE_CENTRE */
    public static boolean isResponsableCentre() {
        return roleConnecte == Role.RESPONSABLEC;
    }

    /** true si rôle = ADMIN */
    public static boolean isAdmin() {
        return roleConnecte == Role.ADMIN;
    }

    /** true si rôle = PSYCHOLOGUE */
    public static boolean isPsychologue() {
        return roleConnecte == Role.PSYCHOLOGUE;
    }

    /** true si USER ou RESPONSABLE_CENTRE → interface /MindCareLayout.fxml */
    public static boolean isInterfaceUtilisateur() {
        return roleConnecte == Role.USER
                || roleConnecte == Role.RESPONSABLEC;
    }

    /** true si ADMIN ou PSYCHOLOGUE → interface /MindCareLayoutPsy.fxml */
    public static boolean isInterfaceAdmin() {
        return roleConnecte == Role.ADMIN
                || roleConnecte == Role.PSYCHOLOGUE;
    }
}
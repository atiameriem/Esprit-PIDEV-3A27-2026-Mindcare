package services;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * SERVICE RAG (Retrieval-Augmented Generation)
 * ═══════════════════════════════════════════════════════════════
 *
 * Principe du RAG appliqué à MindCare :
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. RETRIEVE  → Chercher dans la BDD les comptes rendus     │
 * │                 les plus pertinents par rapport au texte     │
 * │                 saisi par le psychologue (matching textuel)  │
 * │                                                             │
 * │  2. AUGMENT   → Construire un contexte enrichi combinant    │
 * │                 les CR récupérés + le nouveau texte          │
 * │                                                             │
 * │  3. GENERATE  → Envoyer ce contexte enrichi au LLM (Groq)   │
 * │                 pour générer un résumé guidé par             │
 * │                 l'historique réel des séances                │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Stratégie de recherche (sans embeddings vectoriels) :
 * - Matching par mots-clés sur resumeseancecr et prochainesActioncr
 * - Score de pertinence calculé côté SQL (MATCH/LIKE)
 * - Les 3 CR les plus proches sont injectés dans le prompt
 */
public class RagService {

    private final Connection cnx;

    // On prend 3 comptes rendus similaires maximum (Top K).
    private static final int TOP_K = 3;

    // Longueur max d'un extrait de CR dans le contexte (caractères)
    private static final int MAX_EXCERPT_LENGTH = 400;

    //Constructeur par défaut : récupère la connexion depuis MyDatabase.
    public RagService() {
        this.cnx = MyDatabase.getInstance().getConnection();
    }

    //on injecte une connexion
    public RagService(Connection cnx) {
        this.cnx = cnx;
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE : Récupère le contexte RAG
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère les comptes rendus similaires depuis la BDD
     * et construit un bloc de contexte à injecter dans le prompt LLM.
     *
     * @param resumeTexte   Texte du résumé saisi par le psychologue
     * @param actionsTexte  Texte des prochaines actions saisies
     * @param idPsychologiste ID du psychologue connecté (filtrage optionnel)
     * @return Bloc de contexte formaté pour injection dans le prompt
     */
    public String buildRagContext(String resumeTexte, String actionsTexte, int idPsychologiste) {
        try {
            //Appelle la partie Retrieve :
            // cherche en DB les comptes rendus similaires.
            List<CompteRenduExtrait> similaires = rechercherSimilaires(resumeTexte, actionsTexte, idPsychologiste);
    //Si rien trouvé le LLM générera sans contexte.
            if (similaires.isEmpty()) {
                return ""; // Pas de contexte disponible, le LLM génère sans référence
            }

            return formaterContexte(similaires);

        } catch (SQLException e) {
            System.err.println("[RagService] Erreur récupération contexte : " + e.getMessage());
            return ""; // En cas d'erreur, continuer sans contexte RAG
        }
    }

    /**

     * (utilise tous les CR disponibles comme sans id psy pour voir tous les contenus des cp
     */
    public String buildRagContext(String resumeTexte, String actionsTexte) {
        return buildRagContext(resumeTexte, actionsTexte, -1);
    }

    // ═══════════════════════════════════════════════════════════
    // RETRIEVE : Recherche par pertinence textuelle
    // ═══════════════════════════════════════════════════════════


    //Méthode privée qui retourne une liste d’extraits similaires.
    private List<CompteRenduExtrait> rechercherSimilaires(
            String resumeTexte, String actionsTexte, int idPsychologiste) throws SQLException {

        // Transforme le texte saisi en mots-clés
        List<String> motsCles = extraireMoysCles(resumeTexte + " " + actionsTexte);

        if (motsCles.isEmpty()) {
            // Pas de mots-clés : retourner les CR les plus récents comme contexte général
            return rechercherRecents(idPsychologiste);
        }

        // scoreExpr va devenir une formule SQL qui calcule un score.
        //conditions va stocker les paramètres LIKE (%mot%).
        StringBuilder scoreExpr = new StringBuilder("(");
        List<String> conditions = new ArrayList<>();

        //Pour chaque mot-clé :
        //on ajoute 1 point si le mot est trouvé dans resumeseancecr
        //+1 point si trouvé dans prochainesactioncr
        //Et on ajoute 2 paramètres
        for (String mot : motsCles) {
            scoreExpr.append("(CASE WHEN cr.resumeseancecr LIKE ? THEN 1 ELSE 0 END) + ")
                    .append("(CASE WHEN cr.prochainesactioncr LIKE ? THEN 1 ELSE 0 END) + ");
            conditions.add("%" + mot + "%");
            conditions.add("%" + mot + "%");
        }

        // Enlever le dernier " + " et fermer la parenthèse
        String scoreStr = scoreExpr.substring(0, scoreExpr.length() - 3) + ")";

        // Filtrage par psychologue si demandé
        //Si idPsychologiste > 0 → ajoute une condition SQL.
        //Sinon → pas de filtre.
        String filtrepsy = (idPsychologiste > 0)
                ? "AND rv.id_psychologist = " + idPsychologiste
                : "";

        //On sélectionne les champs + score.
        //On garde seulement ceux dont score > 0
        //Tri :
        //meilleur score
        //plus récent
        //Limite : TOP_K (3).
        String sql = "SELECT cr.id_compterendu, cr.progrescr, "
                + "cr.resumeseancecr, cr.prochainesactioncr, cr.date_creationcr, "
                + scoreStr + " AS score "
                + "FROM compte_rendu_seance cr "
                + "JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment "
                + "WHERE " + scoreStr + " > 0 "
                + filtrepsy + " "
                + "ORDER BY score DESC, cr.date_creationcr DESC "
                + "LIMIT " + TOP_K;

        // Les paramètres LIKE sont utilisés deux fois : dans SELECT et dans WHERE
        List<String> allParams = new ArrayList<>();
        allParams.addAll(conditions); // pour le SELECT score
        allParams.addAll(conditions); // pour le WHERE score > 0

        //Prépare la requête.
        //Remplit tous les ?.
        //Exécute et convertit le résultat en liste d’objets.
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            for (int i = 0; i < allParams.size(); i++) {
                pst.setString(i + 1, allParams.get(i));
            }
            try (ResultSet rs = pst.executeQuery()) {
                return mapResultSet(rs);
            }
        }
    }

    /** Fallback : retourne les CR les plus récents si pas de mots-clés */
    private List<CompteRenduExtrait> rechercherRecents(int idPsychologiste) throws SQLException {
        String filtrepsy = (idPsychologiste > 0)
                ? "WHERE rv.id_psychologist = " + idPsychologiste
                : "";

        String sql = "SELECT cr.id_compterendu, cr.progrescr, "
                + "cr.resumeseancecr, cr.prochainesactioncr, cr.date_creationcr "
                + "FROM compte_rendu_seance cr "
                + "JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment "
                + filtrepsy + " "
                + "ORDER BY cr.date_creationcr DESC "
                + "LIMIT " + TOP_K;

        try (PreparedStatement pst = cnx.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            return mapResultSet(rs);
        }
    }
//Convertit les lignes SQL en objets Java.
    private List<CompteRenduExtrait> mapResultSet(ResultSet rs) throws SQLException {
        List<CompteRenduExtrait> liste = new ArrayList<>();
        while (rs.next()) {
            CompteRenduExtrait cr = new CompteRenduExtrait();
            cr.id         = rs.getInt("id_compterendu");
            cr.progres    = rs.getString("progrescr");
            cr.resume     = tronquer(rs.getString("resumeseancecr"));
            cr.actions    = tronquer(rs.getString("prochainesactioncr"));
            cr.date       = rs.getString("date_creationcr");
            liste.add(cr);
        }
        return liste;
    }

    // ═══════════════════════════════════════════════════════════
    // AUGMENT : Formatage du contexte pour le prompt
    // ═══════════════════════════════════════════════════════════

    /**
     * Formate les CR récupérés en un bloc de contexte clair
     * que le LLM peut exploiter pour guider sa génération.
     */
    private String formaterContexte(List<CompteRenduExtrait> crs) {
        StringBuilder sb = new StringBuilder();
        sb.append("CONTEXTE HISTORIQUE (exemples de comptes rendus similaires issus de la base de données) :\n");
        sb.append("─────────────────────────────────────────────────────\n");

        for (int i = 0; i < crs.size(); i++) {
            CompteRenduExtrait cr = crs.get(i);
            //Ajoute progression + résumé + actions.
            //sb = StringBuilder
            //append() = ajouter du texte à la fin
            sb.append("Exemple ").append(i + 1).append(" :\n");
            sb.append("  Progression : ").append(formaterProgres(cr.progres)).append("\n");
            sb.append("  Resume seance : ").append(cr.resume).append("\n");
            sb.append("  Actions suivantes : ").append(cr.actions).append("\n");
            sb.append("─────────────────────────────────────────────────────\n");
        }
        //Instruction importante pour éviter que le LLM copie-colle.
        sb.append("Utilise ces exemples comme référence de style et de structure médicale.\n");
        sb.append("Ne copie pas ces exemples : génère un résumé original basé sur la nouvelle séance.\n");

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    /**
     * Extrait les mots-clés significatifs (longueur > 4, sans mots vides)
     */
    private List<String> extraireMoysCles(String texte) {
        if (texte == null || texte.isBlank()) return new ArrayList<>();

        // Liste de mots à ignorer (stop words).
        //puisquil sont pas des mots scientifiques
        java.util.Set<String> motsVides = java.util.Set.of(
                "avec", "dans", "pour", "mais", "donc", "lors", "cette",
                "plus", "tout", "bien", "aussi", "tres", "comme", "elle",
                "sera", "sont", "nous", "vous", "leur", "une", "des", "les",
                "aux", "par", "sur", "que", "qui", "est", "pas",
                "avoir", "etre", "faire", "patient", "seance", "rendu"
        );

        List<String> mots = new ArrayList<>();
        // Normaliser : supprimer accents, mettre en minuscules, séparer par espaces/ponctuation
        String normalise = texte.toLowerCase()
                .replaceAll("[àáâã]", "a").replaceAll("[éèêë]", "e")
                .replaceAll("[îï]", "i").replaceAll("[ôõ]", "o")
                .replaceAll("[ùûü]", "u").replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s]", " ");

        //Garde mots > 4 lettres et pas dans mots vides.
        //Max 6 mots-clés.
        for (String mot : normalise.split("\\s+")) {
            if (mot.length() > 4 && !motsVides.contains(mot)) {
                mots.add(mot);
                if (mots.size() >= 6) break; // Limiter à 6 mots-clés max
            }
        }
        return mots;
    }

    private String formaterProgres(String progres) {
        if (progres == null) return "non defini";
        //Transforme une valeur enum/DB en texte lisible.
        return switch (progres) {
            case "amelioration_significative" -> "Amelioration significative";
            case "amelioration_legere"        -> "Amelioration legere";
            case "amelioration_stable"        -> "Amelioration stable";
            case "stagnation"                 -> "Stagnation";
            default                           -> progres;
        };
    }
//Coupe à 400 caractères max.
    private String tronquer(String texte) {
        if (texte == null) return "";
        return texte.length() > MAX_EXCERPT_LENGTH
                ? texte.substring(0, MAX_EXCERPT_LENGTH) + "..."
                : texte;
    }

    // ═══════════════════════════════════════════════════════════
    // MODÈLE INTERNE (DTO)
    // ═══════════════════════════════════════════════════════════

    /** Représente un extrait de compte rendu récupéré pour le contexte RAG */
    //Petit objet interne juste pour transporter des données.
    //Pas une entity complète, juste un “extrait”.
    private static class CompteRenduExtrait {
        int    id;
        String progres;
        String resume;
        String actions;
        String date;
    }
}

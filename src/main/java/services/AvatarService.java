package services;

import utils.MyDatabase;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

/**
 * ══════════════════════════════════════════════════════════════
 *  AvatarService v5 — COMPORTEMENT ÉMOTIONNEL RÉACTIF
 *
 *  Comportement avatar selon état détecté :
 *  ┌──────────────────┬──────────────────────┬──────────────────────┐
 *  │ État détecté     │ Réaction avatar       │ Animation            │
 *  ├──────────────────┼──────────────────────┼──────────────────────┤
 *  │ 😤 Colère        │ Ton calme            │ SECOUSSE             │
 *  │ 💜 Stress élevé  │ Voix douce           │ RESPIRATION          │
 *  │ 😢 Tristesse     │ Regard empathique    │ FONDU_DOUX           │
 *  │ 🌟 Score élevé  │ Ton énergique        │ BOUNCE_JOIE          │
 *  │ 💪 Amélioration │ Félicitations animées│ PULSE                │
 *  │ 😌 Équilibre     │ Stable et posé       │ STANDARD             │
 *  │ 👋 Neutre        │ Bienvenue            │ AUCUNE               │
 *  └──────────────────┴──────────────────────┴──────────────────────┘
 *
 *  Paramètres DiceBear avataaars utilisés :
 *  ┌──────────────────┬──────────────┬──────────────┬───────────────────────┐
 *  │ État détecté     │ mouth=       │ eyes=        │ eyebrows=             │
 *  ├──────────────────┼──────────────┼──────────────┼───────────────────────┤
 *  │ 😰 Stress élevé │ concerned    │ squint       │ sadConcerned          │
 *  │ 😢 Tristesse     │ sad          │ cry          │ sadConcernedNatural   │
 *  │ 😤 Colère        │ grimace      │ xDizzy       │ angry                 │
 *  │ 🌟 Score élevé  │ twinkle      │ hearts       │ raisedExcited         │
 *  │ 💪 Amélioration │ smile        │ happy        │ raisedExcitedNatural  │
 *  │ 😌 Équilibre     │ serious      │ side         │ defaultNatural        │
 *  │ 😐 Neutre        │ default      │ default      │ default               │
 *  └──────────────────┴──────────────┴──────────────┴───────────────────────┘
 *
 *  RÈGLE SEED :
 *   • getAvatarUrl()           → seed fixe  → image stable (cache DB)
 *   • getAvatarUrlEmotionnel() → seed fixe + mouth/eyes/eyebrows
 *     Le PERSONNAGE ne change pas, seule l'expression change.
 *
 *  ⚠️  Seul le style "avataaars" supporte mouth/eyes/eyebrows.
 *      Les autres styles utilisent un seed suffixé par état (fallback).
 * ══════════════════════════════════════════════════════════════
 */
public class AvatarService {

    private static final String DICEBEAR_BASE = "https://api.dicebear.com/9.x/";
    private static final String PREFS_DIR =
            System.getProperty("user.home") + "/.mindcare/avatars/";

    // ── Seuils de détection (centralisés pour faciliter la maintenance) ──
    private static final int SEUIL_COLERE          = 20;  // scoreST < 20 → colère
    private static final int SEUIL_STRESS          = 30;  // scoreST < 30 → stress
    private static final int SEUIL_TRISTESSE       = 30;  // scoreBE < 30 && scoreHU < 30 → tristesse
    private static final int SEUIL_SCORE_ELEVE     = 80;  // moy >= 80    → épanoui
    private static final int SEUIL_AMELIORATION    = 55;  // moy >= 55    → amélioration
    private static final int SEUIL_EQUILIBRE       = 35;  // moy >= 35    → équilibre

    // ══════════════════════════════════════════════════════════════
    // ÉTAT ÉMOTIONNEL DÉTECTÉ
    // ══════════════════════════════════════════════════════════════
    public enum EtatDetecte {
        EN_COLERE,      // stress critique  : scoreST < SEUIL_COLERE
        STRESS_ELEVE,   // stress élevé     : scoreST < SEUIL_STRESS
        TRISTE,         // tristesse        : scoreBE < SEUIL_TRISTESSE ET scoreHU < SEUIL_TRISTESSE
        SCORE_ELEVE,    // épanoui          : moy >= SEUIL_SCORE_ELEVE
        AMELIORATION,   // progression      : moy >= SEUIL_AMELIORATION
        EQUILIBRE,      // stable           : moy >= SEUIL_EQUILIBRE
        NEUTRE          // par défaut
    }

    /**
     * Détecte l'état dominant à partir des 3 scores (0-100).
     *
     * Ordre de priorité (du plus urgent au plus positif) :
     *  1. Colère      → stress critique avant tout
     *  2. Stress      → stress élevé
     *  3. Tristesse   → combinaison bien-être + humeur bas
     *  4. Score élevé → épanouissement
     *  5. Amélioration→ progression
     *  6. Équilibre   → stabilité
     *  7. Neutre      → fallback
     */
    public static EtatDetecte detecterEtat(int scoreBE, int scoreST, int scoreHU) {
        int moy = (scoreBE + scoreST + scoreHU) / 3;

        // ── Priorité 1 : états négatifs (du plus critique au moins critique) ──
        if (scoreST < SEUIL_COLERE)                                  return EtatDetecte.EN_COLERE;
        if (scoreST < SEUIL_STRESS)                                  return EtatDetecte.STRESS_ELEVE;
        if (scoreBE < SEUIL_TRISTESSE && scoreHU < SEUIL_TRISTESSE)  return EtatDetecte.TRISTE;

        // ── Priorité 2 : états positifs (du plus haut au plus bas) ──
        if (moy >= SEUIL_SCORE_ELEVE)   return EtatDetecte.SCORE_ELEVE;
        if (moy >= SEUIL_AMELIORATION)  return EtatDetecte.AMELIORATION;
        if (moy >= SEUIL_EQUILIBRE)     return EtatDetecte.EQUILIBRE;

        return EtatDetecte.NEUTRE;
    }

    // ══════════════════════════════════════════════════════════════
    // EXPRESSION — paramètres DiceBear + réaction UI
    // ══════════════════════════════════════════════════════════════
    public static class Expression {
        // Paramètres URL DiceBear avataaars
        public final String mouth;
        public final String eyes;
        public final String eyebrows;
        public final String fondHex;

        // Réaction UI (message + emoji + animation)
        public final String titreReaction;
        public final String messageReaction;
        public final String emoji;
        public final String couleurBandeau;
        public final String couleurBandeauBg;
        public final AnimationType animation;

        public Expression(String mouth, String eyes, String eyebrows,
                          String fondHex, String titreReaction,
                          String messageReaction, String emoji,
                          String couleurBandeau, String couleurBandeauBg,
                          AnimationType animation) {
            this.mouth            = mouth;
            this.eyes             = eyes;
            this.eyebrows         = eyebrows;
            this.fondHex          = fondHex;
            this.titreReaction    = titreReaction;
            this.messageReaction  = messageReaction;
            this.emoji            = emoji;
            this.couleurBandeau   = couleurBandeau;
            this.couleurBandeauBg = couleurBandeauBg;
            this.animation        = animation;
        }
    }

    /** Type d'animation JavaFX à jouer sur l'ImageView avatar */
    public enum AnimationType {
        RESPIRATION,    // ScaleTransition lente 0.95↔1.05 ∞ — stress élevé
        FONDU_DOUX,     // FadeTransition lente 0.65→1.0 ∞  — tristesse
        SECOUSSE,       // TranslateTransition courte → puis apaisement — colère
        BOUNCE_JOIE,    // ScaleTransition + RotateTransition — score élevé
        PULSE,          // ScaleTransition 1.0→1.12→1.0 (4 cycles) — amélioration
        STANDARD,       // FadeTransition 500ms — équilibre
        AUCUNE          // pas d'animation — neutre
    }

    /**
     * Retourne l'expression complète selon l'état détecté.
     *
     * Tableau comportement :
     *   EN_COLERE    → Ton calme        (grimace / xDizzy / angry)      → SECOUSSE
     *   STRESS_ELEVE → Voix douce       (concerned / squint / sadC.)    → RESPIRATION
     *   TRISTE       → Regard empathique(sad / cry / sadConcernedNat.)  → FONDU_DOUX
     *   SCORE_ELEVE  → Ton énergique    (twinkle / hearts / raisedExc.) → BOUNCE_JOIE
     *   AMELIORATION → Félicitations    (smile / happy / raisedExcNat.) → PULSE
     *   EQUILIBRE    → Stable et posé   (serious / side / defaultNat.)  → STANDARD
     *   NEUTRE       → Bienvenue        (default / default / default)   → AUCUNE
     */
    public static Expression getExpression(EtatDetecte etat) {
        switch (etat) {

            case EN_COLERE:
                // Ton calme — bouche crispée, yeux tournoyants → invitation à l'apaisement
                return new Expression(
                        "grimace", "xDizzy", "angry",
                        "ffd5b8",
                        "😤 Moment difficile",
                        "Respirez lentement.\nVotre avatar vous invite au calme. 🌬️",
                        "🌬️", "#c2410c", "#fff7ed",
                        AnimationType.SECOUSSE
                );

            case STRESS_ELEVE:
                // Voix douce — bouche inquiète, yeux plissés (fatigue/anxiété)
                return new Expression(
                        "concerned", "squint", "sadConcerned",
                        "e8d5f4",
                        "💜 Voix douce",
                        "Votre avatar vous envoie de la douceur.\nPrenez soin de vous. 💜",
                        "💜", "#7c3aed", "#ede9fe",
                        AnimationType.RESPIRATION
                );

            case TRISTE:
                // Regard empathique — avatar qui partage et comprend la tristesse
                return new Expression(
                        "sad", "cry", "sadConcernedNatural",
                        "ffd5dc",
                        "🤗 Regard empathique",
                        "Votre avatar vous comprend.\nCe moment passera, tenez bon. 🌧️",
                        "🤗", "#be185d", "#fce7f3",
                        AnimationType.FONDU_DOUX
                );

            case SCORE_ELEVE:
                // Ton énergique — yeux cœurs, sourire rayonnant, célébration
                return new Expression(
                        "twinkle", "hearts", "raisedExcited",
                        "d1f4e0",
                        "🌟 Ton énergique !",
                        "Vous êtes au sommet !\nVotre avatar est fou de joie pour vous. 🎉",
                        "🌟", "#065f46", "#d1fae5",
                        AnimationType.BOUNCE_JOIE
                );

            case AMELIORATION:
                // Félicitations animées — grand sourire, yeux heureux, progression visible
                return new Expression(
                        "smile", "happy", "raisedExcitedNatural",
                        "b6e3f4",
                        "💪 Félicitations !",
                        "Quelle belle progression !\nContinuez sur cette lancée. ✨",
                        "🎉", "#0369a1", "#e0f2fe",
                        AnimationType.PULSE
                );

            case EQUILIBRE:
                // Stable et posé — expression réfléchie et sereine
                return new Expression(
                        "serious", "side", "defaultNatural",
                        "fef3c7",
                        "😌 En équilibre",
                        "Vous êtes stable et posé(e).\nVotre avatar veille sur vous. 🌤️",
                        "😌", "#92400e", "#fffbeb",
                        AnimationType.STANDARD
                );

            default: // NEUTRE
                return new Expression(
                        "default", "default", "default",
                        "f1f5f9",
                        "👋 Bienvenue",
                        "Votre avatar vous accompagne au quotidien. 👋",
                        "👋", "#334155", "#f1f5f9",
                        AnimationType.AUCUNE
                );
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Styles disponibles
    // ══════════════════════════════════════════════════════════════
    public enum Style {
        // ✅ Seul avataaars supporte les vraies grimaces (mouth/eyes/eyebrows)
        AVATAAARS ("avataaars",  "😊 Personnage (avec expressions 🎭)"),
        // ℹ️  Les styles suivants changent de fond/seed selon l'état, pas de visage
        ADVENTURER("adventurer", "🧗 Aventurier"),
        BOTTTS    ("bottts",     "🤖 Robot"),
        LORELEI   ("lorelei",    "🌸 Lorelei"),
        MICAH     ("micah",      "🎨 Micah"),
        PERSONAS  ("personas",   "👤 Persona"),
        PIXEL_ART ("pixel-art",  "🎮 Pixel Art"),
        FUN_EMOJI ("fun-emoji",  "😄 Fun Emoji");

        public final String apiId;
        public final String label;

        Style(String apiId, String label) {
            this.apiId = apiId;
            this.label = label;
        }

        public static Style fromApiId(String apiId) {
            for (Style s : values())
                if (s.apiId.equalsIgnoreCase(apiId)) return s;
            return AVATAAARS;
        }

        /**
         * Seul avataaars supporte les paramètres mouth= eyes= eyebrows=.
         * Les autres styles utilisent un seed suffixé par état (fallback).
         * → Pour de vraies grimaces, l'utilisateur doit choisir AVATAAARS.
         */
        public boolean supporteExpression() {
            return this == AVATAAARS;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Couleurs de fond
    // ══════════════════════════════════════════════════════════════
    public enum CouleurFond {
        BLEU   ("b6e3f4", "💙 Bleu"),
        ROSE   ("ffd5dc", "🌸 Rose"),
        VERT   ("d1f4e0", "💚 Vert"),
        LAVANDE("e8d5f4", "💜 Lavande"),
        PECHE  ("ffd5b8", "🍑 Pêche"),
        JAUNE  ("fef3c7", "💛 Jaune"),
        GRIS   ("d1d5db", "⚪ Gris"),
        BLANC  ("ffffff", "🤍 Blanc");

        public final String hex;
        public final String label;

        CouleurFond(String hex, String label) {
            this.hex   = hex;
            this.label = label;
        }

        public static CouleurFond fromHex(String hex) {
            if (hex == null) return BLEU;
            String h = hex.replace("#", "").toLowerCase();
            for (CouleurFond c : values())
                if (c.hex.equalsIgnoreCase(h)) return c;
            return BLEU;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Préférences avatar
    // ══════════════════════════════════════════════════════════════
    public static class PrefsAvatar {
        public int         userId;
        public String      seed;
        public Style       style;
        public CouleurFond fond;
        public String      pseudo;
        public int         taille;
        public String      avatarBase64;

        public PrefsAvatar(int userId) {
            this.userId       = userId;
            this.seed         = "mc_" + userId;
            this.style        = Style.AVATAAARS;
            this.fond         = CouleurFond.BLEU;
            this.pseudo       = "";
            this.taille       = 200;
            this.avatarBase64 = null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // URLS DICEBEAR
    // ══════════════════════════════════════════════════════════════

    /**
     * URL STABLE — seed fixe, pas d'expression.
     * Utilisée pour le cache DB et l'affichage par défaut.
     */
    public String getAvatarUrl(PrefsAvatar prefs) {
        return DICEBEAR_BASE
                + prefs.style.apiId
                + "/png?seed="        + encode(prefs.seed)
                + "&size="            + prefs.taille
                + "&backgroundColor=" + prefs.fond.hex
                + "&radius=50";
    }

    /**
     * URL ÉMOTIONNELLE — même seed + paramètres d'expression.
     *
     * Pour avataaars : ajoute &mouth= &eyes= &eyebrows=
     *   → le PERSONNAGE est identique, seul le visage change.
     *
     * Pour autres styles : seed suffixé par code état (3 lettres)
     *   → variation légère du même personnage (expression simulée).
     */
    public String getAvatarUrlEmotionnel(PrefsAvatar prefs,
                                         int scoreBE, int scoreST, int scoreHU) {
        EtatDetecte etat = detecterEtat(scoreBE, scoreST, scoreHU);
        Expression  expr = getExpression(etat);

        if (prefs.style.supporteExpression()) {
            // ✅ avataaars — vrais paramètres visage DiceBear
            return DICEBEAR_BASE
                    + prefs.style.apiId
                    + "/png?seed="        + encode(prefs.seed)
                    + "&size="            + prefs.taille
                    + "&backgroundColor=" + expr.fondHex
                    + "&radius=50"
                    + "&mouth="           + expr.mouth
                    + "&eyes="            + expr.eyes
                    + "&eyebrows="        + expr.eyebrows;
        } else {
            // Fallback : seed modifié par état pour variation visuelle
            String code = etat.name().toLowerCase().substring(0, 3);
            return DICEBEAR_BASE
                    + prefs.style.apiId
                    + "/png?seed="        + encode(prefs.seed + "_" + code)
                    + "&size="            + prefs.taille
                    + "&backgroundColor=" + expr.fondHex
                    + "&radius=50";
        }
    }

    /** Retourne l'expression courante selon les scores (pour animations UI) */
    public Expression getExpressionCourante(int scoreBE, int scoreST, int scoreHU) {
        return getExpression(detecterEtat(scoreBE, scoreST, scoreHU));
    }

    /** Génère un seed unique pour le bouton "Régénérer" */
    public String genererNouveauSeed(int userId) {
        return "mc_" + userId + "_"
                + Long.toHexString(System.currentTimeMillis());
    }

    // ══════════════════════════════════════════════════════════════
    // PERSISTANCE DB
    // ══════════════════════════════════════════════════════════════
    public PrefsAvatar chargerPrefs(int userId) {
        PrefsAvatar prefs = chargerPrefsDB(userId);
        if (prefs != null) return prefs;
        return chargerPrefsLocal(userId);
    }

    public PrefsAvatar chargerPrefsDB(int userId) {
        String sql = """
            SELECT seed, style, couleur_fond, pseudo, taille
            FROM avatar_preferences WHERE user_id = ?
            """;
        // ⚠️  On ne charge PLUS avatar_png_base64 :
        //     ce cache contient l'image stable (sans expression).
        //     L'affichage passe toujours par getAvatarUrlEmotionnel()
        //     pour avoir les vraies grimaces selon l'état émotionnel.
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PrefsAvatar p  = new PrefsAvatar(userId);
                p.seed         = rs.getString("seed");
                p.style        = Style.fromApiId(rs.getString("style"));
                p.fond         = CouleurFond.fromHex(rs.getString("couleur_fond"));
                p.pseudo       = rs.getString("pseudo");
                p.taille       = rs.getInt("taille");
                p.avatarBase64 = null; // jamais de cache stable — toujours URL émotionnelle
                return p;
            }
        } catch (Exception e) {
            System.err.println("❌ chargerPrefsDB : " + e.getMessage());
        }
        return null;
    }

    public void sauvegarderPrefs(PrefsAvatar prefs) {
        sauvegarderPrefsDB(prefs);
        sauvegarderPrefsLocal(prefs);
    }

    /**
     * Sauvegarde les préférences en DB.
     *
     * ⚠️  On ne sauvegarde PLUS le base64 de l'image stable ici.
     *     Raison : l'image stable (sans expression) écrasait l'image
     *     émotionnelle dynamique à chaque rechargement.
     *     L'image est toujours reconstituée via getAvatarUrlEmotionnel()
     *     au moment de l'affichage, en fonction des scores courants.
     */
    public void sauvegarderPrefsDB(PrefsAvatar prefs) {
        String sql = """
            INSERT INTO avatar_preferences
                (user_id, seed, style, couleur_fond, pseudo, taille,
                 avatar_url, avatar_png_base64)
            VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
            ON DUPLICATE KEY UPDATE
                seed=VALUES(seed), style=VALUES(style),
                couleur_fond=VALUES(couleur_fond), pseudo=VALUES(pseudo),
                taille=VALUES(taille), avatar_url=VALUES(avatar_url),
                avatar_png_base64=NULL,
                updated_at=CURRENT_TIMESTAMP
            """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt   (1, prefs.userId);
            ps.setString(2, prefs.seed);
            ps.setString(3, prefs.style.apiId);
            ps.setString(4, prefs.fond.hex);
            ps.setString(5, prefs.pseudo);
            ps.setInt   (6, prefs.taille);
            ps.setString(7, getAvatarUrl(prefs));
            ps.executeUpdate();
            prefs.avatarBase64 = null; // jamais de cache stable en mémoire
            System.out.println("✅ Prefs sauvegardées DB userId=" + prefs.userId);
        } catch (Exception e) {
            System.err.println("❌ sauvegarderPrefsDB : " + e.getMessage());
        }
    }

    /**
     * Conservée pour compatibilité — ne fait plus rien.
     * On ne stocke plus de base64 en DB car cela écraserait
     * l'image émotionnelle dynamique lors du rechargement.
     */
    public void mettreAJourImageDB(int userId, String base64) {
        // Intentionnellement vide : voir sauvegarderPrefsDB
    }

    // ══════════════════════════════════════════════════════════════
    // IMAGE depuis base64
    // ══════════════════════════════════════════════════════════════
    public javafx.scene.image.Image getImageDepuisBase64(PrefsAvatar prefs) {
        if (prefs == null || prefs.avatarBase64 == null
                || prefs.avatarBase64.isBlank()) return null;
        try {
            byte[] b = Base64.getDecoder().decode(prefs.avatarBase64);
            return new javafx.scene.image.Image(
                    new ByteArrayInputStream(b), 200, 200, true, true);
        } catch (Exception e) {
            System.err.println("❌ base64 decode : " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TÉLÉCHARGEMENT
    // ══════════════════════════════════════════════════════════════
    public String telechargerEtEncoder(String avatarUrl) {
        try {
            URL url = new URL(avatarUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "MindCare/1.0");
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return Base64.getEncoder().encodeToString(is.readAllBytes());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ telechargerEtEncoder : " + e.getMessage());
        }
        return null;
    }

    public String telechargerAvatarLocal(String url, int userId) {
        try {
            Files.createDirectories(Paths.get(PREFS_DIR));
            String f = PREFS_DIR + "avatar_" + userId + ".png";
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "MindCare/1.0");
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    Files.copy(is, Paths.get(f), StandardCopyOption.REPLACE_EXISTING);
                }
                return f;
            }
        } catch (Exception e) {
            System.err.println("❌ telechargerAvatarLocal : " + e.getMessage());
        }
        return null;
    }

    public String getCacheLocal(int userId) {
        String f = PREFS_DIR + "avatar_" + userId + ".png";
        return Files.exists(Paths.get(f)) ? f : null;
    }

    // ══════════════════════════════════════════════════════════════
    // FALLBACK local (fichier .properties)
    // ══════════════════════════════════════════════════════════════
    public void sauvegarderPrefsLocal(PrefsAvatar prefs) {
        try {
            Files.createDirectories(Paths.get(PREFS_DIR));
            String f = PREFS_DIR + "prefs_" + prefs.userId + ".properties";
            Properties p = new Properties();
            p.setProperty("seed",   prefs.seed);
            p.setProperty("style",  prefs.style.apiId);
            p.setProperty("fond",   prefs.fond.name());
            p.setProperty("pseudo", prefs.pseudo);
            p.setProperty("taille", String.valueOf(prefs.taille));
            try (FileWriter fw = new FileWriter(f)) {
                p.store(fw, "MindCare userId=" + prefs.userId);
            }
        } catch (Exception e) {
            System.err.println("❌ sauvegarderPrefsLocal : " + e.getMessage());
        }
    }

    public PrefsAvatar chargerPrefsLocal(int userId) {
        PrefsAvatar prefs = new PrefsAvatar(userId);
        String f = PREFS_DIR + "prefs_" + userId + ".properties";
        try {
            if (!Files.exists(Paths.get(f))) return prefs;
            Properties p = new Properties();
            try (FileReader fr = new FileReader(f)) { p.load(fr); }
            prefs.seed   = p.getProperty("seed",   prefs.seed);
            prefs.pseudo = p.getProperty("pseudo", prefs.pseudo);
            try { prefs.taille = Integer.parseInt(
                    p.getProperty("taille", "200")); }
            catch (NumberFormatException ignored) {}
            try { prefs.style = Style.fromApiId(
                    p.getProperty("style", "avataaars")); }
            catch (Exception ignored) {}
            try { prefs.fond = CouleurFond.valueOf(
                    p.getProperty("fond", "BLEU")); }
            catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("❌ chargerPrefsLocal : " + e.getMessage());
        }
        return prefs;
    }

    // ══════════════════════════════════════════════════════════════
    // Compatibilité ancienne API (calculerEtat)
    // ══════════════════════════════════════════════════════════════
    public EtatEmotionnel calculerEtat(int scoreBE, int scoreST, int scoreHU) {
        Expression expr = getExpression(detecterEtat(scoreBE, scoreST, scoreHU));
        return new EtatEmotionnel(
                expr.titreReaction,
                expr.messageReaction,
                expr.couleurBandeau,
                expr.couleurBandeauBg,
                expr.emoji);
    }

    public static class EtatEmotionnel {
        public final String titre, message, couleur, couleurBg, emoji;
        public EtatEmotionnel(String t, String m, String c, String cb, String e) {
            titre = t; message = m; couleur = c; couleurBg = cb; emoji = e;
        }
    }

    // ══════════════════════════════════════════════════════════════
    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
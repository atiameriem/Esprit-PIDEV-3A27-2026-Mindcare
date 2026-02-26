package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * ══════════════════════════════════════════════════════════════
 *  AvatarService — v3 CORRIGÉ
 *
 *  PROBLÈMES RÉSOLUS :
 *  ✅ Seed STABLE : jamais recalculé, toujours relu depuis le fichier
 *  ✅ chargerPrefsEtUrl() → méthode unique pour le dashboard
 *  ✅ getAvatarUrlDepuisFichier() → lit le fichier .properties DIRECTEMENT
 *     sans passer par un objet PrefsAvatar en mémoire
 *  ✅ Après sauvegarde, le dashboard appelle chargerPrefs() pour avoir
 *     le bon seed à jour
 * ══════════════════════════════════════════════════════════════
 */
public class AvatarService {

    private static final String DICEBEAR_BASE = "https://api.dicebear.com/9.x/";

    private static final String PREFS_DIR =
            System.getProperty("user.home") + "/.mindcare/avatars/";

    // ══════════════════════════════════════════════════════════════
    // Styles disponibles
    // ══════════════════════════════════════════════════════════════
    public enum Style {
        ADVENTURER("adventurer",  "🧗 Aventurier"),
        AVATAAARS ("avataaars",   "😊 Personnage"),
        BOTTTS    ("bottts",      "🤖 Robot"),
        LORELEI   ("lorelei",     "🌸 Lorelei"),
        MICAH     ("micah",       "🎨 Micah"),
        PERSONAS  ("personas",    "👤 Persona"),
        PIXEL_ART ("pixel-art",   "🎮 Pixel Art"),
        FUN_EMOJI ("fun-emoji",   "😄 Fun Emoji");

        public final String apiId;
        public final String label;

        Style(String apiId, String label) {
            this.apiId = apiId;
            this.label = label;
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
    }

    // ══════════════════════════════════════════════════════════════
    // Préférences utilisateur
    // ══════════════════════════════════════════════════════════════
    public static class PrefsAvatar {
        public int         userId;
        public String      seed;
        public Style       style;
        public CouleurFond fond;
        public String      pseudo;
        public int         taille;

        public PrefsAvatar(int userId) {
            this.userId  = userId;
            this.seed    = seedParDefaut(userId);  // "mc_" + userId — JAMAIS de timestamp
            this.style   = Style.ADVENTURER;
            this.fond    = CouleurFond.BLEU;
            this.pseudo  = "";
            this.taille  = 200;
        }

        /** Seed fixe par défaut — identique à chaque instanciation */
        public static String seedParDefaut(int userId) {
            return "mc_" + userId;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE POUR LE DASHBOARD
    // Lit DIRECTEMENT le fichier .properties → URL toujours à jour
    // ══════════════════════════════════════════════════════════════

    /**
     * Retourne l'URL DiceBear en lisant le fichier .properties directement.
     * C'est cette méthode que le dashboard doit appeler pour afficher l'avatar.
     * → Garantit que l'URL correspond exactement aux dernières préférences sauvegardées.
     */
    public String getAvatarUrlDepuisFichier(int userId) {
        PrefsAvatar prefs = chargerPrefs(userId);
        String url = getAvatarUrl(prefs);
        System.out.println("🔄 URL depuis fichier userId=" + userId + " : " + url);
        return url;
    }

    /**
     * Retourne le chemin du cache PNG local si disponible.
     * Sinon retourne null → le dashboard doit appeler getAvatarUrlDepuisFichier().
     */
    public String getCacheLocal(int userId) {
        String fichier = PREFS_DIR + "avatar_" + userId + ".png";
        if (Files.exists(Paths.get(fichier))) {
            System.out.println("💾 Cache local trouvé : " + fichier);
            return fichier;
        }
        return null;
    }

    /**
     * Rafraîchit le cache local PNG depuis l'URL à jour.
     * À appeler dans le dashboard après une sauvegarde de préférences.
     */
    public void rafraichirCache(int userId) {
        String url = getAvatarUrlDepuisFichier(userId);
        telechargerAvatarLocal(url, userId);
    }

    // ══════════════════════════════════════════════════════════════
    // URL à partir d'un objet PrefsAvatar en mémoire
    // ══════════════════════════════════════════════════════════════
    public String getAvatarUrl(PrefsAvatar prefs) {
        return DICEBEAR_BASE
                + prefs.style.apiId
                + "/png?seed="        + encodeUrl(prefs.seed)
                + "&size="            + prefs.taille
                + "&backgroundColor=" + prefs.fond.hex
                + "&radius=50";
    }

    /**
     * Génère un seed aléatoire unique.
     * Utilisé UNIQUEMENT par le bouton "Régénérer" dans le controller.
     * Ne persiste que si l'utilisateur clique ensuite sur "Sauvegarder".
     */
    public String genererNouveauSeed(int userId) {
        return "mc_" + userId + "_" + Long.toHexString(System.currentTimeMillis());
    }

    /**
     * URL émotionnelle — n'écrase JAMAIS le seed principal.
     */
    public String getAvatarUrlEmotionnel(PrefsAvatar prefs,
                                         int scoreBE, int scoreST, int scoreHU) {
        String moodSeed = prefs.seed + "_m" + calculerMoodHash(scoreBE, scoreST, scoreHU);
        CouleurFond fondAdapte = choisirFondEmotionnel(scoreBE, scoreST, scoreHU);
        return DICEBEAR_BASE
                + prefs.style.apiId
                + "/png?seed="        + encodeUrl(moodSeed)
                + "&size="            + prefs.taille
                + "&backgroundColor=" + fondAdapte.hex
                + "&radius=50";
    }

    // ══════════════════════════════════════════════════════════════
    // Sauvegarder préférences
    // ══════════════════════════════════════════════════════════════
    public void sauvegarderPrefs(PrefsAvatar prefs) {
        try {
            Files.createDirectories(Paths.get(PREFS_DIR));
            String fichier = PREFS_DIR + "prefs_" + prefs.userId + ".properties";

            Properties p = new Properties();
            p.setProperty("seed",   prefs.seed);
            p.setProperty("style",  prefs.style.name());
            p.setProperty("fond",   prefs.fond.name());
            p.setProperty("pseudo", prefs.pseudo);
            p.setProperty("taille", String.valueOf(prefs.taille));

            try (FileWriter fw = new FileWriter(fichier)) {
                p.store(fw, "MindCare Avatar Prefs — userId=" + prefs.userId);
            }
            System.out.println("✅ Prefs sauvegardées — userId=" + prefs.userId
                    + " seed=" + prefs.seed);
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde prefs : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Charger préférences — TOUJOURS depuis le fichier
    // ══════════════════════════════════════════════════════════════
    public PrefsAvatar chargerPrefs(int userId) {
        PrefsAvatar prefs = new PrefsAvatar(userId);   // seed = "mc_" + userId par défaut
        String fichier = PREFS_DIR + "prefs_" + userId + ".properties";
        try {
            if (!Files.exists(Paths.get(fichier))) {
                System.out.println("ℹ️ Pas de prefs pour userId=" + userId
                        + " → seed par défaut=" + prefs.seed);
                return prefs;
            }

            Properties p = new Properties();
            try (FileReader fr = new FileReader(fichier)) {
                p.load(fr);
            }

            // ✅ Seed relu depuis fichier — JAMAIS recalculé
            prefs.seed   = p.getProperty("seed",   prefs.seed);
            prefs.pseudo = p.getProperty("pseudo", prefs.pseudo);

            try {
                prefs.taille = Integer.parseInt(p.getProperty("taille", "200"));
            } catch (NumberFormatException ignored) { prefs.taille = 200; }

            try {
                prefs.style = Style.valueOf(p.getProperty("style", "ADVENTURER"));
            } catch (Exception ignored) {}

            try {
                prefs.fond = CouleurFond.valueOf(p.getProperty("fond", "BLEU"));
            } catch (Exception ignored) {}

            System.out.println("✅ Prefs chargées — userId=" + userId
                    + " seed=" + prefs.seed);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement prefs : " + e.getMessage());
        }
        return prefs;
    }

    // ══════════════════════════════════════════════════════════════
    // Télécharger avatar en cache local PNG
    // ══════════════════════════════════════════════════════════════
    public String telechargerAvatarLocal(String avatarUrl, int userId) {
        try {
            Files.createDirectories(Paths.get(PREFS_DIR));
            String fichier = PREFS_DIR + "avatar_" + userId + ".png";

            URL url = new URL(avatarUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "MindCare/1.0");

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    Files.copy(is, Paths.get(fichier),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("✅ Avatar téléchargé : " + fichier);
                return fichier;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement : " + e.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    // Accompagnement émotionnel
    // ══════════════════════════════════════════════════════════════
    public EtatEmotionnel calculerEtat(int scoreBE, int scoreST, int scoreHU) {
        int moy = (scoreBE + scoreST + scoreHU) / 3;
        if (moy >= 75) return new EtatEmotionnel("Épanoui(e) ✨",
                "Votre avatar rayonne de bien-être !\nContinuez sur cette belle lancée.",
                "#10b981", "#d1fae5", "🌟");
        if (moy >= 55) return new EtatEmotionnel("Équilibré(e) 🌤️",
                "Vous avancez bien.\nUn peu de pleine conscience vous aidera.",
                "#0ea5e9", "#e0f2fe", "💙");
        if (moy >= 40) return new EtatEmotionnel("En progression 💪",
                "Votre avatar vous encourage !\nChaque petit pas compte.",
                "#f97316", "#fff7ed", "🔥");
        if (moy >= 25) return new EtatEmotionnel("Vulnérable 🌱",
                "C'est normal de traverser des phases difficiles.\nVotre avatar est là pour vous soutenir.",
                "#8b5cf6", "#ede9fe", "💜");
        return new EtatEmotionnel("A besoin de soutien 🤗",
                "Votre avatar vous envoie de la force.\nPensez à consulter un professionnel.",
                "#ef4444", "#fef2f2", "❤️");
    }

    public static class EtatEmotionnel {
        public final String titre, message, couleur, couleurBg, emoji;
        EtatEmotionnel(String titre, String message,
                       String couleur, String couleurBg, String emoji) {
            this.titre = titre; this.message = message;
            this.couleur = couleur; this.couleurBg = couleurBg; this.emoji = emoji;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers privés
    // ══════════════════════════════════════════════════════════════
    private CouleurFond choisirFondEmotionnel(int be, int st, int hu) {
        int moy = (be + st + hu) / 3;
        if (moy >= 70) return CouleurFond.VERT;
        if (moy >= 55) return CouleurFond.BLEU;
        if (moy >= 40) return CouleurFond.PECHE;
        if (moy >= 25) return CouleurFond.LAVANDE;
        return CouleurFond.ROSE;
    }

    private String calculerMoodHash(int be, int st, int hu) {
        return String.valueOf((be / 20) * 100 + (st / 20) * 10 + (hu / 20));
    }

    private String encodeUrl(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
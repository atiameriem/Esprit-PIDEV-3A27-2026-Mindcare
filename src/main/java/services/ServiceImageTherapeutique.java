package services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class ServiceImageTherapeutique {

    private static final String CACHE_DIR =
            System.getProperty("user.home") + "/.mindcare/images/";

    private static final int TIMEOUT_MS = 15_000;

    // ── Index de rotation par thème (change à chaque régénération) ──
    private static final Map<String, Integer> urlIndex = new HashMap<>();

    // ══════════════════════════════════════════════════════════
    //  Modèle de résultat
    // ══════════════════════════════════════════════════════════
    public static class ResultatImage {
        public byte[] imageBytes;
        public String prompt;
        public String message;
        public String source;
        public String theme;
        public String couleurPrimaire;

        public boolean isSuccess() {
            return imageBytes != null && imageBytes.length > 0;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  THÈMES ÉMOTIONNELS
    //  CORRECTION : messages sans emojis (s'affichaient en carré dans JavaFX)
    // ══════════════════════════════════════════════════════════
    public enum ThemeEmotionnel {

        EPANOUISSEMENT(
                "Votre bien-etre rayonne. Ce moment de serenite reflete votre harmonie interieure.",
                "#fde68a",
                new String[]{
                        "https://images.unsplash.com/photo-1522383225653-ed111181a951?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1478827387698-1527781a4887?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1490750967868-88df5691cc66?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1503149779833-1de50ebe5f8a?w=1200&q=90&fit=crop"
                }
        ),
        EQUILIBRE(
                "Vous etes dans un bel equilibre. Laissez cette serenite vous accompagner.",
                "#bae6fd",
                new String[]{
                        "https://images.unsplash.com/photo-1439066615861-d1af74d74000?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=1200&q=90&fit=crop"
                }
        ),
        PROGRESSION(
                "Chaque nouveau jour est une opportunite. Vous etes sur la bonne voie.",
                "#fed7aa",
                new String[]{
                        "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1499988921418-b7df40ff03f9?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1504701954957-2010ec3bcec1?w=1200&q=90&fit=crop"
                }
        ),
        VULNERABILITE(
                "Il est normal de traverser des periodes difficiles. Trouvez votre espace de reconfort.",
                "#ddd6fe",
                new String[]{
                        "https://images.unsplash.com/photo-1508193638397-1c4234db14d8?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1512236258305-32d7f4f8f434?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1445308394109-4ec2920981b1?w=1200&q=90&fit=crop"
                }
        ),
        SOUTIEN(
                "La nature est douce et accueillante. Vous n'etes pas seul(e) dans ce chemin.",
                "#fecaca",
                new String[]{
                        "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=1200&q=90&fit=crop",
                        "https://images.unsplash.com/photo-1484291000577-3b85f2238a45?w=1200&q=90&fit=crop"
                }
        );

        public final String messageFR;
        public final String couleur;
        public final String[] imageUrls;

        ThemeEmotionnel(String m, String c, String[] urls) {
            this.messageFR = m;
            this.couleur   = c;
            this.imageUrls = urls;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  API PUBLIQUE
    // ══════════════════════════════════════════════════════════
    public ResultatImage genererImage(int scoreBE, int scoreST, int scoreHU) {
        ThemeEmotionnel theme = choisirTheme(scoreBE, scoreST, scoreHU);
        return genererImagePourTheme(theme);
    }

    public ResultatImage genererImagePourTheme(ThemeEmotionnel theme) {
        ResultatImage res   = new ResultatImage();
        res.theme           = theme.name().toLowerCase();
        res.message         = theme.messageFR;
        res.couleurPrimaire = theme.couleur;

        // ── 1. Cache local (valide 24h) ───────────────────────
        byte[] cached = lireCache(theme.name());
        if (cached != null) {
            res.imageBytes = cached;
            res.source     = "cache";
            System.out.println("Image depuis cache : " + theme.name());
            return res;
        }

        // ── 2. Rotation des URLs Unsplash ─────────────────────
        String[] urls  = theme.imageUrls;
        int startIndex = urlIndex.getOrDefault(theme.name(), 0);

        for (int i = 0; i < urls.length; i++) {
            int idx = (startIndex + i) % urls.length;
            try {
                byte[] img = appellerUrl(urls[idx]);
                if (img != null) {
                    urlIndex.put(theme.name(), (idx + 1) % urls.length);
                    sauvegarderCache(theme.name(), img);
                    res.imageBytes = img;
                    res.source     = "unsplash-" + theme.name().toLowerCase();
                    System.out.println("Image [" + theme.name()
                            + "] OK — URL #" + (idx + 1) + "/" + urls.length);
                    return res;
                }
            } catch (Exception e) {
                System.err.println("URL #" + (idx + 1) + " indisponible");
            }
        }

        System.err.println("Aucune image disponible pour : " + theme.name());
        return res;
    }

    // ══════════════════════════════════════════════════════════
    //  CHOIX DU THÈME
    // ══════════════════════════════════════════════════════════
    public ThemeEmotionnel choisirTheme(int be, int st, int hu) {
        int moy = (be + st + hu) / 3;
        ThemeEmotionnel t =
                moy >= 75 ? ThemeEmotionnel.EPANOUISSEMENT :
                        moy >= 55 ? ThemeEmotionnel.EQUILIBRE      :
                                moy >= 40 ? ThemeEmotionnel.PROGRESSION    :
                                        moy >= 25 ? ThemeEmotionnel.VULNERABILITE  :
                                                ThemeEmotionnel.SOUTIEN;
        System.out.println("Theme image → moy=" + moy + "% → " + t.name());
        return t;
    }

    // ══════════════════════════════════════════════════════════
    //  APPEL HTTP
    // ══════════════════════════════════════════════════════════
    private byte[] appellerUrl(String urlStr) throws Exception {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MindCareApp/1.0");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStream is = conn.getInputStream()) {
                byte[] data = is.readAllBytes();
                if (data.length > 100 && estImage(data)) return data;
                return null;
            }
        }
        System.err.println("HTTP " + code + " → " + urlStr);
        return null;
    }

    private boolean estImage(byte[] data) {
        if (data.length < 4) return false;
        if (data[0] == (byte)0x89 && data[1] == 0x50) return true; // PNG
        if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) return true; // JPEG
        if (data[0] == 0x52 && data[1] == 0x49) return true; // WEBP
        return false;
    }

    // ══════════════════════════════════════════════════════════
    //  CACHE LOCAL
    // ══════════════════════════════════════════════════════════
    private byte[] lireCache(String nomTheme) {
        try {
            Path p = Path.of(CACHE_DIR + nomTheme + ".png");
            if (Files.exists(p)) {
                long age = System.currentTimeMillis()
                        - Files.getLastModifiedTime(p).toMillis();
                if (age < 86_400_000L) return Files.readAllBytes(p);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private void sauvegarderCache(String nomTheme, byte[] data) {
        try {
            Files.createDirectories(Path.of(CACHE_DIR));
            Files.write(Path.of(CACHE_DIR + nomTheme + ".png"), data);
        } catch (Exception ignore) {}
    }

    /** Vide le cache ET avance l'index d'URL → nouvelle image à chaque clic */
    public static void viderCache() {
        try {
            Path dir = Path.of(CACHE_DIR);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .filter(p -> p.toString().endsWith(".png"))
                        .forEach(p -> {
                            try { Files.delete(p); }
                            catch (Exception ignore) {}
                        });
                System.out.println("Cache vide — prochaine image differente");
            }
        } catch (Exception ignore) {}
    }
}
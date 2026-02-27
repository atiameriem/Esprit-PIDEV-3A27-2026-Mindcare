package services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ══════════════════════════════════════════════════════════════
 *  ServiceVoix — Synthèse vocale (TTS) cross-platform
 *
 *  Utilise les commandes système natives selon l'OS :
 *   • Windows  → PowerShell  (voix FR intégrée si installée)
 *   • macOS    → say -v      (Thomas = voix française)
 *   • Linux    → espeak-ng   (apt install espeak-ng)
 *
 *  Usage :
 *   ServiceVoix.parler("Bonjour !");
 *   ServiceVoix.parlerAvatar("Vous êtes au sommet !");
 *   ServiceVoix.arreter();
 * ══════════════════════════════════════════════════════════════
 */
public class ServiceVoix {

    // Thread unique — une seule voix à la fois
    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ServiceVoix-Thread");
                t.setDaemon(true); // ne bloque pas la fermeture de l'appli
                return t;
            });

    private static Process processEnCours = null;
    private static boolean voixActive = true;

    // ── OS détecté une seule fois ──────────────────────────────
    private static final String OS =
            System.getProperty("os.name", "").toLowerCase();

    // ── Vitesse et volume par défaut ──────────────────────────
    private static int vitesse = 150;   // mots/min (espeak) ou taux PowerShell
    private static float volume = 1.0f;

    // ══════════════════════════════════════════════════════════
    // API PUBLIQUE
    // ══════════════════════════════════════════════════════════

    /**
     * Lit le texte en voix française en arrière-plan.
     * Si une voix est déjà en cours, elle est interrompue.
     */
    public static void parler(String texte) {
        if (!voixActive || texte == null || texte.isBlank()) return;
        arreter(); // coupe la voix précédente
        executor.submit(() -> lireTexte(nettoyer(texte)));
    }

    /**
     * Variante pour les messages de l'avatar :
     * lit le texte avec un léger délai pour laisser
     * l'animation d'apparition se terminer d'abord.
     */
    public static void parlerAvatar(String texte) {
        if (!voixActive || texte == null || texte.isBlank()) return;
        arreter();
        executor.submit(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            lireTexte(nettoyer(texte));
        });
    }

    /** Arrête la lecture en cours immédiatement */
    public static void arreter() {
        if (processEnCours != null && processEnCours.isAlive()) {
            processEnCours.destroyForcibly();
            processEnCours = null;
        }
    }

    /** Active / désactive la voix globalement */
    public static void setVoixActive(boolean active) {
        voixActive = active;
        if (!active) arreter();
    }

    public static boolean isVoixActive() { return voixActive; }

    /** Règle la vitesse (100 = normal, 150 = rapide) */
    public static void setVitesse(int v) { vitesse = Math.max(50, Math.min(300, v)); }

    // ══════════════════════════════════════════════════════════
    // IMPLÉMENTATION INTERNE
    // ══════════════════════════════════════════════════════════

    private static void lireTexte(String texte) {
        try {
            ProcessBuilder pb;

            if (OS.contains("win")) {
                // ── Windows : PowerShell SAPI ──────────────────
                // Cherche d'abord une voix française, sinon voix par défaut
                String script = String.format(
                        "Add-Type -AssemblyName System.Speech; " +
                                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                "$voices = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture -like 'fr*' }; " +
                                "if ($voices) { $s.SelectVoice($voices[0].VoiceInfo.Name) }; " +
                                "$s.Rate = %d; " +
                                "$s.Speak('%s');",
                        rateWindows(vitesse),
                        texte.replace("'", " ")
                );
                pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-NonInteractive",
                        "-Command", script);

            } else if (OS.contains("mac")) {
                // ── macOS : commande say ───────────────────────
                // Thomas = voix française intégrée macOS
                pb = new ProcessBuilder(
                        "say", "-v", "Thomas",
                        "-r", String.valueOf(vitesse),
                        texte);

            } else {
                // ── Linux : espeak-ng ──────────────────────────
                // sudo apt install espeak-ng
                pb = new ProcessBuilder(
                        "espeak-ng",
                        "-v", "fr",
                        "-s", String.valueOf(vitesse),
                        "-a", String.valueOf((int)(volume * 100)),
                        texte);
            }

            pb.redirectErrorStream(true);
            processEnCours = pb.start();
            processEnCours.waitFor(); // attend la fin de la lecture

        } catch (Exception e) {
            System.err.println("⚠️ ServiceVoix : " + e.getMessage()
                    + " (vérifiez que espeak-ng/say/powershell est disponible)");
        }
    }

    /** Nettoie le texte : supprime émojis et caractères spéciaux */
    private static String nettoyer(String texte) {
        return texte
                // Supprime les émojis (blocs Unicode hors BMP)
                .replaceAll("[\\x{1F000}-\\x{1FFFF}]", "")
                .replaceAll("[\\x{2600}-\\x{27FF}]", "")
                // Supprime les retours à la ligne → pause naturelle
                .replace("\n", ". ")
                // Supprime les caractères problématiques pour le shell
                .replaceAll("[\"'`\\\\]", " ")
                .trim();
    }

    /**
     * Convertit vitesse (mots/min ~50-300) en Rate PowerShell (-10 à +10)
     * 150 mots/min → Rate 0 (normal)
     */
    private static int rateWindows(int vitesse) {
        // 150 wpm = taux 0, chaque ±15 wpm = ±1 taux
        int rate = (vitesse - 150) / 15;
        return Math.max(-10, Math.min(10, rate));
    }

    /** Libère les ressources à la fermeture de l'application */
    public static void fermer() {
        arreter();
        executor.shutdownNow();
    }
}
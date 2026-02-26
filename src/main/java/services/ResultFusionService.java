package services;

/**
 * ══════════════════════════════════════════════════════════════
 *  ResultFusionService
 *  Architecture hybride :
 *    1. EmotionAnalyzer (HuggingFace NLP) → émotion détectée
 *    2. ServiceGroq (LLM) → analyse personnalisée
 *    3. Fusion score + émotion → résultat enrichi
 * ══════════════════════════════════════════════════════════════
 */
public class ResultFusionService {

    private final ServiceEmotionAnalyzer emotionAnalyzer;
    private final ServiceGroq     serviceGroq;

    public ResultFusionService() {
        this.emotionAnalyzer = new ServiceEmotionAnalyzer();
        this.serviceGroq     = new ServiceGroq();
    }

    // ══════════════════════════════════════════════════════════════
    // Résultat enrichi
    // ══════════════════════════════════════════════════════════════
    public static class ResultatFusionne {
        public final String emotionLabel;   // "😊 Joie"
        public final double emotionScore;   // 0.0 → 1.0
        public final String analyse;
        public final String traitement;
        public final String exercices;

        ResultatFusionne(String emotionLabel, double emotionScore,
                         String analyse, String traitement,
                         String exercices) {
            this.emotionLabel  = emotionLabel;
            this.emotionScore  = emotionScore;
            this.analyse       = analyse;
            this.traitement    = traitement;
            this.exercices     = exercices;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Point d'entrée principal
    // ══════════════════════════════════════════════════════════════
    public ResultatFusionne analyser(String titreQuiz, int pct) {

        // ── Étape 1 : Analyse émotionnelle (HuggingFace) ──────────
        String texteContexte =
                ServiceEmotionAnalyzer.construireTexteContexte(titreQuiz, pct);

        System.out.println("🧠 Analyse émotion pour : " + texteContexte);

        String emotionLabel =
                emotionAnalyzer.analyserEmotion(texteContexte);
        double emotionScore =
                emotionAnalyzer.analyserScore(texteContexte);

        System.out.println("🎭 Émotion détectée : "
                + emotionLabel + " (" + (int)(emotionScore*100) + "%)");

        // ── Étape 2 : Prompts enrichis avec émotion ───────────────
        String emotionBrute = emotionLabel.replaceAll("[^a-zA-Zéàèùêîôûç ]", "").trim();

        String promptAnalyse =
                "Tu es un psychologue clinicien expert. "
                        + "Un patient a passé le test '" + titreQuiz + "' "
                        + "avec un score de " + pct + "%. "
                        + "L'analyse NLP de son état émotionnel indique : "
                        + emotionBrute + " (confiance " + (int)(emotionScore*100) + "%). "
                        + "Donne une analyse clinique précise en 2-3 phrases "
                        + "qui intègre à la fois le score ET l'émotion détectée. "
                        + "Sois bienveillant et professionnel. En français.";

        String promptTraitement =
                "Tu es un psychologue clinicien expert. "
                        + "Patient score " + pct + "% au test '" + titreQuiz + "', "
                        + "émotion dominante : " + emotionBrute + ". "
                        + "Propose un plan de traitement adapté à cette émotion : "
                        + "type de thérapie, fréquence, techniques (TCC, mindfulness). "
                        + "2-3 phrases précises. En français.";

        String promptExercices =
                "Tu es un psychologue clinicien expert. "
                        + "Patient score " + pct + "% au test '" + titreQuiz + "', "
                        + "émotion : " + emotionBrute + ". "
                        + "Propose 3 exercices quotidiens adaptés à cette émotion. "
                        + "Format : Nom (durée, fréquence). En français.";

        // ── Étape 3 : Appel Groq avec contexte enrichi ────────────
        String analyse    = null;
        String traitement = null;
        String exercices  = null;

        try { analyse    = serviceGroq.appellerGroq(promptAnalyse);   }
        catch (Exception e) { System.err.println("❌ Groq analyse : " + e.getMessage()); }

        try { traitement = serviceGroq.appellerGroq(promptTraitement); }
        catch (Exception e) { System.err.println("❌ Groq traitement : " + e.getMessage()); }

        try { exercices  = serviceGroq.appellerGroq(promptExercices);  }
        catch (Exception e) { System.err.println("❌ Groq exercices : " + e.getMessage()); }

        // ── Fallbacks ─────────────────────────────────────────────
        if (analyse    == null) analyse    = genererAnalyseFallback(pct, emotionLabel);
        if (traitement == null) traitement = genererTraitementFallback(pct);
        if (exercices  == null) exercices  =
                "Respiration 4-7-8 (5 min, matin). "
                        + "Méditation guidée (10 min, soir). "
                        + "Marche rapide (20 min, quotidien).";

        return new ResultatFusionne(
                emotionLabel, emotionScore,
                analyse, traitement, exercices);
    }

    // ══════════════════════════════════════════════════════════════
    // Fallbacks locaux
    // ══════════════════════════════════════════════════════════════
    private String genererAnalyseFallback(int pct, String emotion) {
        String emo = emotion.contains("Joie")
                ? "un état émotionnel positif" : emotion.contains("Tristesse")
                ? "une certaine tristesse" : emotion.contains("Anxiété")
                ? "de l'anxiété" : "un état neutre";

        if (pct >= 70)
            return "Vous présentez " + emo
                    + " avec de bons résultats. Continuez sur cette lancée.";
        if (pct >= 40)
            return "Vous traversez " + emo
                    + " avec des résultats intermédiaires. Un suivi est conseillé.";
        return "Votre profil indique " + emo
                + " et des résultats qui nécessitent attention particulière.";
    }

    private String genererTraitementFallback(int pct) {
        if (pct >= 70)
            return "Maintien par des séances bimensuelles de pleine conscience.";
        if (pct >= 40)
            return "Thérapie cognitivo-comportementale (TCC) hebdomadaire recommandée.";
        return "Suivi psychologique intensif bi-hebdomadaire vivement conseillé.";
    }
}
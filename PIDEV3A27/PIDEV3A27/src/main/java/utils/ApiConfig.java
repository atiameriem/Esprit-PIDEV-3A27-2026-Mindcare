package utils;

/**
 * Centralise la configuration API via variables d'environnement.
 *
 * Avantages:
 * - pas de clés API dans le code
 * - facile à changer sans recompiler
 */
public class ApiConfig {

    /*
     * ✅ Mode "simple" (pour projet universitaire):
     * Tu peux mettre tes clés ici directement.
     * ⚠️ Attention: ne jamais push ces clés sur GitHub.
     */
             // ex: "hf_xxx..."

    // ✅ Mailjet (v3.1) - mets tes clés ici (ne pas push sur GitHub)
    private static final String MAILJET_API_KEY_HARDCODED = "fe170674664fb6fbf494a04e42072a4b";        // ex: "xxxxxxxxxxxxxxxxxxxx"
    private static final String MAILJET_SECRET_KEY_HARDCODED = "3fd014ee122fef17f06f6e94be768b2e";     // ex: "xxxxxxxxxxxxxxxxxxxx"
    private static final String MAILJET_SENDER_EMAIL_HARDCODED = "mohamedaymen.gnaoui@esprit.tn";
    private static final String MAILJET_SENDER_NAME_HARDCODED = "MindCare Admin";

    private static final String LT_URL_HARDCODED = "http://localhost:5000";
    private static final String LT_API_KEY_HARDCODED = ""; // pas nécessaire pour public

    // Groq
    public static String groqApiKey() {
        // soit en dur, soit variable d'env GROQ_API_KEY
        return env("GROQ_API_KEY", "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");
    }

    public static String groqModel() {
        // modèles supportés (Groq) : llama-3.1-8b-instant, llama-3.3-70b-versatile
        return env("GROQ_MODEL", "llama-3.1-8b-instant");
    }

    // Un modèle instruct simple (tu peux changer sans toucher le code)
    public static String hfModel() {
        // Modèle stable pour hf-inference (tu peux changer plus tard)
        return env("HF_MODEL", "mistralai/Mistral-7B-Instruct-v0.2");
    }

    // LibreTranslate
    public static String libreTranslateUrl() {
        // Tu peux mettre ton serveur local: http://localhost:5000
        if (!LT_URL_HARDCODED.isBlank()) return LT_URL_HARDCODED.trim();
        return env("LT_URL", "https://libretranslate.com");
    }

    public static String libreTranslateApiKey() {
        // Souvent optionnel si tu utilises un serveur public (mais il peut en demander)
        if (!LT_API_KEY_HARDCODED.isBlank()) return LT_API_KEY_HARDCODED.trim();
        return env("LT_API_KEY", "");
    }

    // Mailjet
    public static String mailjetApiKey() {
        if (!MAILJET_API_KEY_HARDCODED.isBlank()) return MAILJET_API_KEY_HARDCODED.trim();
        return env("MAILJET_API_KEY", "");
    }

    public static String mailjetSecretKey() {
        if (!MAILJET_SECRET_KEY_HARDCODED.isBlank()) return MAILJET_SECRET_KEY_HARDCODED.trim();
        return env("MAILJET_SECRET_KEY", "");
    }

    public static String mailjetSenderEmail() {
        if (!MAILJET_SENDER_EMAIL_HARDCODED.isBlank()) return MAILJET_SENDER_EMAIL_HARDCODED.trim();
        return env("MAILJET_SENDER_EMAIL", "no-reply@mindcare.tn");
    }

    public static String mailjetSenderName() {
        if (!MAILJET_SENDER_NAME_HARDCODED.isBlank()) return MAILJET_SENDER_NAME_HARDCODED.trim();
        return env("MAILJET_SENDER_NAME", "MindCare Admin");
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        return v.trim();
    }
}

package services;

import utils.AppConfig;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════
 * SERVICE LLM + RAG — Génération de résumé enrichi par le contexte
 * ═══════════════════════════════════════════════════════════════
 *
 * Pipeline RAG complet :
 *   [BDD] → RagService.buildRagContext() → [Contexte CR similaires]
 *                                                    ↓
 *   [Nouveau texte saisi] ──────────────→ [Prompt enrichi]
 *                                                    ↓
 *                                          [Groq LLM API]
 *                                                    ↓
 *                                        [Résumé guidé par l'historique]
 *
 * API : Groq (gratuit) — https://console.groq.com
 * Clé : src/main/resources/config.properties → groq.api.key=gsk_...
 */
//Service qui génère un résumé via LLM.
public class LlmSummaryService {

    //URL de l’API Groq
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    //Création du client HTTP
    //HttpClient = outil qui envoie des requêtes HTTP.
    //connectTimeout(10s) = si la connexion prend plus de 10 secondes → stop.
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Objet qui va chercher dans la base les anciens comptes-rendus
    // similaires, pour fabriquer un contexte.
    private final RagService ragService = new RagService();

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE avec RAG
    // ═══════════════════════════════════════════════════════════

    /**
     * Génère un résumé enrichi par le contexte RAG (historique BDD).
     *
     * @param resumeSeance      Résumé saisi par le psychologue
     * @param prochainesActions Prochaines actions saisies
     * @param progresCr         Niveau de progression (enum name)
     * @param idPsychologiste   ID du psychologue connecté (pour filtrer le RAG)
     */
    public String generateSummaryWithRag(String resumeSeance, String prochainesActions,
                                         String progresCr, int idPsychologiste) throws Exception {
        // ÉTAPE 1 — RETRIEVE : Récupérer le contexte depuis la BDD
        String ragContext = ragService.buildRagContext(resumeSeance, prochainesActions, idPsychologiste);

        // ÉTAPE 2 — AUGMENT : Construire le prompt enrichi
        String prompt = buildRagPrompt(resumeSeance, prochainesActions, progresCr, ragContext);

        // ÉTAPE 3 — GENERATE : Envoie le prompt à Groq et renvoie le résumé généré
        return callGroqApi(prompt);
    }

    /**
     * Version sans filtrage par psychologue (contexte global de la BDD)
     */
    //Appelle la méthode RAG mais avec -1 comme psy introuvable recherche generela

    public String generateSummary(String resumeSeance, String prochainesActions, String progresCr)
            throws Exception {
        return generateSummaryWithRag(resumeSeance, prochainesActions, progresCr, -1);
    }

    // ═══════════════════════════════════════════════════════════
    // CONSTRUCTION DU PROMPT RAG
    // ═══════════════════════════════════════════════════════════

    //Fabrique un grand texte qui sera envoyé au LLM.
    private String buildRagPrompt(String resume, String actions, String progres, String ragContext) {
        //Transforme amelioration_legere en texte lisible.
        String progresLabel = formatProgres(progres);

        StringBuilder prompt = new StringBuilder();
        //Donne un rôle au modèle.
        prompt.append("Tu es un assistant medical specialise en psychologie clinique.\n");
        //Donne l’objectif précis : résumé pro en 3–5 phrases.
        prompt.append("Ta mission : generer un resume synthetique professionnel (3 a 5 phrases) ");
        prompt.append("d un compte-rendu de seance, en te basant sur les informations ci-dessous.\n\n");

        // Si tu as du contexte → tu l’ajoutes avant
        //Sinon → tu fais prompt normal
        if (ragContext != null && !ragContext.isBlank()) {
            prompt.append(ragContext).append("\n");
            prompt.append("═══════════════════════════════════════════════════\n");
            prompt.append("NOUVELLE SEANCE A RESUMER :\n");
        } else {
            prompt.append("SEANCE A RESUMER :\n");
        }

        //Ajouter les données (progress, résumé, actions)
        //Tu fournis au LLM les infos concrètes.
        prompt.append("Progression du patient : ").append(progresLabel).append("\n\n");
        prompt.append("Resume de seance saisi :\n").append(resume).append("\n\n");
        prompt.append("Prochaines actions prevues :\n").append(actions).append("\n\n");
        prompt.append("Instructions :\n");
        prompt.append("- Redige en francais, style medical professionnel\n");
        prompt.append("- Inclure le niveau de progression du patient\n");
        prompt.append("- Mentionner les points cles de la seance\n");
        prompt.append("- Conclure par les prochaines etapes therapeutiques\n");

        prompt.append("- NE PAS inventer d informations non mentionnees\n\n");
        prompt.append("Resume synthetique :");

        return prompt.toString();
    }

    //Convertit tes valeurs enum en texte humain.
    private String formatProgres(String progres) {
        if (progres == null) return "non defini";
        return switch (progres) {
            case "amelioration_significative" -> "Amelioration significative";
            case "amelioration_legere"        -> "Amelioration legere";
            case "amelioration_stable"        -> "Amelioration stable";
            case "stagnation"                 -> "Stagnation";
            default                           -> progres;
        };
    }

    // ═══════════════════════════════════════════════════════════
    // APPEL API GROQ
    // ═══════════════════════════════════════════════════════════


    //Envoie un prompt à Groq et retourne la réponse.
    private String callGroqApi(String prompt) throws Exception {
        //Cherche groq.api.key dans config.properties.
        String apiKey = AppConfig.get("groq.api.key");
        //Si pas de clé → erreur
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("VOTRE_CLE_GROQ_ICI")) {
            throw new IllegalStateException(
                    "Cle API Groq manquante.\n" +
                            "Ouvrez src/main/resources/config.properties\n" +
                            "et remplacez VOTRE_CLE_GROQ_ICI par votre cle.\n" +
                            "Obtenez-la gratuitement sur : https://console.groq.com"
            );
        }
//Construire le JSON de la requête
        String reqBody = buildRequestBody(prompt);


        //Construire la requête HTTP
        //uri : endpoint Groq
        //Content-Type : JSON
        //Authorization : Bearer + clé
        //POST : on envoie du texte JSON
        //timeout(30s) : si Groq répond pas en 30 sec → stop
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
//Envoie et récupère la réponse en texte.
        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        //401 : clé invalide
        //429 : quota dépassé
        //autre : erreur inconnue
        int status = response.statusCode();
        if (status == 401) {
            throw new RuntimeException("Cle API Groq invalide (401). Verifiez config.properties.");
        } else if (status == 429) {
            throw new RuntimeException("Quota Groq depasse (429). Reessayez dans quelques secondes.");
        } else if (status != 200) {
            throw new RuntimeException("Erreur API Groq (" + status + "):\n" + response.body());
        }

        return parseGroqResponse(response.body());
    }

    // ═══════════════════════════════════════════════════════════
    // JSON : Construction et parsing
    // ═══════════════════════════════════════════════════════════

    //Protège le texte newlines..
    private String buildRequestBody(String prompt) {
        String escaped = jsonEscape(prompt);
        return "{\"model\":\"llama-3.3-70b-versatile\"," //modele llama-3.3-70b-versatile
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}],"
                + "\"temperature\":0.3," //réponse plus stable
                + "\"max_tokens\":500}"; //longueur max réponse
    }

    private String jsonEscape(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length() + 32);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private String parseGroqResponse(String json) {
        //Cherche "content":. Si absent → réponse inattendue.
        int contentIdx = json.indexOf("\"content\":");
        if (contentIdx == -1) {
            throw new RuntimeException("Format de reponse inattendu de Groq:\n" + json);
        }
        //Trouve le premier guillemet après "content":
        //Puis boucle pour trouver la fin du texte content :
        //Stoppe sur un " qui n’est pas échappé
        int start = json.indexOf("\"", contentIdx + 10) + 1;
        int end   = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }
}

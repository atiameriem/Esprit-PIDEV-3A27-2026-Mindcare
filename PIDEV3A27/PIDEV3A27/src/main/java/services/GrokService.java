package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GrokService {

    private static final String API_KEY = "xai-8qiSYbviNN8Vs4OeX5A9fiEhPuiAvjLUscw4LcTJXFFSThk9ypvsaiSPV4XpDSBcA2irIbaFYXUKCsDY"; // À
                                                                                                                                  // remplacer
                                                                                                                                  // par
                                                                                                                                  // l'utilisateur
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";

    private final HttpClient httpClient;

    public GrokService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Classifie une réclamation en utilisant l'IA Grok.
     * 
     * @param description La description de la réclamation.
     * @param objet       L'objet de la réclamation.
     * @return La catégorie prédite.
     */
    private static final String[] CATEGORIES = {
            "BUG APPLICATION", "ERREUR SERVEUR", "PROBLÈME AFFICHAGE", "CRASH", "LENTEUR",
            "SERVICE PATIENT", "RENDEZ-VOUS", "FORUM", "FORMATION"
    };

    public CompletableFuture<String> classifyReclamation(String objet, String description) {
        String prompt = "Tu es l'assistant IA de l'application MindCare. Ta mission est de classer la réclamation du patient.\n"
                +
                "CHOISIS PARMI CETTE LISTE UNIQUEMENT :\n" +
                String.join(", ", CATEGORIES) + ", AUTRE.\n\n" +
                "RÈGLES :\n" +
                "1. Réponds par le NOM de la catégorie en MAJUSCULES.\n" +
                "2. Pas de ponctuation, pas de phrase.\n\n" +
                "Objet: " + objet + "\n" +
                "Description: " + description;

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", "grok-2");
        body.add("messages", messages);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", 50); // Pour une catégorie

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                        String content = jsonResponse.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString().toUpperCase();

                        // Extraction précise : on cherche le mot-clé exact dans la réponse
                        for (String cat : CATEGORIES) {
                            if (content.contains(cat)) {
                                return cat;
                            }
                        }
                    } else {
                        System.err.println("❌ Erreur Grok API (" + response.statusCode() + "): " + response.body());
                    }

                    // Fallback Local (Intelligence Locale)
                    return fallbackClassification(objet + " " + description);
                })
                .exceptionally(ex -> {
                    System.err.println("❌ Exception Grok Service: " + ex.getMessage());
                    return fallbackClassification(objet + " " + description);
                });
    }

    public CompletableFuture<String> summarizeReclamation(String objet, String description) {
        String prompt = "Tu es un expert en analyse de support client pour MindCare (application de santé mentale). Ton rôle est de fournir un RÉSUMÉ PROFESSIONNEL et ANALYTIQUE de la réclamation ci-dessous.\n\n"
                + "CONSIGNES :\n"
                + "1. Résume en UNE SEULE phrase percutante (max 25 mots).\n"
                + "2. Ne commence JAMAIS par 'Le client...', 'Voici...', ou 'Résumé'.\n"
                + "3. Identifie l'action critique requise.\n"
                + "4. Utilise un niveau de langue soutenu.\n\n"
                + "CONTEXTE :\n"
                + "Objet: " + objet + "\n"
                + "Description: " + description;

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", "grok-2");
        body.add("messages", messages);
        body.addProperty("temperature", 0.5);
        body.addProperty("max_tokens", 100); // Pour un résumé

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                        return jsonResponse.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString().trim();
                    }
                    if (response.statusCode() == 400) {
                        System.err.println("❌ Erreur Grok Summary API (400 Bad Request): " + response.body());
                    } else {
                        System.err.println(
                                "❌ Erreur Grok Summary API (" + response.statusCode() + "): " + response.body());
                    }
                    return fallbackSummary(objet, description);
                });
    }

    public CompletableFuture<String> suggestProfessional(String objet, String description) {
        String prompt = "Analyse cette réclamation MindCare et identifie le MÉTIER / PROFESSIONNEL EXACT (Ex: Psychiatre, Développeur Backend, Modérateur, Administrateur, etc.) le plus qualifié pour résoudre ce problème spécifiquement.\n"
                + "Réponds UNIQUEMENT par le nom du métier en MAJUSCULES.\n\n"
                + "Objet: " + objet + "\n"
                + "Description: " + description;

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", "grok-2");
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 50);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                        return jsonResponse.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString().trim().toUpperCase();
                    }
                    return "ADMINISTRATEUR"; // Fallback par défaut
                })
                .exceptionally(ex -> "ADMINISTRATEUR");
    }

    private String fallbackSummary(String objet, String description) {
        if (description == null || description.trim().isEmpty())
            return "Aucune description fournie.";

        String base = (objet != null && !objet.isEmpty()) ? objet + " : " : "";
        String clean = description.trim().replaceAll("\\s+", " ");
        int endOfFirstSentence = clean.indexOf('.');

        if (endOfFirstSentence != -1 && endOfFirstSentence > 10 && endOfFirstSentence < 120) {
            return base + clean.substring(0, endOfFirstSentence + 1);
        }

        if (clean.length() > 100) {
            return base + clean.substring(0, 97) + "...";
        }
        return base + clean;
    }

    private String fallbackClassification(String text) {
        text = text.toUpperCase();

        // 1. Technique
        if (text.contains("BUG") || text.contains("ERREUR") && text.contains("APP"))
            return "BUG APPLICATION";
        if (text.contains("SERVEUR") || text.contains("CONNEXION") || text.contains("CHARGE"))
            return "ERREUR SERVEUR";
        if (text.contains("AFFICHAGE") || text.contains("BOUTON") || text.contains("INTERF"))
            return "PROBLÈME AFFICHAGE";
        if (text.contains("CRASH") || text.contains("FERME") || text.contains("QUITTE"))
            return "CRASH";
        if (text.contains("LENT") || text.contains("TEMPS") || text.contains("RAMME"))
            return "LENTEUR";

        // 2. Business (MindCare)
        if (text.contains("RENDEZ") || text.contains("RDV") || text.contains("CONSULT"))
            return "RENDEZ-VOUS";
        if (text.contains("FORUM") || text.contains("POST") || text.contains("MESSAGE"))
            return "FORUM";
        if (text.contains("FORMAT") || text.contains("COURS") || text.contains("APPREN"))
            return "FORMATION";
        if (text.contains("PATIENT") || text.contains("AIDE") || text.contains("SUPPORT"))
            return "SERVICE PATIENT";

        return "AUTRE";
    }
}

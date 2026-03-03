package services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Petit serveur localhost pour récupérer le "code" OAuth de Nylas (redirect_uri).
 * Ex: http://localhost:7777/callback?code=...&state=...
 */
public class NylasOAuthCallbackServer {

    private HttpServer server;
    private final CompletableFuture<Map<String, String>> result = new CompletableFuture<>();

    public CompletableFuture<Map<String, String>> start(int port, String path) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext(path, new Handler());
        server.setExecutor(null);
        server.start();
        return result;
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            Map<String, String> qs = parseQuery(uri.getRawQuery());

            String body = "<html><body style='font-family:system-ui'>" +
                    "<h2>Connexion réussie ✅</h2>" +
                    "<p>Vous pouvez fermer cette page et revenir à l'application.</p>" +
                    "</body></html>";
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            result.complete(qs);
            stop();
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return map;
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }
}

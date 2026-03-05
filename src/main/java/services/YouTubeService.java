package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class YouTubeService {

    private static final String API_KEY = "";

    public static JsonArray search(String query) throws Exception {
        OkHttpClient client = new OkHttpClient();

        String searchUrl = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet" +
                "&q=" + query + " méditation relaxation" +
                "&type=video" +
                "&relevanceLanguage=fr" +
                "&maxResults=10" +
                "&key=" + API_KEY;

        Request request = new Request.Builder().url(searchUrl).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Erreur API YouTube: " + response.code() + " " + response.message());
            }
            String body = response.body().string();
            JSONObject obj = new JSONObject(body);
            JSONArray items = obj.getJSONArray("items");

            JsonArray standardizedItems = new JsonArray();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject snippet = item.getJSONObject("snippet");
                String videoId = item.getJSONObject("id").getString("videoId");

                JsonObject st = new JsonObject();
                st.addProperty("title", snippet.getString("title"));
                st.addProperty("description", snippet.getString("description"));
                st.addProperty("thumbnail",
                        snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                st.addProperty("videoId", videoId);
                standardizedItems.add(st);
            }
            return standardizedItems;
        }
    }
}

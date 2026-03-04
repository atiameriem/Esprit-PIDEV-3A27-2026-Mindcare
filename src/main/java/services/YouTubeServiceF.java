package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class YouTubeServiceF {

    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY";

    public static String getVideoTitle(String videoId) {
        try {
            OkHttpClient client = new OkHttpClient();
            String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + API_KEY;
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful())
                    return null;
                JSONObject obj = new JSONObject(response.body().string());
                JSONArray items = obj.getJSONArray("items");
                if (items.length() > 0) {
                    return decodeHtml(items.getJSONObject(0).getJSONObject("snippet").getString("title"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonArray search(String query) throws Exception {
        OkHttpClient client = new OkHttpClient();

        String searchUrl = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet" +
                "&q=" + java.net.URLEncoder.encode(query + " podcast thérapie psychologie formation", "UTF-8") +
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
                st.addProperty("title", decodeHtml(snippet.getString("title")));
                st.addProperty("description", decodeHtml(snippet.getString("description")));
                st.addProperty("thumbnail",
                        snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                st.addProperty("videoId", videoId);
                standardizedItems.add(st);
            }
            return standardizedItems;
        }
    }

    private static String decodeHtml(String text) {
        if (text == null)
            return "";
        return text
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#x27;", "'")
                .replace("&apos;", "'");
    }
}

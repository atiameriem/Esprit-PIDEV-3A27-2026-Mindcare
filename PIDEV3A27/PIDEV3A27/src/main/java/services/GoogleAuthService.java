package services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class GoogleAuthService {

    private static final String APPLICATION_NAME = "MindCare";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Arrays.asList(
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile");

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static Credential getCredentials(final HttpTransport HTTP_TRANSPORT)
            throws IOException {

        InputStream in = GoogleAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

        if (in == null) {
            throw new IOException("❌ credentials.json introuvable dans src/main/resources/");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                SCOPES)
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        // On utilise un ID unique à chaque fois (timestamp) pour que la librairie
        // Google
        // ne réutilise JAMAIS un jeton stocké précédemment et force l'ouverture du
        // navigateur.
        String userId = "user_" + System.currentTimeMillis();

        return new AuthorizationCodeInstalledApp(flow, receiver) {
            @Override
            protected void onAuthorization(
                    com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl) {
                // "select_account" force Google à afficher le sélecteur de compte dans le
                // navigateur
                authorizationUrl.set("prompt", "select_account");
                try {
                    super.onAuthorization(authorizationUrl);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.authorize(userId);
    }

    public static Userinfo authenticate()
            throws IOException, GeneralSecurityException {

        final HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = getCredentials(HTTP_TRANSPORT);

        Oauth2 oauth2 = new Oauth2.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return oauth2.userinfo().get().execute();
    }
}

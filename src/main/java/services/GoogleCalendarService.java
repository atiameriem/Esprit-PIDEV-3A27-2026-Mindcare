package services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.util.DateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import models.SeanceGroupe;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Service Google Calendar via Service Account (calendar-service.json).
 * 
 * NOTE IMPORTANTE : Sur un compte @gmail.com gratuit, un Service Account
 * ne peut pas inviter d'attendees (Erreur 403).
 * L'invitation "réelle" des patients se fait donc via l'EmailService (envoi
 * d'un fichier .ics).
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "MindCare";
    private static final String SERVICE_ACCOUNT_FILE = "/calendar-service.json";
    private static final String CALENDAR_ID = "primary";

    private final EmailService emailService = new EmailService();

    private Calendar buildCalendarService() throws Exception {
        InputStream in = GoogleCalendarService.class.getResourceAsStream(SERVICE_ACCOUNT_FILE);
        if (in == null) {
            throw new IllegalStateException("Fichier calendar-service.json introuvable.");
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(in)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        return new Calendar.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String addSeanceToCalendar(SeanceGroupe seance, int formationId, String userEmail, String psyName) {
        String eventLink = null;
        String targetCalendar = (userEmail != null && !userEmail.isEmpty()) ? userEmail : CALENDAR_ID;

        // 1. Ajout dans VOTRE calendrier (Psychologue)
        try {
            Calendar service = buildCalendarService();
            Event event = new Event()
                    .setSummary("🧠 Séance MindCare : " + seance.getTitre())
                    .setDescription("Organisateur : " + psyName + "\nLien Jitsi : " + seance.getLienJitsi())
                    .setLocation(seance.getLienJitsi());

            Date startDate = Date.from(seance.getDateHeure().atZone(ZoneId.systemDefault()).toInstant());
            event.setStart(new EventDateTime().setDateTime(new DateTime(startDate))
                    .setTimeZone(ZoneId.systemDefault().getId()));

            Date endDate = Date.from(seance.getDateHeure().plusMinutes(seance.getDureeMinutes())
                    .atZone(ZoneId.systemDefault()).toInstant());
            event.setEnd(
                    new EventDateTime().setDateTime(new DateTime(endDate)).setTimeZone(ZoneId.systemDefault().getId()));

            // On n'ajoute pas les attendees ici pour éviter l'erreur 403 Forbidden sur
            // Gmail standard
            Event created = service.events().insert(targetCalendar, event).execute();
            eventLink = created.getHtmlLink();
            seance.setGoogleEventId(created.getId());
            System.out.println("✅ Séance ajoutée à votre agenda Google (ID: " + created.getId() + ")");
        } catch (Exception e) {
            System.err.println("❌ Erreur Calendar (Service Account) : " + e.getMessage());
        }

        // 2. INVITATION DES PATIENTS (par Email avec fichier .ics pour leur calendrier)
        // C'est cette étape qui "invite" réellement les patients sans erreur 403.
        try {
            int sent = emailService.envoyerInvitationsSeance(seance, formationId, psyName, userEmail);
            System.out.println("📧 " + sent + " invitations envoyées aux patients par email.");
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi emails : " + e.getMessage());
        }

        return eventLink != null ? eventLink : "email";
    }

    // Méthodes de compatibilité
    public String addSeanceToCalendar(SeanceGroupe seance, int formationId, String userEmail) {
        return addSeanceToCalendar(seance, formationId, userEmail, "Votre Psychologue");
    }

    public boolean testEmailConnection() {
        return emailService.testConnection();
    }

    public List<String> getParticipantEmailsForFormation(int formationId) {
        return emailService.getParticipantsForFormation(formationId).stream().map(p -> p[0]).toList();
    }

    public void deleteEventFromCalendar(String eventId, String userEmail) {
        if (eventId == null || eventId.isEmpty())
            return;
        String targetCalendar = (userEmail != null && !userEmail.isEmpty()) ? userEmail : CALENDAR_ID;
        try {
            Calendar service = buildCalendarService();
            service.events().delete(targetCalendar, eventId).execute();
            System.out.println("✅ Événement Google Calendar supprimé.");
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression Calendar : " + e.getMessage());
        }
    }

    public void deleteStoredToken() {
    }
}

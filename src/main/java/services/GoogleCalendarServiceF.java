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
public class GoogleCalendarServiceF {

    private static final String APPLICATION_NAME = "MindCare";
    private static final String SERVICE_ACCOUNT_FILE = "/calendar-service.json";
    private static final String CALENDAR_ID = "mindcareservicemdp@gmail.com";

    private final EmailServiceF emailServiceF = new EmailServiceF();

    private Calendar buildCalendarService() throws Exception {
        InputStream in = GoogleCalendarServiceF.class.getResourceAsStream(SERVICE_ACCOUNT_FILE);
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
        String targetCalendar = CALENDAR_ID; // Centralise sur l'email de l'app

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
            int sent = emailServiceF.envoyerInvitationsSeance(seance, formationId, psyName, userEmail);
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
        return emailServiceF.testConnection();
    }

    public List<String> getParticipantEmailsForFormation(int formationId) {
        return emailServiceF.getParticipantsForFormation(formationId).stream().map(p -> p[0]).toList();
    }

    public void deleteEventFromCalendar(String eventId, String userEmail) {
        if (eventId == null || eventId.isEmpty())
            return;

        try {
            Calendar service = buildCalendarService();
            try {
                // 1. Tenter sur l'agenda central
                service.events().delete(CALENDAR_ID, eventId).execute();
                System.out.println("✅ Événement supprimé de l'agenda : " + CALENDAR_ID);
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                if (e.getStatusCode() == 404) {
                    // 2. Fallback sur "primary" au cas où l'événement a été créé avant la
                    // centralisation
                    service.events().delete("primary", eventId).execute();
                    System.out.println("✅ Événement supprimé de l'agenda local (primary).");
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression Calendar : " + e.getMessage());
        }
    }

    public void updateEventInCalendar(SeanceGroupe seance, String psyName) {
        if (seance.getGoogleEventId() == null || seance.getGoogleEventId().isEmpty())
            return;
        try {
            Calendar service = buildCalendarService();
            String calendarId = CALENDAR_ID;

            // Récupérer l'événement existant — fallback sur "primary" si 404
            Event event;
            try {
                event = service.events().get(calendarId, seance.getGoogleEventId()).execute();
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e1) {
                if (e1.getStatusCode() == 404) {
                    // Fallback : essai sur le calendrier "primary"
                    calendarId = "primary";
                    try {
                        event = service.events().get(calendarId, seance.getGoogleEventId()).execute();
                    } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e2) {
                        if (e2.getStatusCode() == 404) {
                            // L'event n'existe dans aucun calendrier (supprimé manuellement ?)
                            System.out.println("ℹ️ Event Google introuvable (" + seance.getGoogleEventId()
                                    + ") — peut-être supprimé de Google Calendar. Mise à jour ignorée.");
                            return;
                        }
                        throw e2;
                    }
                } else {
                    throw e1;
                }
            }

            event.setSummary("🧠 Séance MindCare : " + seance.getTitre())
                    .setDescription("Organisateur : " + psyName + "\nLien Jitsi : " + seance.getLienJitsi())
                    .setLocation(seance.getLienJitsi());

            Date startDate = Date.from(seance.getDateHeure().atZone(ZoneId.systemDefault()).toInstant());
            event.setStart(new EventDateTime().setDateTime(new DateTime(startDate))
                    .setTimeZone(ZoneId.systemDefault().getId()));

            Date endDate = Date.from(seance.getDateHeure().plusMinutes(seance.getDureeMinutes())
                    .atZone(ZoneId.systemDefault()).toInstant());
            event.setEnd(
                    new EventDateTime().setDateTime(new DateTime(endDate)).setTimeZone(ZoneId.systemDefault().getId()));

            service.events().update(calendarId, seance.getGoogleEventId(), event).execute();
            System.out.println("✅ Séance mise à jour dans l'agenda Google (" + calendarId + ").");
        } catch (Exception e) {
            System.err.println("❌ Erreur mise à jour Calendar : " + e.getMessage());
        }
    }

    public void deleteStoredToken() {
    }
}

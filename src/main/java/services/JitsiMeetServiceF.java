package services;

import utils.JitsiMeetWindowF;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service de gestion des consultations vidéo MindCare.
 * Fait le lien entre les rendez-vous et la fenêtre Jitsi.
 */
public class JitsiMeetServiceF {

    /**
     * Démarre une consultation entre un médecin et un patient.
     *
     * @param appointmentId ID du rendez-vous en base de données
     * @param doctorName    Nom complet du médecin
     * @param patientName   Nom complet du patient
     */
    public static void startConsultation(int appointmentId, String doctorName, String patientName) {
        System.out.println("[MindCare] Démarrage consultation #" + appointmentId);
        System.out.println("[MindCare] Médecin  : " + doctorName);
        System.out.println("[MindCare] Patient  : " + patientName);
        System.out.println("[MindCare] Heure    : " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        JitsiMeetWindowF.open(appointmentId, doctorName, patientName);
    }

    /**
     * Génère le lien de salle Jitsi pour partager avec le patient par email/SMS.
     */
    public static String getRoomLink(int appointmentId) {
        return "https://meet.ffmuc.net/MindCare-Session-" + appointmentId;
    }
}
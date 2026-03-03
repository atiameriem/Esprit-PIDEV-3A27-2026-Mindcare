package services;

import models.Reservation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sync simple : pousse les réservations locales vers le calendrier de l'utilisateur via Nylas.
 *
 * Notes:
 * - Pour éviter les doublons (idempotence), il faudra plus tard lire les events existants
 *   et matcher via metadata.reservationId.
 */
public class NylasCalendarSyncService {

    private final NylasService nylasService = new NylasService();
    private final ReservationService reservationService = new ReservationService();

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public CompletableFuture<String> syncRange(LocalDateTime start, LocalDateTime end) {
        String grantId = nylasService.getStoredGrantId();
        CompletableFuture<String> grantFuture = (grantId == null || grantId.isBlank())
                ? nylasService.connectAndGetGrantId()
                : CompletableFuture.completedFuture(grantId);

        return grantFuture.thenCompose(gid -> {
            List<Reservation> reservations;
            try {
                reservations = reservationService.listBetween(start, end);
            } catch (SQLException e) {
                return CompletableFuture.failedFuture(e);
            }

            // Calendar id par défaut
            String calendarId = "primary";

            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (Reservation r : reservations) {
                var startDt = r.getStartDateTime();
                var endDt = r.getEndDateTime();
                if (startDt == null || endDt == null) continue;

                String title = "Réservation - " + (r.getLocalNom() != null ? r.getLocalNom() : ("#" + r.getIdReservation()));
                String desc = "Réservation ID: " + r.getIdReservation();

                String startIso = startDt.atZone(ZoneId.systemDefault()).toOffsetDateTime().format(ISO);
                String endIso = endDt.atZone(ZoneId.systemDefault()).toOffsetDateTime().format(ISO);

                chain = chain.thenCompose(v -> nylasService.createEvent(
                        gid, calendarId, title, desc, startIso, endIso,
                        String.valueOf(r.getIdReservation())
                ));
            }

            return chain.thenApply(v -> gid);
        });
    }
}

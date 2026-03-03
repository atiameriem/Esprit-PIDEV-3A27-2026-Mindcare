package utils;

import models.Reservation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QR payload format (URL query-string):
 * rid=<id>&d=<YYYY-MM-DD>&t=<HH:MM>&sig=<base64url(hmac_sha256(secret, rid|d|t))>
 *
 * - No DB change needed to validate authenticity (token is deterministic).
 * - To prevent re-use, we rely on reservation status CHECKED_IN.
 */
public final class QrPayloadUtil {

    private QrPayloadUtil() {}

    private static final String DEFAULT_SECRET = "mindcare_dev_secret_change_me";
    private static final String ENV_SECRET = "QR_SECRET";

    private static String secret() {
        String s = System.getenv(ENV_SECRET);
        if (s == null || s.isBlank()) return DEFAULT_SECRET;
        return s;
    }

    public static String buildPayload(Reservation r) {
        if (r == null) throw new IllegalArgumentException("Reservation null");
        return buildPayload(r.getIdReservation(), r.getDateReservation(), r.getHeureDebut());
    }

    public static String buildPayload(int reservationId, LocalDate date, LocalTime heureDebut) {
        if (reservationId <= 0) throw new IllegalArgumentException("reservationId invalide");
        if (date == null) throw new IllegalArgumentException("date manquante");
        if (heureDebut == null) throw new IllegalArgumentException("heureDebut manquante");

        String d = date.toString();
        String t = heureDebut.toString(); // HH:MM or HH:MM:SS
        if (t.length() > 5) t = t.substring(0,5);

        String sig = sign(reservationId, d, t);
        return "rid=" + reservationId + "&d=" + d + "&t=" + t + "&sig=" + sig;
    }

    public static ParsedPayload parseAndVerify(String payload) {
        ParsedPayload p = parse(payload);
        String expected = sign(p.reservationId, p.date, p.time);
        if (!constantTimeEquals(expected, p.sig)) {
            throw new IllegalArgumentException("QR invalide (signature incorrecte).");
        }
        return p;
    }

    public static ParsedPayload parse(String payload) {
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("QR vide.");
        String[] parts = payload.trim().split("&");
        Map<String,String> m = new HashMap<>();
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) m.put(kv[0].trim(), kv[1].trim());
        }
        String ridS = m.get("rid");
        String d = m.get("d");
        String t = m.get("t");
        String sig = m.get("sig");

        if (ridS == null || d == null || t == null || sig == null) {
            throw new IllegalArgumentException("QR invalide (champs manquants).");
        }
        int rid;
        try { rid = Integer.parseInt(ridS); }
        catch (Exception e) { throw new IllegalArgumentException("QR invalide (rid)."); }

        if (rid <= 0) throw new IllegalArgumentException("QR invalide (rid).");
        return new ParsedPayload(rid, d, t, sig);
    }

    private static String sign(int reservationId, String d, String t) {
        String msg = reservationId + "|" + d + "|" + t;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de signer le QR payload: " + e.getMessage(), e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static final class ParsedPayload {
        public final int reservationId;
        public final String date; // YYYY-MM-DD
        public final String time; // HH:MM
        public final String sig;

        public ParsedPayload(int reservationId, String date, String time, String sig) {
            this.reservationId = reservationId;
            this.date = date;
            this.time = time;
            this.sig = sig;
        }
    }
}

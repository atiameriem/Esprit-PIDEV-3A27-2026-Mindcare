package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Minimal iCalendar (.ics) generator with VALARM.
 * This lets users import the reservation into any calendar app (Google/Apple/Outlook).
 */
public final class IcsUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private IcsUtil(){}

    public static String buildIcs(String title, String description, LocalDateTime start, LocalDateTime end, int reminderMinutesBefore) {
        String uid = UUID.randomUUID() + "@mindcare";
        String dtStart = FMT.format(start);
        String dtEnd = FMT.format(end);
        String dtStamp = FMT.format(LocalDateTime.now());

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//MindCare//Calendar//FR\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(dtStamp).append("\r\n");
        sb.append("DTSTART:").append(dtStart).append("\r\n");
        sb.append("DTEND:").append(dtEnd).append("\r\n");
        sb.append("SUMMARY:").append(escape(title)).append("\r\n");
        if (description != null && !description.isBlank()) {
            sb.append("DESCRIPTION:").append(escape(description)).append("\r\n");
        }

        // VALARM (negative trigger = before)
        if (reminderMinutesBefore > 0) {
            sb.append("BEGIN:VALARM\r\n");
            sb.append("ACTION:DISPLAY\r\n");
            sb.append("DESCRIPTION:Rappel\r\n");
            sb.append("TRIGGER:-PT").append(reminderMinutesBefore).append("M\r\n");
            sb.append("END:VALARM\r\n");
        }

        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

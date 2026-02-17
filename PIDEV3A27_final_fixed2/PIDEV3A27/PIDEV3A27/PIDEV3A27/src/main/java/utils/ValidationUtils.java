package utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Validation util (UI) : contrôle de saisie clair + messages.
 */
public class ValidationUtils {

    private ValidationUtils() {}

    public static String requiredText(String s, String fieldName) {
        if (s == null || s.trim().isEmpty()) {
            return "❌ " + fieldName + " : champ obligatoire.";
        }
        return null;
    }

    public static String minLength(String s, String fieldName, int min) {
        if (s == null) return "❌ " + fieldName + " : champ obligatoire.";
        String t = s.trim();
        if (t.length() < min) {
            return "❌ " + fieldName + " : au moins " + min + " caractères.";
        }
        return null;
    }

    public static String maxLength(String s, String fieldName, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > max) {
            return "❌ " + fieldName + " : max " + max + " caractères.";
        }
        return null;
    }

    public static Integer parsePositiveInt(String s) {
        if (s == null) return null;
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDate parseDate(String s, DateTimeFormatter fmt) {
        try {
            return LocalDate.parse(s.trim(), fmt);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalTime parseTime(String s, DateTimeFormatter fmt) {
        try {
            return LocalTime.parse(s.trim(), fmt);
        } catch (Exception e) {
            return null;
        }
    }
}

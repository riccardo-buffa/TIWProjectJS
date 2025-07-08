package it.polimi.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Classe utility per la gestione di date e tempi nell'applicazione Aste Online
 */
public class DateUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /**
     * Formatta una data nel formato dd-MM-yyyy HH:mm
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(FORMATTER);
    }

    /**
     * Converte una stringa in formato dd-MM-yyyy HH:mm in LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, FORMATTER);
    }

    /**
     * Calcola il tempo rimanente fino alla scadenza
     */
    public static String getTempoRimanente(LocalDateTime scadenza) {
        if (scadenza == null) {
            return "N/A";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(scadenza)) {
            return "Scaduta";
        }

        long giorni = ChronoUnit.DAYS.between(now, scadenza);
        long ore = ChronoUnit.HOURS.between(now.plusDays(giorni), scadenza);

        if (giorni > 0) {
            return giorni + " giorni, " + ore + " ore";
        } else {
            return ore + " ore";
        }
    }
}
package priv.cais.newDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @auther CaiS
 */
public class DateTest {

    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.of(2018, 1, 31, 22, 33);
        Date date = LocalDateTimeToUDate(localDateTime);
        LocalDate localDate = UDateToLocalDate(date);
        LocalTime localTime = UDateToLocalTime(date);

        System.out.println(localDateTime);
        System.out.println(date);
        System.out.println(localDate);
        System.out.println(localTime);

        System.out.println(LocalDateToUDate(localDate));
    }

    public static LocalDateTime UDateToLocalDateTime(Date date) {
        Instant instant = date.toInstant();
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }

    public static LocalDate UDateToLocalDate(Date date) {
        return UDateToLocalDateTime(date).toLocalDate();
    }

    public static LocalTime UDateToLocalTime(Date date) {
        return UDateToLocalDateTime(date).toLocalTime();
    }

    public static Date LocalDateTimeToUDate(LocalDateTime localDateTime) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }

    // 05. java.time.LocalDate --> Date
    public static Date LocalDateToUDate(LocalDate localDate) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
        return Date.from(instant);
    }

    // 06. java.time.LocalTime --> Date
    public static Date LocalTimeToUdate() {
        LocalTime localTime = LocalTime.now();
        LocalDate localDate = LocalDate.now();
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }

}

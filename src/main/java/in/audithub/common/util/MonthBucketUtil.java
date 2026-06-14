package in.audithub.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MonthBucketUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneId.of("Asia/Kolkata"));

    public static String getMonthBucket(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        return FORMATTER.format(instant);
    }
}

package antessio.common;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class DateUtils {
    private DateUtils(){

    }
    public static int timeDiff(Date targetDate, Date createdAt, TimeUnit timeUnit) {
        long diffInMillies = Math.abs(targetDate.getTime() - createdAt.getTime());
        return (int) timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

}

package ru.oilab.shifttimer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class SalaryCalculator {
    private SalaryCalculator() {}

    public static Result calculate(long startMillis, long endMillis, long nowMillis,
                                   double monthlySalary, ZoneId zone) {
        long safeEnd = Math.max(startMillis, endMillis);
        long cappedNow = Math.max(startMillis, Math.min(nowMillis, safeEnd));
        double earned = amountBetween(startMillis, cappedNow, monthlySalary, zone);
        double total = amountBetween(startMillis, safeEnd, monthlySalary, zone);
        long remaining = Math.max(0L, safeEnd - nowMillis);
        double progress = safeEnd == startMillis ? 1.0
                : Math.max(0.0, Math.min(1.0, (double) (cappedNow - startMillis) / (safeEnd - startMillis)));
        return new Result(earned, total, remaining, progress);
    }

    public static double amountBetween(long fromMillis, long toMillis, double monthlySalary, ZoneId zone) {
        if (toMillis <= fromMillis || monthlySalary <= 0) return 0.0;
        ZonedDateTime cursor = Instant.ofEpochMilli(fromMillis).atZone(zone);
        ZonedDateTime finish = Instant.ofEpochMilli(toMillis).atZone(zone);
        double result = 0.0;

        while (cursor.isBefore(finish)) {
            ZonedDateTime monthStart = cursor.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime nextMonth = monthStart.plusMonths(1);
            ZonedDateTime segmentEnd = finish.isBefore(nextMonth) ? finish : nextMonth;
            double monthMillis = ChronoUnit.MILLIS.between(monthStart, nextMonth);
            double segmentMillis = ChronoUnit.MILLIS.between(cursor, segmentEnd);
            result += monthlySalary * segmentMillis / monthMillis;
            cursor = segmentEnd;
        }
        return result;
    }

    public static final class Result {
        public final double earned;
        public final double total;
        public final long remainingMillis;
        public final double progress;

        Result(double earned, double total, long remainingMillis, double progress) {
            this.earned = earned;
            this.total = total;
            this.remainingMillis = remainingMillis;
            this.progress = progress;
        }
    }
}

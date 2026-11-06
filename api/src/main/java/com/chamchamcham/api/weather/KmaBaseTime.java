package com.chamchamcham.api.weather;

import java.util.Objects;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 제공 지연을 포함한 기상청 발표시각을 주입된 KST Clock에서 계산한다. */
@Component
public class KmaBaseTime {
    private static final DateTimeFormatter DATE_FORMAT=DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TM_FC_FORMAT=DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter UV_FORMAT=DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final int[] VILAGE_HOURS={2,5,8,11,14,17,20,23};
    private static final int[] MID_HOURS={6,18};
    private final Clock clock;
    public KmaBaseTime(Clock clock) { this.clock=Objects.requireNonNull(clock, "clock"); }
    public KmaBaseDateTime resolveNcst() {
        LocalDateTime now=LocalDateTime.now(clock);
        LocalDateTime base=now.getMinute()<40?now.minusHours(1):now;
        return baseDateTime(base);
    }
    public KmaBaseDateTime resolveLatest() {
        return baseDateTime(latestAvailable(LocalDateTime.now(clock),VILAGE_HOURS,10));
    }
    public KmaBaseDateTime resolveTodayRange() {
        LocalDateTime now=LocalDateTime.now(clock);
        boolean available=now.getHour()>2 || (now.getHour()==2 && now.getMinute()>=10);
        return baseDateTime(available?now.withHour(2):now.minusDays(1).withHour(23));
    }
    public String resolveMidFcst() {
        return latestAvailable(LocalDateTime.now(clock),MID_HOURS,20).withMinute(0).withSecond(0).withNano(0).format(TM_FC_FORMAT);
    }
    public String resolveUv() {
        LocalDateTime now=LocalDateTime.now(clock);
        return now.withHour(now.getHour()/3*3).format(UV_FORMAT);
    }
    private LocalDateTime latestAvailable(LocalDateTime now,int[] hours,int minute) {
        for (int i=hours.length-1;i>=0;i--) {
            int hour=hours[i];
            if (now.getHour()>hour || (now.getHour()==hour && now.getMinute()>=minute)) return now.withHour(hour);
        }
        return now.minusDays(1).withHour(hours[hours.length-1]);
    }
    private KmaBaseDateTime baseDateTime(LocalDateTime base) {
        return new KmaBaseDateTime(base.format(DATE_FORMAT),String.format("%02d00",base.getHour()));
    }
}

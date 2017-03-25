package com.jleaew.id;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Data;
import lombok.Getter;

class IntervalSequencer {
    
    private final long MASK;
    
    private final long INTERVAL;
    
    private final AtomicReference<TimestampedLong> updater;
    
    public IntervalSequencer(long mask, IntervalType intervalType) {
        MASK = mask;
        INTERVAL = (null == intervalType ? IntervalType.MILLISECOND : intervalType).getInterval();
        
        TimestampedLong initValue = new TimestampedLong(getTime(), 0);
        updater = new AtomicReference<>(initValue);
    }
    
    public IntervalSequencer(long mask) {
        this(mask, IntervalType.MILLISECOND);
    }
    
    public IntervalSequencer() {
        this(Long.MAX_VALUE + 1);
    }
    
    private long getTime() {
        return System.currentTimeMillis() / INTERVAL;
    }

    public long next() {
        return nextStamped().value;
    }
    
    public TimestampedLong nextStamped() {
        return updater.updateAndGet(this::next);
    }
    
    private TimestampedLong next(TimestampedLong prev) {
        long seq = prev.getValue();
        long prevTs = prev.getTimestamp();
        long ts = getTime();
        if (ts == prevTs) {
            seq = (seq + 1) & MASK;
            while (0 == seq && (ts = getTime()) == prevTs); // wait to next interval
        } else {
            seq = 0;
        }
        return new TimestampedLong(ts, seq);
    }
    
    static enum IntervalType {
        MILLISECOND(1),
        SECOND(1000),
        MINUTE(60 * 1000),
        HOUR(60 * 60 * 1000),
        DAY(24 * 60 * 60 * 1000),
        WEEK(7 * 24 * 60 * 60 * 1000);

        @Getter
        private int interval;

        private IntervalType(int milliseconds) {
            interval = milliseconds;
        }
    }
    
    @Data
    static class TimestampedLong implements Serializable {

        private static final long serialVersionUID = 1L;
        
        private final long timestamp;
        private final long value;
        
        private TimestampedLong(long timestamp, long value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}

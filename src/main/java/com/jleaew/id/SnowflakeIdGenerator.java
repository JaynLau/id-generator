package com.jleaew.id;

import java.util.concurrent.ThreadLocalRandom;

import com.jleaew.id.IntervalSequencer.TimestampedLong;

public class SnowflakeIdGenerator implements IdGenerator {

    private static final long EPOCH = 1451577600000L; // 2016-01-01 00:00:00.000
    
    private static final int TOTAL_BITS = 64;
    
    private static final int TIMESTAMP_BITS = 1 + 41; // first is sign bit, next 41 timestamp(ms) bits
    
    private static final int TIMESTAMP_SHIFT_BITS = TOTAL_BITS - TIMESTAMP_BITS;

    private static final int DATACENTER_ID_BITS = 5;
    
    private static final int DATACENTER_ID_SHIFT_BITS = TIMESTAMP_SHIFT_BITS - DATACENTER_ID_BITS;
    
    private static final int DATACENTER_ID_MASK = (1 << DATACENTER_ID_BITS) - 1;
    
    private static final int NODE_ID_BITS = 5;
    
    private static final int NODE_ID_SHIFT_BITS = DATACENTER_ID_SHIFT_BITS - NODE_ID_BITS;
    
    private static final int NODE_ID_MASK = (1 << NODE_ID_BITS) - 1;
    
    private static final int SEQ_BITS = 12;

    private static final int SEQ_MASK = (1 << SEQ_BITS) - 1;

    private static final IntervalSequencer SEQUENCER = new IntervalSequencer(SEQ_MASK);
    
    private final int DATACENTER_ID;

    private final int NODE_ID;
    
    public SnowflakeIdGenerator() {
        this(ThreadLocalRandom.current().nextInt(1 << DATACENTER_ID_BITS),
                ThreadLocalRandom.current().nextInt(1 << NODE_ID_BITS));
    }
    
    public SnowflakeIdGenerator(int workerId) {
        this(workerId >>> NODE_ID_BITS, workerId & NODE_ID_MASK);
    }
    
    public SnowflakeIdGenerator(int datacenterId, int nodeId) {
        DATACENTER_ID = datacenterId;
        NODE_ID = nodeId;
        
        if (DATACENTER_ID < 0 || DATACENTER_ID > DATACENTER_ID_MASK) {
            throw new IllegalArgumentException("datacenter id must be between 0 and " + DATACENTER_ID_MASK);
        }
        
        if (NODE_ID < 0 || NODE_ID > NODE_ID_MASK) {
            throw new IllegalArgumentException("node id must be between 0 and " + NODE_ID_MASK);
        }
    }

    private long nextLongId() {
        TimestampedLong seq = SEQUENCER.nextStamped();

        return ((seq.getTimestamp() - EPOCH) << TIMESTAMP_SHIFT_BITS)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT_BITS)
                | (NODE_ID << NODE_ID_SHIFT_BITS)
                | seq.getValue();
    }
    
    @Override
    public Long next() {
        return nextLongId();
    }
    
    private enum Singleton {
        INSTANCE;
        private final SnowflakeIdGenerator value = new SnowflakeIdGenerator();
    }
    
    public static SnowflakeIdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

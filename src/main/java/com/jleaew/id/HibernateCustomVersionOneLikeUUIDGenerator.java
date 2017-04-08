package com.jleaew.id;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hibernate 自定义 UUID 生成策略修改版（Hibernate 原版序号的最大值为 Short.MAX_VALUE，
 * 只使用了 15 bits，本算法使用 16 bits，降低冲突），在同一 JVM 进程内高并发大于 65536 tpms 时会产生重复 ID
 *
 * @author Jayn Leaew
 */
public class HibernateCustomVersionOneLikeUUIDGenerator implements IdGenerator {
    
    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final long mostSignificantBits;
    static {
        // IP ADDRESS
        byte[] address;
        try {
            address = InetAddress.getLocalHost().getAddress();
        } catch ( Exception e ) {
            address = new byte[4];
        }
        
        // JVM identifier
        int jvmIdentifier = (int) (System.currentTimeMillis() >>> 8);
        
        long hiBits = 0;
        // use address as first 32 bits (8 * 4 bytes)
        for (int i = 0; i < 4; i++) {
            hiBits |= (address[i] & 0xffL) << (56 - (i << 3));
        }
        // use the "jvm identifier" as the next 32 bits
        hiBits |= jvmIdentifier;
        // set the version (rfc term) appropriately
        hiBits &= 0xff_ff_ff_ff_ff_ff_0f_ffL;
        hiBits |= 0x00_00_00_00_00_00_10_00L;
        
        mostSignificantBits = hiBits;
    }
    
    @Override
    public UUID next() {
        long leastSignificantBits = generateLeastSignificantBits(System.currentTimeMillis());
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private static long generateLeastSignificantBits(long seed) {
        long loBits = 0;
        
        // use time seed as first 48 bits
        loBits |= seed << 16;
        // use short count as the next 16 bits
        int count = counter.getAndIncrement();
        loBits |= count & 0xff_ffL;
        loBits &= 0x3f_ff_ff_ff_ff_ff_ff_ffL;
        loBits |= 0x80_00_00_00_00_00_00_00L;

        return loBits;
    }

    private enum Singleton {
        INSTANCE;
        private final IdGenerator value = new HibernateCustomVersionOneLikeUUIDGenerator();
    }
    
    public static IdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

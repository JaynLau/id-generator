package com.jleaew.id;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.UUID;

import com.jleaew.id.IntervalSequencer.TimestampedLong;

/**
 * 该UUID算法是基于时间算法的一个变体，用于生成基于时间的有序自增的UUID。
 * @author Jayn Leaew
 */
public class TimedSequencedUUIDGenerator implements IdGenerator {
    
    // 16 bits sequencer
    private static final IntervalSequencer SEQUENCER = new IntervalSequencer(0xff_ffL);

    private static final short pid = getPid();
    private static final long mac_flag = getMac() << 16;

    private static short getPid() {
        short pid = 0;
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int index = name.indexOf('@');
            if (index < 0) {
                int hash = name.hashCode();
                pid = (short) ((hash >>> 16) ^ (hash & 0xffff));
            } else {
                pid = (short) Integer.parseInt(name.substring(0, index));
            }
        } catch (Throwable t) {
            pid = (short) new SecureRandom().nextInt();
        }
        return pid;
    }
    
    private static long getMac() {
        byte[] mac = null;
        try {
            NetworkInterface nic = null;
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni.isUp() && !ni.isPointToPoint()
                        && null != ni.getHardwareAddress()) {
                    nic = ni;
                    break;
                }
            }
            if (null == nic) {
                nic = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            }
            
            mac = nic.getHardwareAddress();
            
            if (mac.length < 6) {
                throw new IllegalStateException();
            }
        } catch (Throwable e) {
            mac = new byte[6];
            new SecureRandom().nextBytes(mac);
        }

        long v = 0;
        for (int i = 0; i < 6; i++) {
            v = (v << 8) | (mac[i] & 0xffL);
        }
        
        return v;
    }
    
    @Override
    public UUID next() {
        TimestampedLong tsseq = SEQUENCER.nextStamped();
        long hiBits = generateMostSignificantBits(tsseq.getTimestamp());
        long loBits = generateLeastSignificantBits(tsseq.getValue());
        return new UUID(hiBits, loBits);
    }

    private static long generateMostSignificantBits(long timestamp) {
        // use current timestamp as first 48 bits
        // use short pid as the next 16 bits
        long hiBits = (timestamp << 16) | (pid & 0xff_ffL);
        hiBits &= 0xff_ff_ff_ff_ff_ff_0f_ffL;
        hiBits |= 0x00_00_00_00_00_00_10_00L;
        return hiBits;
    }
    
    private static long generateLeastSignificantBits(long sequence) {
        // use mac address as first 48 bits
        // use short sequence as the next 16 bits
        long loBits = mac_flag | sequence;
        loBits &= 0x3f_ff_ff_ff_ff_ff_ff_ffL;
        loBits |= 0x80_00_00_00_00_00_00_00L;
        return loBits;
    }
    
    private enum Singleton {
        INSTANCE;
        
        private final IdGenerator value = new TimedSequencedUUIDGenerator();
    }
    
    public static IdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

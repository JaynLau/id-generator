package com.jleaew.id;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ThreadLocalRandom;

import com.jleaew.id.IntervalSequencer.TimestampedLong;

/**
 * 该UUID算法是基于时间算法的一个变体，主要用于生成基于时间的有序的UUID做数据库索引使用。
 * @author Jayn Leaew
 */
public class TimedSequencedUuidGenerator implements IdGenerator {
    
    private static final int PID = getPid();
    
    private static final byte[] MAC = getMac();
    
    private static final int SEQ_BITS = 13;
    
    private static final int SEQ_MASK = (1 << SEQ_BITS) - 1;

    private static final IntervalSequencer SEQUENCER = new IntervalSequencer(SEQ_MASK);

    private static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        StringBuilder sb = new StringBuilder(8);
        char ch;
        for (int i = 0; (ch = name.charAt(i++)) != '@'; ) {
            sb.append(ch);
        }
        return Integer.parseInt(sb.toString());
    }
    
    private static byte[] getMac() {
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
        } catch (Exception e) {
            mac = new byte[6];
            ThreadLocalRandom.current().nextBytes(mac);
        }
        
        return mac;
    }
    
    private long mixMacBits() {

        byte[] mac = new byte[6];
        ThreadLocalRandom.current().nextBytes(mac);
        for (int i = 0; i < mac.length; i++) {
            mac[i] ^= MAC[i];
        }
        
        long v = 0;
        for (int i = 0; i < mac.length; i++) {
            v = (v << 8) | (mac[i] & 0xFF);
        }
        
        return v;
    }
    
    @Override
    public Uuid next() {
        TimestampedLong tsseq = SEQUENCER.nextStamped();
        
        long h = tsseq.getTimestamp() << 20; // 取系统当前时间(ms)，填充开始的44位
        long seq = tsseq.getValue();
        int seqLBits = SEQ_BITS - 4;
        long seqh4 = (seq >>> seqLBits) << 16; // 取序号高4位填充时间位后面的位
        long seql9 = (seq & ((1 << seqLBits) - 1)) << 3; // 清除序号高4位，保留版本号4位，依次填充版本号后面的位
        
        long pid = PID;
        int pidLBits = 16 - 3;
        long pidh3 = pid >>> pidLBits; // 取PID高3位依次填充序号后面的位
        
        h |= (seqh4 | seql9 | pidh3); // 版本号不设置，默认0
        
        long variant = 0x80L << 56; /* set to IETF variant  */
        
        long pidl13 = (pid & ((1 << pidLBits) - 1)) << 48; // 清除PID高3位，依次填充variant后面的位
        
        long l = variant | pidl13 | mixMacBits(); // 最后48位为MAC地址的随机混淆值
        
        return new Uuid(h, l);
    }
    
    private enum Singleton {
        INSTANCE;
        
        private final IdGenerator value = new TimedSequencedUuidGenerator();
    }
    
    public static IdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

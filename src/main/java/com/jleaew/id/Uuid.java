package com.jleaew.id;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

/**
 * 扩展 {@link java.util.UUID}
 * @author Jayn Leaew
 */
public class Uuid implements Serializable, Comparable<Uuid> {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    
    public Uuid(long mostSigBits, long leastSigBits) {
        this.id = new UUID(mostSigBits, leastSigBits);
    }
    
    public Uuid(byte[] data) {
        if (data.length != 16) {
            throw new IllegalArgumentException("data must be 16 bytes in length");
        }
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);

        this.id = new UUID(msb, lsb);
    }
    
    /**
     * @see {@link java.util.UUID#version()}
     */
    public int version() {
        return id.version();
    }

    /**
     * @see {@link java.util.UUID#variant()}
     */
    public int variant() {
        return id.variant();
    }

    /**
     * @see {@link java.util.UUID#timestamp()}
     */
    public long timestamp() {
        return id.timestamp();
    }

    /**
     * @see {@link java.util.UUID#clockSequence()}
     */
    public int clockSequence() {
        return id.clockSequence();
    }

    /**
     * @see {@link java.util.UUID#node()}
     */
    public long node() {
        return id.node();
    }
    
    public UUID toUUID() {
        return id;
    }

    public String toHexString() {
        StringBuilder s = new StringBuilder(32);
        return s.append(hex(id.getMostSignificantBits()))
                .append(hex(id.getLeastSignificantBits()))
                .toString();
    }
    
    public byte[] toBytes() {
        byte[] bytes = new byte[16];
        
        for (int i = 0, shiftBits = 56; i < 8; i++) {
            bytes[i]     = (byte) ((id.getMostSignificantBits()  >>> (shiftBits - (i << 3))) & 0xFF);
            bytes[i + 8] = (byte) ((id.getLeastSignificantBits() >>> (shiftBits - (i << 3))) & 0xFF);
        }
        
        return bytes;
    }
    
    @Override
    public String toString() {
        return toHexString();
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Uuid)) {
            return false;
        }
        return this.id.equals(((Uuid) obj).id);
    }

    @Override
    public int compareTo(Uuid val) {
        return id.compareTo(val.id);
    }
    
    private static final char[] D = {
            '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
            '8' , '9' , 'a' , 'b' , 'c' , 'd' , 'e' , 'f'
    };

    private static char[] hex(long n) {
        final int SIZE = 16;
        char[] v = new char[SIZE];

        for (int i = 0; i < SIZE; i++) {
            v[i] = (char) D[(int) (n >>> ((SIZE - 1 - i) << 2)) & 0x0F];
        }

        return v;
    }
    
    private static byte[] digest(byte[] data, String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * 基于MD5构造UUID
     */
    public static Uuid md5Named(byte[] name) {
        byte[] bytes = digest(name, "MD5");
        bytes[6]  &= 0x0f;  /* clear version        */
        bytes[6]  |= 0x30;  /* set to version 3     */
        bytes[8]  &= 0x3f;  /* clear variant        */
        bytes[8]  |= 0x80;  /* set to IETF variant  */
        return new Uuid(bytes);
    }

    /**
     * 基于MD5构造UUID
     */
    public static Uuid md5Named(CharSequence name) {
        return md5Named(name.toString().getBytes());
    }

    /**
     * 基于SHA-1构造UUID
     */
    public static Uuid sha1Named(byte[] name) {
        byte[] bytes = digest(name, "SHA-1");
        bytes = Arrays.copyOfRange(bytes, 0, 16);
        bytes[6]  &= 0x0f;  /* clear version        */
        bytes[6]  |= 0x50;  /* set to version 5     */
        bytes[8]  &= 0x3f;  /* clear variant        */
        bytes[8]  |= 0x80;  /* set to IETF variant  */
        return new Uuid(bytes);
    }

    /**
     * 基于SHA-1构造UUID
     */
    public static Uuid sha1Named(CharSequence name) {
        return sha1Named(name.toString().getBytes());
    }
    
    /*
     * The random number generator used by this class to create random
     * based UUIDs. In a holder class to defer initialization until needed.
     */
    private static class Holder {
        static final SecureRandom PRNG = new SecureRandom();
    }
    
    /**
     * 基于随机算法的UUID，JDK的UUID实现
     */
    public static Uuid random() {
        byte[] randomBytes = new byte[16];
        Holder.PRNG.nextBytes(randomBytes);
        randomBytes[6]  &= 0x0f;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3f;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */
        return new Uuid(randomBytes);
    }
    
    /**
     * 基于时间算法的一个变体，生成基于时间序的UUID
     */
    public static Uuid timeSequenced() {
        return (Uuid) TimedSequencedUuidGenerator.get().next();
    }
}

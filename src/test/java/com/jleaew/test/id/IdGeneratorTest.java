package com.jleaew.test.id;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.jleaew.id.IdGenerator;
import com.jleaew.id.RandomUuidGenerator;
import com.jleaew.id.SnowflakeIdGenerator;
import com.jleaew.id.TimedSequencedUuidGenerator;

public class IdGeneratorTest {
    
    @Test
    public void testSnowflakeId() throws InterruptedException {
        System.out.println("------------- snowflake ----------------");
        testGenerator(SnowflakeIdGenerator.get());
        System.out.println("------------- snowflake benchmark ----------------");
        testGeneratorBenchmark(SnowflakeIdGenerator.get());
    }
    
    @Test
    public void testTimedSequencedUuid() throws InterruptedException {
        System.out.println("------------- Timed Sequenced UUID ----------------");
        testGenerator(TimedSequencedUuidGenerator.get());
        System.out.println("------------- Timed Sequenced UUID benchmark----------------");
        testGeneratorBenchmark(TimedSequencedUuidGenerator.get());
    }
    
    @Test
    public void testRandomUuid() throws InterruptedException {
        System.out.println("------------- Random UUID ----------------");
        testGenerator(RandomUuidGenerator.get());
        System.out.println("------------- Random UUID benchmark----------------");
        testGeneratorBenchmark(RandomUuidGenerator.get());
    }
    
    private static void preheat(IdGenerator generator, long time) {
        long st = System.currentTimeMillis();
        while (true) {
            generator.next();
            if (System.currentTimeMillis() - st > time) {
                break;
            }
        }
    }
    
    private static void testGenerator(IdGenerator generator) throws InterruptedException {
        preheat(generator, 10);
        
        Map<Serializable, Serializable> data = new ConcurrentHashMap<>(7000000);
        
        boolean[] stop = new boolean[64];
        int[] count = new int[stop.length];
        AtomicInteger activeCount = new AtomicInteger(stop.length);
        
        long st = System.currentTimeMillis();
        
        for (int i = 0; i < count.length; i++) {
            int tid = i;
            new Thread(() -> {
                while (!stop[tid]) {
                    Serializable id = generator.next();
                    data.put(id, id);
                    count[tid]++;
                }
                activeCount.decrementAndGet();
            }).start();
        }

        Thread.sleep(3000);
        
        Arrays.fill(stop, true);

        while (activeCount.get() > 0) {
            Thread.yield();
        }
        
        long t = System.currentTimeMillis() - st;
        
        long total = 0;
        for (int i = 0; i < count.length; i++) {
            int n = (int) Math.floor(count[i] / (t / 1000.0));
            total += count[i];
            System.out.printf("%4d %7d", i, n);
            System.out.println();
        }
        System.out.printf(" %11d t/s [%d %d]\n", (long) Math.floor(total / (t / 1000.0)), total, data.size());
        Assert.assertEquals(total, data.size());
    }
    
    private static void testGeneratorBenchmark(IdGenerator generator) throws InterruptedException {
        preheat(generator, 10);

        boolean[] stop = new boolean[64];
        int[] count = new int[stop.length];
        AtomicInteger activeCount = new AtomicInteger(stop.length);
        
        long st = System.currentTimeMillis();
        
        for (int i = 0; i < count.length; i++) {
            int tid = i;
            new Thread(() -> {
                while (!stop[tid]) {
                    generator.next();
                    count[tid]++;
                }
                activeCount.decrementAndGet();
            }).start();
        }

        Thread.sleep(3000);
        
        Arrays.fill(stop, true);

        while (activeCount.get() > 0) {
            Thread.yield();
        }
        
        long t = System.currentTimeMillis() - st;
        
        long total = 0;
        for (int i = 0; i < count.length; i++) {
            int n = (int) Math.floor(count[i] / (t / 1000.0));
            total += count[i];
            System.out.printf("%4d %7d", i, n);
            System.out.println();
        }
        System.out.printf(" %11d t/s [%d]\n", (long) Math.floor(total / (t / 1000.0)), total);
    }

    public static void main(String[] args) throws Exception {
        IdGeneratorTest test = new IdGeneratorTest();
        test.testSnowflakeId();
        test.testTimedSequencedUuid();
        test.testRandomUuid();
    }
}

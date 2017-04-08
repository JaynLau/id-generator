package com.jleaew.test.id;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.jleaew.id.HibernateCustomVersionOneLikeUUIDGenerator;
import com.jleaew.id.IdGenerator;
import com.jleaew.id.MongoObjectIdGenerator;
import com.jleaew.id.RandomUUIDGenerator;
import com.jleaew.id.SnowflakeIdGenerator;
import com.jleaew.id.TimedSequencedUUIDGenerator;

public class IdGeneratorTest {

    @Test
    public void testSnowflakeId() throws InterruptedException {
        System.out.println("------------- snowflake ----------------");
        testGenerator(SnowflakeIdGenerator.get());
        System.out.println("------------- snowflake benchmark ----------------");
        testGeneratorBenchmark(SnowflakeIdGenerator.get());
    }
    
    @Test
    public void testTimedSequencedUUID() throws InterruptedException {
        System.out.println("------------- Timed Sequenced UUID ----------------");
        testGenerator(TimedSequencedUUIDGenerator.get());
        System.out.println("------------- Timed Sequenced UUID benchmark----------------");
        testGeneratorBenchmark(TimedSequencedUUIDGenerator.get());
    }
    
    @Test
    public void testRandomUUID() throws InterruptedException {
        System.out.println("------------- Random UUID ----------------");
        testGenerator(RandomUUIDGenerator.get());
        System.out.println("------------- Random UUID benchmark----------------");
        testGeneratorBenchmark(RandomUUIDGenerator.get());
    }
    
    public void testHibernateCustomVersionOneLikeUUID() throws InterruptedException {
        // Hibernate自定义UUID生成策略在同一JVM进程内高并发大于 65536 tpms 时会产生重复ID
        // 该策略每毫秒生成不重复的ID数为： 65536 = 1 << 16, 当
        System.out.println("------------- HibernateCustomVersionOneLike UUID ----------------");
        testGenerator(HibernateCustomVersionOneLikeUUIDGenerator.get());
        benchmarkHibernateCustomVersionOneLikeUUID();
    }
    
    @Test
    public void benchmarkHibernateCustomVersionOneLikeUUID() throws InterruptedException {
        System.out.println("------------- HibernateCustomVersionOneLike UUID benchmark----------------");
        testGeneratorBenchmark(HibernateCustomVersionOneLikeUUIDGenerator.get());
    }
    
    public void testMongoObjectId() throws InterruptedException {
        // MongoDB的ObjectID生成策略在同一JVM进程内高并发大于 16777216 tps 时会产生重复ID
        // 该策略每秒生成不重复的ID的最大数量为： 16777216 = 1 << 24
        System.out.println("------------- MongoObjectId ----------------");
        testGenerator(MongoObjectIdGenerator.get());
        benchmarkMongoObjectId();
    }
    
    @Test
    public void benchmarkMongoObjectId() throws InterruptedException {
        System.out.println("------------- MongoObjectId benchmark----------------");
        testGeneratorBenchmark(MongoObjectIdGenerator.get());
    }
    
    private static void preheat(IdGenerator generator, long time) {
        long st = System.currentTimeMillis();
        while (true) {
            System.out.println(generator.next());
            if (System.currentTimeMillis() - st > time) {
                break;
            }
        }
    }
    
    private static final int threads = 64;
    
    private static void testGenerator(IdGenerator generator) throws InterruptedException {
        preheat(generator, 10);
        
        Map<Serializable, Serializable> data = new ConcurrentHashMap<>(7000000);
        
        boolean[] stop = new boolean[threads];
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

        boolean[] stop = new boolean[threads];
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
        test.testTimedSequencedUUID();
        test.testRandomUUID();
        test.testHibernateCustomVersionOneLikeUUID();
        test.testMongoObjectId();
    }
}

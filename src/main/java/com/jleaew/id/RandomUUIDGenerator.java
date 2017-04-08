package com.jleaew.id;

import java.util.UUID;

public class RandomUUIDGenerator implements IdGenerator {
    
    /**
     * the most compatible with jdk uuid, generate a version 4 uuid with a security PRNG.
     */
    @Override
    public UUID next() {
        return Uuid.random();
    }

    private enum Singleton {
        INSTANCE;
        private final IdGenerator value = new RandomUUIDGenerator();
    }
    
    public static IdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

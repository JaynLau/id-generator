package com.jleaew.id;

import java.io.Serializable;

public class RandomUuidGenerator implements IdGenerator {
    
    /**
     * the most compatible with jdk uuid, generate a version 4 uuid with a security PRNG.
     */
    @Override
    public Serializable next() {
        return Uuid.random();
    }

    private enum Singleton {
        INSTANCE;
        private final IdGenerator value = new RandomUuidGenerator();
    }
    
    public static IdGenerator get() {
        return Singleton.INSTANCE.value;
    }
}

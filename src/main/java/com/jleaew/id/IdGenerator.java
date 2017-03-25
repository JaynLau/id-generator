package com.jleaew.id;

import java.io.Serializable;

/**
 * ID生成器
 * 
 * @author Jayn Leaew
 */
public interface IdGenerator {

    /**
     * 生成ID，生成的ID保证在业务范围内唯一
     */
    Serializable next();
}

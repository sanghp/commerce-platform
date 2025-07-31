package com.commerce.platform.domain.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

public class UuidGenerator {
    
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();
    
    public static UUID generate() {
        return GENERATOR.generate();
    }
}
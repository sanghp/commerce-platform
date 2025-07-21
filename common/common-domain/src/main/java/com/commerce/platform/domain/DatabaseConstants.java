package com.commerce.platform.domain;

/**
 * 데이터베이스 관련 상수들을 정의하는 클래스
 */
public final class DatabaseConstants {
    
    private DatabaseConstants() {
    }

    public static final class MySQLErrorCodes {
        private MySQLErrorCodes() {}
        
        public static final int DUPLICATE_ENTRY = 1062;
    }
} 
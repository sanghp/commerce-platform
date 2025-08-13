package com.commerce.platform.payment.service.dataaccess.converter;

import org.jooq.Converter;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidBinaryConverter implements Converter<byte[], UUID> {
    
    @Override
    public UUID from(byte[] databaseObject) {
        if (databaseObject == null || databaseObject.length != 16) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(databaseObject);
        return new UUID(bb.getLong(), bb.getLong());
    }

    @Override
    public byte[] to(UUID userObject) {
        if (userObject == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(userObject.getMostSignificantBits());
        bb.putLong(userObject.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public Class<byte[]> fromType() {
        return byte[].class;
    }

    @Override
    public Class<UUID> toType() {
        return UUID.class;
    }
}
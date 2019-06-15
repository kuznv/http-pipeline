package com.payneteasy.http.pipeline.cache;

import com.payneteasy.tlv.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PathQueryBodyKeyFactory implements ICacheKeyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PathQueryBodyKeyFactory.class);

    @Override
    public CacheKey createKey(String aPath, String aQuery, byte[] aBody) {

        MessageDigest digest = createDigest();

        if(aPath == null || aQuery == null || aBody == null) {
            LOG.warn("Cannot create cache key for {} {}", aPath, aQuery);
            return CacheKey.ERROR;
        }

        digest.update(aPath.getBytes());
        digest.update(aQuery.getBytes());
        digest.update(aBody);

        byte[] hash = digest.digest();

        return new CacheKey(HexUtil.toHexString(hash));
    }

    private MessageDigest createDigest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("No sha-256 algorithm", e);
        }
        return digest;
    }
}

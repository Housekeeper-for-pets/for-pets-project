package com.forpets.domain.post.service;

import com.forpets.domain.post.dto.PostSearchCondition;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component("postCacheKeyGenerator")
public class PostCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        PostSearchCondition condition = (PostSearchCondition) params[0];
        int page = (int) params[1];
        int size = (int) params[2];
        String sort = (String) params[3];

        return generate(condition, page, size, sort);
    }

    public static String generate(PostSearchCondition condition, int page, int size, String sort) {
        String source = String.join(":",
                valueOf(condition.region()),
                valueOf(condition.careType()),
                valueOf(condition.status()),
                valueOf(condition.keyword()),
                String.valueOf(page),
                String.valueOf(size),
                valueOf(sort)
        );

        return sha256(source);
    }

    private static String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}

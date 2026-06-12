package com.bbq.module.flashsale.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "shaokao-secret-key-2024-must-be-long-enough-for-hs256";
    private static final long EXPIRATION = 30 * 60 * 1000; // accessToken 30分钟
    private static final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // refreshToken 7天

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final SecretKey REFRESH_KEY = Keys.hmacShaKeyFor((SECRET + "-refresh").getBytes(StandardCharsets.UTF_8));

    public static String generateToken(String username) {
        return buildToken(username, EXPIRATION, KEY);
    }

    public static String generateRefreshToken(String username) {
        return buildToken(username, REFRESH_EXPIRATION, REFRESH_KEY);
    }

    private static String buildToken(String username, long expiration, SecretKey key) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(REFRESH_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String getUsernameFromExpiredToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
}

package com.rayfay.gira.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return generateToken(claims, userDetails, refreshExpiration);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (isTokenBlacklisted(token)) {
            return false;
        }
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("type"));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("deprecation")
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignInKey())
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    public long getExpirationTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public void addUserToken(String username, String token) {
        userTokens.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(token);
    }

    public void invalidateAllUserTokens(String username) {
        Set<String> tokens = userTokens.remove(username);
        if (tokens != null) {
            blacklistedTokens.addAll(tokens);
        }
    }

    public void removeUserToken(String username, String token) {
        Set<String> tokens = userTokens.get(username);
        if (tokens != null) {
            tokens.remove(token);
        }
    }

    @Scheduled(fixedRate = 3600000) // 每小时清理一次
    public void cleanupExpiredTokens() {
        blacklistedTokens.removeIf(token -> {
            try {
                return isTokenExpired(token);
            } catch (Exception e) {
                return true;
            }
        });

        userTokens.values().forEach(set -> set.removeIf(token -> {
            try {
                return isTokenExpired(token);
            } catch (Exception e) {
                return true;
            }
        }));
    }
}

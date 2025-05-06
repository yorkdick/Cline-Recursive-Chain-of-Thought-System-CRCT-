package com.rayfay.gira.service.impl;

import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.response.LoginResponse;
import com.rayfay.gira.security.JwtTokenProvider;
import com.rayfay.gira.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.JwtException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .build();
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = authentication.getCredentials().toString();
            jwtTokenProvider.blacklistToken(token);
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        try {
            if (refreshToken == null) {
                throw new IllegalArgumentException("无效的刷新令牌");
            }

            String token = refreshToken;
            if (!jwtTokenProvider.isRefreshToken(token)) {
                throw new IllegalArgumentException("无效的刷新令牌类型");
            }

            String username = jwtTokenProvider.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtTokenProvider.isTokenValid(token, userDetails)) {
                String newAccessToken = jwtTokenProvider.generateToken(userDetails);
                String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

                return LoginResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtTokenProvider.getExpirationTime())
                        .build();
            }
            throw new IllegalArgumentException("刷新令牌已过期");
        } catch (JwtException e) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }
    }
}
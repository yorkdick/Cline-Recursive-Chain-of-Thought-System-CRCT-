package com.rayfay.gira.service.interfaces;

import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout();

    LoginResponse refreshToken(String refreshToken);
}
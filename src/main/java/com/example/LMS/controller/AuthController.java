package com.example.LMS.controller;

import com.example.LMS.dto.request.LoginRequest;
import com.example.LMS.dto.response.AuthResponse;
import com.example.LMS.security.JwtService;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;




    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Spring Security kiểm tra username và password
        // Nếu sai mật khẩu, nó sẽ tự văng lỗi 403 Forbidden ở đây
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Nếu đúng, tải thông tin User lên
        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

        // 3. Tạo Token từ thông tin User
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Trả về cho Frontend
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

}
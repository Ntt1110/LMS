package com.example.LMS.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getServletPath().contains("/api/v1/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Lấy Header có tên là "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Kiểm tra xem Header có tồn tại và có bắt đầu bằng chữ "Bearer " không?
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Nếu không có token, cho request đi tiếp (nó sẽ bị block ở SecurityConfig sau)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cắt lấy chuỗi Token (Bỏ đi 7 ký tự "Bearer ")
        jwt = authHeader.substring(7);

        // 4. Giải mã Token để lấy Username
        username = jwtService.extractUsername(jwt);

        // 5. Nếu có Username và người dùng này CHƯA được xác thực trong ngữ cảnh hiện tại
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Lấy thông tin User từ Database lên
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Kiểm tra Token có hợp lệ và chưa hết hạn không
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 6. Tạo một "Thẻ thông hành" (Authentication) cho Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Lưu thêm thông tin chi tiết về request (IP, SessionID...)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Cấp thẻ thông hành vào Context (Hoàn tất đăng nhập)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Cho phép request đi tiếp đến Controller
        filterChain.doFilter(request, response);
    }
}

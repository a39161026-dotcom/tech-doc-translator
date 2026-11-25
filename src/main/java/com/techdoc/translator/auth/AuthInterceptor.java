package com.techdoc.translator.auth;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.member.CurrentMemberProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /api/** 요청에서 Authorization: Bearer 토큰을 확인한다.
 * 회원가입·로그인·로그아웃과 CORS 사전 요청(OPTIONS)은 토큰 없이 통과한다.
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublic(request)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
        }
        Long memberId = jwtService.parseMemberId(header.substring(BEARER.length()).trim());
        request.setAttribute(CurrentMemberProvider.REQUEST_ATTR, memberId);
        return true;
    }

    private static boolean isPublic(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.equals("/api/members") || path.equals("/api/auth/login") || path.equals("/api/auth/logout");
    }
}

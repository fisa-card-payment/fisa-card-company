package com.fisa.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class TraceIdFilter implements HandlerInterceptor {
// HandlerInterceptor : 요청이 controller 도달하기 전/후에 끼어들 수 있는 인터셉터
    @Override
    // 요청이 들어올 때 Controller 실행 전 먼저 실행
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // UUID로 고유 16자리 ID 만들기
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        request.setAttribute("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        log.info("[GATEWAY] TraceId: {} | {} {}", traceId, request.getMethod(), request.getRequestURI());
        return true;
        // TODO: 나중에 JWT 인증 실패하면 false 로직 추가
    }
}
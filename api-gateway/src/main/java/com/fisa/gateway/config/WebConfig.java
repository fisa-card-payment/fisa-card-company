package com.fisa.gateway.config;

import com.fisa.gateway.filter.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
// Spring MVC 설정 커스터마이징
public class WebConfig implements WebMvcConfigurer {

    private final TraceIdFilter traceIdFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TraceFilter를 모든 경로에 적용
        registry.addInterceptor(traceIdFilter)
                .addPathPatterns("/**");
    }
}
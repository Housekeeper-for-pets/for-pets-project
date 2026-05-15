package com.forpets.global.health;

import com.forpets.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Docker, AWS, GitHub Actions 배포 과정에서 애플리케이션이 정상 실행 중인지 확인합니다.
 * Actuator health와 별도로 간단한 테스트용 엔드포인트를 제공합니다.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "OK"));
    }
}
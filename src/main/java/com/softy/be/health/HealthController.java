package com.softy.be.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "헬스체크", description = "서비스 상태 확인 API")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "서비스 상태 확인",
            description = "서비스 가용 상태를 반환합니다."
    )
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(
                Map.of("status", "UP")
        );
    }
}

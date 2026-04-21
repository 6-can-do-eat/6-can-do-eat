package com.team6.backend;

import com.team6.backend.global.infrastructure.config.security.jwt.JwtUtil;
import com.team6.backend.global.infrastructure.redis.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test") // application-test.yml 사용
class SixCanDoEatApplicationTests {

    // TODO
    // @MockitoBean -> 위 부분들은 ci.yml파일을 위해 junit-test를 위해 작성
    // 후에 juit test시 단위 테스트와 통합 테스트 분리 필
    // 이때는 하단과 같이 redis를 mock이 아닌 테스트용 Redis 직접 필요.
    // Redis 관련 빈들을 Mock(가짜)으로 대체하여 실제 Redis 없이도 서버가 뜨게 만듭니다.
    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    // JwtUtil도 Mock으로 대체하여 환경 변수 주입 과정을 생략합니다.
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void contextLoads() {
    }

}

package com.forpets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ForPetsApplicationTests {

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}

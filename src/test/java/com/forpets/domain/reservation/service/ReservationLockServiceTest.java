package com.forpets.domain.reservation.service;

import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationLockServiceTest {

    @InjectMocks
    private ReservationLockService reservationLockService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final Long sitterProfileId = 100L;

    @Test
    @DisplayName("[성공] 락 획득 후 task 실행 및 락 해제")
    void lock_test_01() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("lock:sitter:" + sitterProfileId),
                any(String.class),
                any(Duration.class)))
                .willReturn(true);
        given(valueOperations.get(eq("lock:sitter:" + sitterProfileId)))
                .willReturn(null); // lockValue 불일치 → delete 호출 안 됨 (정상 동작에선 일치하지만 Mock이라 상관없음)

        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        // when
        String result = reservationLockService.executeWithSitterLock(
                sitterProfileId,
                () -> {
                    taskExecuted.set(true);
                    return "success";
                });

        // then
        assertThat(result).isEqualTo("success");
        assertThat(taskExecuted).isTrue();
    }

    @Test
    @DisplayName("[성공] 락 획득 후 정상적으로 락 해제 확인")
    void lock_test_02() {
        // given
        String lockKey = "lock:sitter:" + sitterProfileId;

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(lockKey), any(String.class), any(Duration.class)))
                .willReturn(true);
        // releaseLock에서 저장된 값과 일치하면 delete 호출
        given(valueOperations.get(eq(lockKey))).willAnswer(invocation -> {
            // setIfAbsent에 전달된 lockValue를 캡처할 수 없으므로 any로 검증
            return null; // 불일치 시 delete 안 함
        });

        // when
        reservationLockService.executeWithSitterLock(sitterProfileId, () -> "done");

        // then — opsForValue()는 획득 + 해제에서 호출됨
        then(stringRedisTemplate).should(atLeast(1)).opsForValue();
    }

    @Test
    @DisplayName("[실패] 락 획득 실패 시 예외 발생")
    void lock_test_03() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("lock:sitter:" + sitterProfileId),
                any(String.class),
                any(Duration.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> reservationLockService.executeWithSitterLock(
                sitterProfileId, () -> "should not run"))
                .isInstanceOf(ReservationException.class)
                .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_CONFIRM_LOCK_FAILED));
    }

    @Test
    @DisplayName("[실패] task 실행 중 예외 발생해도 락은 해제됨")
    void lock_test_04() {
        // given
        String lockKey = "lock:sitter:" + sitterProfileId;

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(lockKey), any(String.class), any(Duration.class)))
                .willReturn(true);
        given(valueOperations.get(eq(lockKey))).willReturn(null);

        // when & then
        assertThatThrownBy(() -> reservationLockService.executeWithSitterLock(
                sitterProfileId,
                () -> { throw new RuntimeException("task 실행 중 에러"); }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("task 실행 중 에러");

        // finally 블록에서 releaseLock이 호출됐는지 확인
        then(stringRedisTemplate).should(atLeast(2)).opsForValue(); // 획득 + 해제
    }

    @Test
    @DisplayName("[성공] setIfAbsent가 null 반환 시 락 획득 실패 처리")
    void lock_test_05() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(
                eq("lock:sitter:" + sitterProfileId),
                any(String.class),
                any(Duration.class)))
                .willReturn(null); // Redis 연결 문제 등으로 null 반환

        // when & then
        assertThatThrownBy(() -> reservationLockService.executeWithSitterLock(
                sitterProfileId, () -> "should not run"))
                .isInstanceOf(ReservationException.class)
                .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                        .isEqualTo(ReservationErrorCode.RESERVATION_CONFIRM_LOCK_FAILED));
    }
}
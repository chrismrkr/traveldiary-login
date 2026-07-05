package kko.traveldiary_login.member.adaptor.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kko.traveldiary_login.member.adaptor.oauth.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataRedisTest
@Testcontainers
class RedisRefreshTokenStorageTest {

    private static final Duration REFRESH_TTL = Duration.ofSeconds(100);
    private static final Long MEMBER_ID = 1L;
    private static final String JTI = "jti-1";

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisRefreshTokenStorage storage;

    @BeforeEach
    void setUp() {
        storage = new RedisRefreshTokenStorage(
                redisTemplate, new JwtProperties(Duration.ofMinutes(30), REFRESH_TTL));
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("save 후 동일한 토큰으로 isValid 하면 true를 반환한다")
    void save_thenIsValid_returnsTrue() {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        assertThat(storage.isValid(MEMBER_ID, JTI, "refresh-token-1")).isTrue();
    }

    @Test
    @DisplayName("저장된 토큰과 다른 값이면 isValid는 false를 반환한다")
    void isValid_returnsFalse_whenTokenMismatch() {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        assertThat(storage.isValid(MEMBER_ID, JTI, "other-token")).isFalse();
    }

    @Test
    @DisplayName("저장된 토큰이 없으면 isValid는 false를 반환한다")
    void isValid_returnsFalse_whenNothingStored() {
        assertThat(storage.isValid(MEMBER_ID, JTI, "any-token")).isFalse();
    }

    @Test
    @DisplayName("delete 후에는 isValid가 false를 반환한다")
    void delete_invalidatesToken() {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        storage.delete(MEMBER_ID, JTI);

        assertThat(storage.isValid(MEMBER_ID, JTI, "refresh-token-1")).isFalse();
    }

    @Test
    @DisplayName("같은 회원의 서로 다른 기기(jti) 토큰은 독립적으로 저장·검증된다")
    void multiDevice_tokensAreIndependent() {
        storage.save(MEMBER_ID, "jti-device-A", "token-A");
        storage.save(MEMBER_ID, "jti-device-B", "token-B");

        // 두 기기의 토큰이 서로 덮어쓰지 않고 각각 유효
        assertThat(storage.isValid(MEMBER_ID, "jti-device-A", "token-A")).isTrue();
        assertThat(storage.isValid(MEMBER_ID, "jti-device-B", "token-B")).isTrue();

        // 한 기기만 로그아웃(delete)해도 다른 기기는 유지된다
        storage.delete(MEMBER_ID, "jti-device-A");

        assertThat(storage.isValid(MEMBER_ID, "jti-device-A", "token-A")).isFalse();
        assertThat(storage.isValid(MEMBER_ID, "jti-device-B", "token-B")).isTrue();
    }

    @Test
    @DisplayName("getAndDelete: 저장된 토큰을 반환하면서 즉시 삭제한다")
    void getAndDelete_returnsValueAndRemoves() {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        String consumed = storage.getAndDelete(MEMBER_ID, JTI);

        assertThat(consumed).isEqualTo("refresh-token-1");
        // 소비 후에는 더 이상 유효하지 않다
        assertThat(storage.isValid(MEMBER_ID, JTI, "refresh-token-1")).isFalse();
    }

    @Test
    @DisplayName("getAndDelete: 저장된 값이 없으면 null을 반환한다")
    void getAndDelete_returnsNullWhenAbsent() {
        assertThat(storage.getAndDelete(MEMBER_ID, JTI)).isNull();
    }

    @Test
    @DisplayName("getAndDelete: 여러 스레드가 동시에 호출해도 오직 하나만 값을 받는다 (원자적 소비)")
    void getAndDelete_isAtomic_onlyOneCallerGetsValue() throws InterruptedException {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger nonNullCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();   // 모두 동시에 출발
                    if (storage.getAndDelete(MEMBER_ID, JTI) != null) {
                        nonNullCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // 20개가 동시에 소비를 시도했지만 값을 받은 건 정확히 1개
        assertThat(nonNullCount.get()).isEqualTo(1);
        assertThat(storage.isValid(MEMBER_ID, JTI, "refresh-token-1")).isFalse();
    }

    @Test
    @DisplayName("save 시 refresh token TTL이 Redis 키 만료시간으로 설정된다")
    void save_setsTtlOnKey() {
        storage.save(MEMBER_ID, JTI, "refresh-token-1");

        Long expire = redisTemplate.getExpire("refresh:" + MEMBER_ID + ":" + JTI, TimeUnit.SECONDS);

        // 방금 설정했으므로 TTL 근처(95~100초) 값이어야 한다
        assertThat(expire).isBetween(REFRESH_TTL.getSeconds() - 5, REFRESH_TTL.getSeconds());
    }
}

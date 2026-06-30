package kko.traveldiary_login.member.adaptor.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
        storage.save(MEMBER_ID, "refresh-token-1");

        assertThat(storage.isValid(MEMBER_ID, "refresh-token-1")).isTrue();
    }

    @Test
    @DisplayName("저장된 토큰과 다른 값이면 isValid는 false를 반환한다")
    void isValid_returnsFalse_whenTokenMismatch() {
        storage.save(MEMBER_ID, "refresh-token-1");

        assertThat(storage.isValid(MEMBER_ID, "other-token")).isFalse();
    }

    @Test
    @DisplayName("저장된 토큰이 없으면 isValid는 false를 반환한다")
    void isValid_returnsFalse_whenNothingStored() {
        assertThat(storage.isValid(MEMBER_ID, "any-token")).isFalse();
    }

    @Test
    @DisplayName("delete 후에는 isValid가 false를 반환한다")
    void delete_invalidatesToken() {
        storage.save(MEMBER_ID, "refresh-token-1");

        storage.delete(MEMBER_ID);

        assertThat(storage.isValid(MEMBER_ID, "refresh-token-1")).isFalse();
    }

    @Test
    @DisplayName("save 시 refresh token TTL이 Redis 키 만료시간으로 설정된다")
    void save_setsTtlOnKey() {
        storage.save(MEMBER_ID, "refresh-token-1");

        Long expire = redisTemplate.getExpire("refresh:" + MEMBER_ID, TimeUnit.SECONDS);

        // 방금 설정했으므로 TTL 근처(95~100초) 값이어야 한다
        assertThat(expire).isBetween(REFRESH_TTL.getSeconds() - 5, REFRESH_TTL.getSeconds());
    }
}

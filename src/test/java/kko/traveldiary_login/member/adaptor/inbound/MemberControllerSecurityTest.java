package kko.traveldiary_login.member.adaptor.inbound;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;   // 실제 서명 키로 만료 토큰을 만들기 위해

    @Test
    @DisplayName("보호된 엔드포인트에 토큰 없이 접근하면 401 + 통일된 ErrorResponse 형식으로 응답한다")
    void noToken_returns401WithErrorResponse() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("위조/형식이 잘못된 토큰으로 접근하면 401 + 통일된 ErrorResponse 형식으로 응답한다")
    void invalidToken_returns401WithErrorResponse() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("만료된 access token으로 접근하면 401 + EXPIRED_TOKEN 으로 응답한다")
    void expiredToken_returns401WithExpiredCode() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + expiredAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("EXPIRED_TOKEN"))
                .andExpect(jsonPath("$.message").exists());
    }

    // 우리 서명 키로 서명되었지만 이미 만료된 access token
    private String expiredAccessToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .claim("role", "USER")
                .claim("type", "access")
                .issuedAt(now.minus(Duration.ofHours(2)))
                .expiresAt(now.minus(Duration.ofHours(1)))   // 1시간 전 만료
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

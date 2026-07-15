package kko.traveldiary_login.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // 보호된 엔드포인트에서 참조하는 인증 스킴 이름
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI travelDiaryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TravelDiary Login API")
                        .description("Google ID token 검증 및 서비스 JWT(access/refresh) 발급 API")
                        .version("v1"))
                // access token(Bearer) 인증 스킴만 등록하고, 실제 적용은 각 API의 @SecurityRequirement로 지정한다.
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("발급받은 access token을 입력하세요 (Bearer 접두사는 자동 추가)")));
    }
}

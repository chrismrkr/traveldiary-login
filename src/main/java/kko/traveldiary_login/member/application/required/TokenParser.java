package kko.traveldiary_login.member.application.required;

import kko.traveldiary_login.member.domain.TokenClaims;

public interface TokenParser {
    TokenClaims parse(String token, TokenType type);
    default TokenClaims parseAccessToken(String token) {
        return parse(token, TokenType.ACCESS);
    }
    default TokenClaims parseRefreshToken(String token) {
        return parse(token, TokenType.REFRESH);
    }

    enum TokenType {
        ACCESS("access"), REFRESH("refresh");
        private final String value;
        TokenType(String value) { this.value = value; }
        public String value() { return value; }
    }
}

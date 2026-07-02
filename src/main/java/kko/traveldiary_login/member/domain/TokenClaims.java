package kko.traveldiary_login.member.domain;

import kko.traveldiary_login.member.application.required.TokenParser;

public record TokenClaims(Long memberId, String jti, TokenParser.TokenType tokenType) { }

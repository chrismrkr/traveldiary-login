package kko.traveldiary_login.member.adaptor.oauth.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

package kko.traveldiary_login.member.application;

import kko.traveldiary_login.member.application.required.MemberRepository;
import kko.traveldiary_login.member.application.required.RefreshTokenStorage;
import kko.traveldiary_login.member.application.required.TokenIssuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    // TODO
    // AccessToken + RefreshToken 정상 발급해서 TokenPair 반환 가능한지 확인
        // case 1. 존재하는 회원인 경우 Member를 새로 만들지 않고 업데이트만 함
        // case 2. 최초 로그인 회원인 경우 Member 신규 생성

}
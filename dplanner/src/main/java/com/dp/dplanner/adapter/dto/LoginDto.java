package com.dp.dplanner.adapter.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 소셜 로그인 요청.
 * 새 방식: provider + token 을 보내면 서버가 토큰을 직접 검증해 이메일을 얻는다. (email 은 무시, name 은 신규 가입 시 표시 이름으로만 사용)
 * 옛 방식(전환 기간): email + name 만 보냄. auth.legacy-login.enabled=false 가 되면 거부된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    @Email
    private String email;
    private String name;

    /** apple, google, kakao, naver */
    private String provider;
    /** apple/google: ID 토큰, kakao/naver: 액세스 토큰 */
    private String token;

}

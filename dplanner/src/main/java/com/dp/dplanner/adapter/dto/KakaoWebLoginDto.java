package com.dp.dplanner.adapter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 웹 전용 카카오 로그인 요청.
 * 웹은 카카오 정책상 클라이언트에서 토큰 발급이 불가하여 인가 코드만 받을 수 있으므로,
 * 인가 코드와 redirect_uri 를 서버로 보내 서버가 토큰 교환을 수행한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoWebLoginDto {

    @NotBlank
    private String authorizationCode;

    @NotBlank
    private String redirectUri;
}

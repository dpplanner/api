package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.dto.LoginDto;
import com.dp.dplanner.adapter.dto.LoginResponseDto;
import com.dp.dplanner.adapter.dto.TokenDto;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;



    @GetMapping("/auth/token")
    public ResponseEntity<Void> issueToken() {

        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/refresh")
    public CommonResponse<?> refreshToken(@RequestBody @Valid TokenDto tokenDto, HttpServletResponse response) {
        TokenDto refreshedToken;
        try {
            refreshedToken = authService.refreshToken(tokenDto);
            return CommonResponse.createSuccess(refreshedToken);

        } catch (ServiceException e) {
            response.setStatus(401);
            return CommonResponse.createFail(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public CommonResponse<LoginResponseDto> login(@RequestBody @Valid LoginDto loginDto) {
        return CommonResponse.createSuccess(authService.login(loginDto));
    }

    @PostMapping("/eula") // /auth 경로는 JwtAuthenticationFilter 타지 않기 때문에 해당 API는 /auth 접두사 붙이지 않음.
    public CommonResponse eula(@AuthenticationPrincipal PrincipalDetails principal) {
        authService.eula(principal.getId());
        return CommonResponse.createSuccessWithNoContent();
    }
}

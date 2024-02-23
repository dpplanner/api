package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.dto.LoginDto;
import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/token")
    public ResponseEntity<Void> issueToken() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public CommonResponse<?> refreshToken(@RequestBody @Valid TokenDto tokenDto, HttpServletResponse response) {
        TokenDto refreshedToken;
        try {
            refreshedToken = authService.refreshToken(tokenDto);
            return CommonResponse.createSuccess(refreshedToken);

        } catch (AuthenticationException e) {
            response.setHeader("Location", "/login");
            response.setStatus(HttpStatus.SEE_OTHER.value());

            return CommonResponse.createSuccessWithNoContent();
        }
    }

    @PostMapping("/login")
    public CommonResponse<TokenDto> login(@RequestBody @Valid LoginDto loginDto) {
        TokenDto refreshedToken;
        refreshedToken = authService.login(loginDto);
        return CommonResponse.createSuccess(refreshedToken);
    }

}

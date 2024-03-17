package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.dto.LoginDto;
import com.dp.dplanner.adapter.dto.TokenDto;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

        } catch (ServiceException e) {
            response.setStatus(401);
            return CommonResponse.createFail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public CommonResponse<TokenDto> login(@RequestBody @Valid LoginDto loginDto) {
        TokenDto refreshedToken;
        refreshedToken = authService.login(loginDto);
        return CommonResponse.createSuccess(refreshedToken);
    }

    @GetMapping("/health")
    public CommonResponse health() {
        return CommonResponse.createSuccess("UP");
    }
}

package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.dto.LoginDto;
import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.redis.RedisReservationService;
import com.dp.dplanner.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final RedisReservationService redisReservationService;

    @GetMapping("/cache/{time}")
    public ResponseEntity redis(@PathVariable("time") Integer time) {

        Boolean ret = redisReservationService.saveReservation(LocalDateTime.of(2023, 8, 10, time, 0),
                LocalDateTime.of(2023, 8, 10, time + 3, 0),
                1L);


        return ResponseEntity.ok(ret);
    }

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

package com.dp.dplanner.controller;

import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.service.AuthService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<TokenDto> refreshToken(@RequestBody @Valid TokenDto tokenDto) {
        TokenDto refreshedToken = authService.refreshToken(tokenDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(refreshedToken);
    }
}

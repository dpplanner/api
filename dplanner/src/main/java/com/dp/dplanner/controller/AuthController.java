package com.dp.dplanner.controller;

import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
        TokenDto refreshedToken;
        try {
            refreshedToken = authService.refreshToken(tokenDto);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(refreshedToken);

        } catch (AuthenticationException e) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/login"));
            return ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .headers(headers)
                    .build();
        }
    }
}

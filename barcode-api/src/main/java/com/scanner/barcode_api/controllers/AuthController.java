package com.scanner.barcode_api.controllers;

import com.scanner.barcode_api.dtos.LoginRequestDTO;
import com.scanner.barcode_api.dtos.RefreshTokenRequestDTO;
import com.scanner.barcode_api.dtos.TokenResponseDTO;
import com.scanner.barcode_api.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                                  HttpServletResponse response) {
        TokenResponseDTO tokens = authService.login(loginRequest);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh-token")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(new TokenResponseDTO(tokens.accessToken(), null, tokens.tokenType()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponseDTO> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                         HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        TokenResponseDTO tokens = authService.refreshToken(new RefreshTokenRequestDTO(refreshToken));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh-token")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(new TokenResponseDTO(tokens.accessToken(), null, tokens.tokenType()));
    }
}

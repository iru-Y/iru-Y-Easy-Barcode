package com.scanner.barcode_api.services;

import com.scanner.barcode_api.dtos.LoginRequestDTO;
import com.scanner.barcode_api.dtos.RefreshTokenRequestDTO;
import com.scanner.barcode_api.dtos.TokenResponseDTO;
import com.scanner.barcode_api.models.User;
import com.scanner.barcode_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TokenResponseDTO login(LoginRequestDTO loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new TokenResponseDTO(accessToken, refreshToken, "Bearer");
    }

    public TokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token inválido");
        }

        String email = jwtService.getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);

        return new TokenResponseDTO(newAccessToken, newRefreshToken, "Bearer");
    }
}

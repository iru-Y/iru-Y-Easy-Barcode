package com.scanner.barcode_api.dtos;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType
) {}

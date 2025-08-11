package com.scanner.barcode_api.dtos;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {}

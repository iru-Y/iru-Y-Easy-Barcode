package com.scanner.barcode_api.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ScannerFileDto(
        String filename,
        LocalDateTime createdAt,
        List<String> barcodes
) {}

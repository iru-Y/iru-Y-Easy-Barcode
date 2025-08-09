package com.scanner.barcode_api.controllers;

import com.scanner.barcode_api.dtos.ScannerFileDto;
import com.scanner.barcode_api.services.BarcodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/barcode")
@RequiredArgsConstructor
@Slf4j
public class BarcodeController {

    private final BarcodeService barcodeService;

    @PostMapping()
    public ResponseEntity<?> addMultipleBarcodes(@RequestBody Map<String, Object> payload) {
        String filename = (String) payload.get("filename");
        List<String> barcodes = (List<String>) payload.get("barcodes");

        log.info("📥 Requisição batch recebida: arquivo '{}.csv' com {} barcodes", filename,
                barcodes != null ? barcodes.size() : 0);

        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "filename não pode ser vazio"));
        }

        if (barcodes == null || barcodes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "lista de barcodes não pode ser vazia"));
        }

        try {
            ScannerFileDto fileDto = barcodeService.addBarcodes(filename, barcodes);
            return ResponseEntity.status(201).body(fileDto);
        } catch (IOException e) {
            log.error("❌ Erro ao salvar lista de barcodes", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao salvar barcodes"));
        }
    }

    @GetMapping()
    public ResponseEntity<List<ScannerFileDto>> listScans() {
        log.info("📥 Requisição recebida: listar arquivos de scanner");

        try {
            List<ScannerFileDto> files = barcodeService.listScannerFilesWithBarcodes();
            log.info("✅ Total de arquivos encontrados: {}", files.size());
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            log.error("❌ Erro ao listar arquivos de scanner", e);
            return ResponseEntity.status(500).body(null);
        }
    }
    @DeleteMapping("/{filename}")
    public ResponseEntity<?> deleteByFilename(@PathVariable String filename) {
        log.info("🗑️ Requisição para deletar arquivo: {}", filename);

        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Filename não pode ser vazio"));
        }

        boolean deleted = barcodeService.deleteFileByName(filename);

        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Arquivo não encontrado ou falha ao deletar"));
        }
    }

}

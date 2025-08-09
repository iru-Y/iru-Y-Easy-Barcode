package com.scanner.barcode_api.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.scanner.barcode_api.dtos.ScannerFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarcodeService {

    private final Cloudinary cloudinary;

    private static final String DIRECTORY = "./scanner-files";

    public ScannerFileDto addBarcodes(String filenameWithoutExtension, List<String> barcodes) throws IOException {
        String cleanFilename = filenameWithoutExtension.endsWith(".csv") ? filenameWithoutExtension.substring(0, filenameWithoutExtension.length() - 4) : filenameWithoutExtension;
        String finalFilename = cleanFilename;
        Path path = Paths.get(DIRECTORY, finalFilename);

        Files.createDirectories(path.getParent());
        log.debug("📂 Diretório verificado/criado: {}", path.getParent());

        List<String> sanitized = barcodes.stream()
                .map(String::trim)
                .filter(b -> !b.isEmpty())
                .toList();

        Files.write(
                path,
                sanitized.stream()
                        .map(b -> b + System.lineSeparator())
                        .collect(Collectors.toList()),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        log.info("📝 {} barcodes adicionados ao arquivo: {}", sanitized.size(), finalFilename);

        Map<?, ?> uploadResult = cloudinary.uploader().upload(path.toFile(), ObjectUtils.asMap(
                "resource_type", "raw",
                "folder", "scanner-files",
                "public_id", cleanFilename,
                "overwrite", true
        ));
        String cloudinaryUrl = (String) uploadResult.get("secure_url");
        log.info("☁️ Arquivo '{}' enviado para Cloudinary com public_id: scanner-files/{}", cleanFilename, cleanFilename);

        FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
        LocalDateTime createdAt = creationTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        List<String> allBarcodes = Files.readAllLines(path)
                .stream()
                .map(String::trim)
                .filter(b -> !b.isEmpty())
                .collect(Collectors.toList());

        Files.delete(path);
        log.info("🗑️ Arquivo local deletado após upload: {}", path);

        return new ScannerFileDto(cleanFilename + ".csv", createdAt, allBarcodes);
    }

    public List<ScannerFileDto> listScannerFilesWithBarcodes() throws IOException {
        List<ScannerFileDto> result = new ArrayList<>();

        Path dir = Paths.get(DIRECTORY);
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                List<ScannerFileDto> localFiles = stream
                        .filter(Files::isRegularFile)
                        .map(path -> {
                            try {
                                String filename = path.getFileName().toString();
                                String filenameWithoutExtension = filename.endsWith(".csv") ? filename.substring(0, filename.length() - 4) : filename;
                                FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
                                LocalDateTime createdAt = creationTime
                                        .toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
                                List<String> barcodes = Files.readAllLines(path).stream()
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());
                                log.debug("📄 Arquivo local processado: {} ({} barcodes)", filename, barcodes.size());
                                return new ScannerFileDto(filenameWithoutExtension + ".csv", createdAt, barcodes);
                            } catch (IOException e) {
                                log.error("❌ Erro ao ler arquivo local: {}", path, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();
                result.addAll(localFiles);
            }
        }

        try {
            Map<String, Object> options = ObjectUtils.asMap("sort_by", Collections.singletonList(ObjectUtils.asMap("created_at", "desc")));
            ApiResponse cloudinaryResponse = cloudinary.search()
                    .expression("folder:scanner-files AND resource_type:raw")
                    .maxResults(100)
                    .execute();

            List<Map<String, Object>> resources = (List<Map<String, Object>>) cloudinaryResponse.get("resources");

            for (Map<String, Object> resource : resources) {
                String publicId = (String) resource.get("public_id");
                String filename = publicId.startsWith("scanner-files/") ? publicId.substring("scanner-files/".length()) : publicId;
                filename = filename.contains("/") ? filename.substring(filename.lastIndexOf("/") + 1) : filename;
                String secureUrl = (String) resource.get("secure_url");
                String createdAtStr = (String) resource.get("created_at");
                DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                LocalDateTime createdAt = OffsetDateTime.parse(createdAtStr, formatter)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();

                List<String> barcodes = fetchBarcodesFromCloudinaryUrl(secureUrl);
                result.add(new ScannerFileDto(filename + ".csv", createdAt, barcodes));
            }

            log.info("☁️ Total de arquivos no Cloudinary: {}", resources.size());
        } catch (Exception e) {
            log.error("❌ Erro ao listar arquivos do Cloudinary", e);
        }

        return result;
    }

    private List<String> fetchBarcodesFromCloudinaryUrl(String secureUrl) throws IOException {
        try {
            URL url = new URL(secureUrl);
            try (InputStream in = url.openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("❌ Erro ao baixar ou ler arquivo do Cloudinary: {}", secureUrl, e);
            throw e;
        }
    }
    public boolean deleteFileByName(String filenameWithoutExtension) {
        try {
            String publicId = "scanner-files/" + filenameWithoutExtension.replace(".csv", "");

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "raw"
            ));

            String status = (String) result.get("result");
            if ("ok".equals(status)) {
                log.info("🗑️ Arquivo deletado do Cloudinary: {}", publicId);
                return true;
            } else {
                log.warn("⚠️ Tentativa de deletar falhou ou arquivo não encontrado: {}", publicId);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Erro ao deletar arquivo no Cloudinary", e);
            return false;
        }
    }

    

}
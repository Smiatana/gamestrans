package com.smiatana.gamestrans.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smiatana.gamestrans.entity.UploadedFile;
import com.smiatana.gamestrans.repository.UploadedFileRepository;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");
    private final UploadedFileRepository uploadedFileRepository;

    public String store(MultipartFile file, String subdirectory) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        Path dir = uploadDir.resolve(subdirectory);
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        file.transferTo(dir.resolve(filename));

        String url = "/uploads/" + subdirectory + "/" + filename;

        UploadedFile record = new UploadedFile();
        record.setPath(url);
        uploadedFileRepository.save(record);

        return url;
    }

    public String storeBase64(String base64, String subdirectory) throws IOException {
        String data = base64.substring(base64.indexOf(',') + 1);
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        Path dir = uploadDir.resolve(subdirectory);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        Files.write(dir.resolve(filename), bytes);
        return "/uploads/" + subdirectory + "/" + filename;
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            uploadedFileRepository.findByPath(fileUrl).ifPresent(uploadedFile -> {
                uploadedFileRepository.delete(uploadedFile);
            });
            String relativePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            System.err.println("Не атрымалася выдаліць файл: " + fileUrl + " - " + e.getMessage());
        }
    }
}
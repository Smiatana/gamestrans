package com.smiatana.gamestrans.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public String store(MultipartFile file, String subdirectory) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        Path dir = uploadDir.resolve(subdirectory);
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        file.transferTo(dir.resolve(filename));

        return "/uploads/" + subdirectory + "/" + filename;
    }
}
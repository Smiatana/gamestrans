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
}
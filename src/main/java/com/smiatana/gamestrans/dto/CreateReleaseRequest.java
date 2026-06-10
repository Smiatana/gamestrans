package com.smiatana.gamestrans.dto;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
public class CreateReleaseRequest {
    @NotNull
    private UUID translationId;
    private String description;
    private String releaseLink;
    private MultipartFile releaseFile;
    @NotBlank
    private String status;
    @NotBlank
    private String title;
}

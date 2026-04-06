package com.smiatana.gamestrans.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class CreateTranslationRequest {
    @NotBlank
    private String gameTitle;
    @NotBlank
    private String title;
    private String gameDescription;
    private MultipartFile gameCover;

    private String description;
}

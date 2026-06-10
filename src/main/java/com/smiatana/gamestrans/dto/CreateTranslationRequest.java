package com.smiatana.gamestrans.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class CreateTranslationRequest {
    @NotBlank
    private String gameTitle;
    @NotBlank
    @Size(max = 255)
    private String title;
    private String gameDescription;
    private MultipartFile gameCover;
    private MultipartFile gameBackground;
    private String developer;
    private List<String> genres;
    private Integer releaseYear;

    @NotBlank
    private String status;

    private String description;
}
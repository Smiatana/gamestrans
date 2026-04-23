package com.smiatana.gamestrans.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class AddGameRequest {
    @NotBlank
    private String gameTitle;
    private String gameDescription;
    private MultipartFile gameCover;
    private String developer;
    private List<String> genres;
}
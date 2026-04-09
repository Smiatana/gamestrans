package com.smiatana.gamestrans.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class AddGameRequest {
    @NotBlank
    private String gameTitle;
    private String gameDescription;
    private MultipartFile gameCover;
}

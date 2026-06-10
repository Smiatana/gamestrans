package com.smiatana.gamestrans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTranslationRequest {
    @NotBlank
    @Size(max = 255)
    private String title;
    private String description;
    @NotBlank
    private String status;
}

package com.smiatana.gamestrans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTranslationRequest {
    @NotBlank
    private String title;
    private String description;
}

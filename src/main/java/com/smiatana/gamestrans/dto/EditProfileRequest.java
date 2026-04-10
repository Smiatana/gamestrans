package com.smiatana.gamestrans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditProfileRequest {
    @NotBlank
    private String username;
    private String bio;
    private String avatarCropped;
}

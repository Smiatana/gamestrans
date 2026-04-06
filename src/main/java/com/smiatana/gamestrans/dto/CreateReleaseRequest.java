package com.smiatana.gamestrans.dto;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import lombok.*;

@Getter
@Setter
public class CreateReleaseRequest {
    private UUID translationId;
    private String description;
    private String releaseLink;
    private MultipartFile releaseFile;
    private String title;
}

package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.smiatana.gamestrans.entity.Genre;
import com.smiatana.gamestrans.repository.GenreRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GenresController {
    private final GenreRepository genreRepository;

    @GetMapping("/api/genres/search")
    public List<Map<String, String>> search(@RequestParam String q) {
        return genreRepository.findByNameContainingIgnoreCase(q)
                .stream()
                .map(g -> Map.of("name", g.getName()))
                .toList();
    }

    @PostMapping("/api/genres")
    @ResponseBody
    public Map<String, String> create(@RequestParam String name) {
        return genreRepository.findByNameIgnoreCase(name)
                .map(g -> Map.of(
                        "id", g.getId().toString(),
                        "name", g.getName()))
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setName(name.trim().toLowerCase());
                    genreRepository.save(genre);
                    return Map.of(
                            "id", genre.getId().toString(),
                            "name", genre.getName());
                });
    }
}

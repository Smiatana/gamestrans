package com.smiatana.gamestrans.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String email, SseEmitter emitter) {
        emitters.put(email, emitter);
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError(e -> emitters.remove(email));
    }

    public void send(String email, String type, String payload) {
        SseEmitter emitter = emitters.get(email);
        if (emitter == null)
            return;
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(payload));
        } catch (IOException e) {
            emitters.remove(email);
        }
    }
}
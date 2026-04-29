package com.team6.backend.ai.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GeminiGenerateRequest {

    private List<Content> contents;

    public GeminiGenerateRequest(List<Content> contents) {
        this.contents = contents;
    }

    public static GeminiGenerateRequest from(String text) {
        return new GeminiGenerateRequest(
                List.of(new Content(List.of(new Part(text))))
        );
    }

    @Getter
    @NoArgsConstructor
    public static class Content {

        private List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Part {

        private String text;

        public Part(String text) {
            this.text = text;
        }
    }
}

package com.team6.backend.ai.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiGenerateResponse {

    private List<Candidate> candidates;

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        Content content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null) {
            return "";
        }

        return content.getParts()
                .stream()
                .map(Part::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining())
                .trim();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }
}

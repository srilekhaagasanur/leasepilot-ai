package com.leasepilot.backend.ai;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Finds the damage in a tenant's photo with SAM 3.1 (Segment Anything).
// Docs: https://dev.meta.ai/docs/media-segmentation and https://dev.meta.ai/docs/sam/overview
//
// We send the photo plus a short noun phrase (e.g. "water stain"). SAM answers with one line of
// special-token text: for every match, a box token and a mask token describing its exact shape.
// We store that text as-is; the frontend decodes the masks with Meta's @meta-sam/parser library.
@Component
public class SamClient {

    private final RestClient restClient;
    private final String model;

    public SamClient(RestClient metaApiRestClient, @Value("${meta.api.sam-model}") String model) {
        this.restClient = metaApiRestClient;
        this.model = model;
    }

    // Returns SAM's segmentation text, or null if nothing matching the phrase was found
    public String segment(byte[] imageBytes, String mimeType, String phrase) {
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", List.of(Map.of(
                        "type", "message",
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", phrase),
                                Map.of("type", "input_image", "image_url", dataUrl)))),
                // one_bit masks are several times smaller than the default lossless ones
                "metadata", Map.of("mask_encoding", "one_bit"));

        SamResponse response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(SamResponse.class);

        if (response == null || response.output() == null) {
            throw new IllegalStateException("SAM returned no output");
        }
        // The text lives in output[] -> message -> content[] -> output_text
        String text = response.output().stream()
                .filter(item -> item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(part -> "output_text".equals(part.type()) && part.text() != null)
                .map(OutputContent::text)
                .reduce("", String::concat)
                .trim();
        // An empty answer means SAM found no match for the phrase
        return text.isEmpty() ? null : text;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SamResponse(String status, List<OutputItem> output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OutputItem(String type, List<OutputContent> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OutputContent(String type, String text) {}
}

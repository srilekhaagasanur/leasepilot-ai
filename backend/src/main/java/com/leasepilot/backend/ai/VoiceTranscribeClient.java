package com.leasepilot.backend.ai;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.json.JsonMapper;

// Turns a voice note into text with Muse Voice Transcribe.
// Docs: https://dev.meta.ai/docs/speech-to-text
// The audio must be WAV, mono, 16-bit PCM, at 16 kHz or 24 kHz (max 10 minutes).
@Component
public class VoiceTranscribeClient {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String model;

    public VoiceTranscribeClient(RestClient metaApiRestClient, JsonMapper jsonMapper,
                                 @Value("${meta.api.transcribe-model}") String model) {
        this.restClient = metaApiRestClient;
        this.jsonMapper = jsonMapper;
        this.model = model;
    }

    public String transcribe(byte[] wavBytes) {
        String settings = jsonMapper.writeValueAsString(Map.of(
                "mode", "PUSH_TO_TALK",
                "model", model,
                "audioEncoding", "WAV"));

        // We build the multipart body by hand so it matches the docs' curl example exactly.
        // Spring's built-in multipart writer adds extra per-part headers that this endpoint
        // rejects with "Malformed multipart body".
        String boundary = "leasepilot-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, settings, wavBytes);

        TranscribeResponse response = restClient.post()
                .uri("/asr/transcribe")
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TranscribeResponse.class);

        if (response == null || response.transcript() == null) {
            throw new IllegalStateException("Muse Voice Transcribe returned no transcript");
        }
        return response.transcript().trim();
    }

    // Two parts, like: curl -F 'request={...};type=application/json' -F 'audio=@voice.wav'
    private static byte[] multipartBody(String boundary, String settingsJson, byte[] wavBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(wavBytes.length + 1024);
        write(out, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"request\"\r\n"
                + "Content-Type: application/json\r\n\r\n"
                + settingsJson + "\r\n");
        write(out, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"audio\"; filename=\"voice-note.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n");
        out.writeBytes(wavBytes);
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscribeResponse(String transcript, Long audioDurationMs) {
    }
}

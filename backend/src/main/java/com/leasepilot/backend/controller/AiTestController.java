package com.leasepilot.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.ai.MuseSparkClient;

// Temporary endpoint to check that the backend can reach Muse Spark.
// Each call uses real API tokens, so don't hit it in a loop.
@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private final MuseSparkClient museSparkClient;

    public AiTestController(MuseSparkClient museSparkClient) {
        this.museSparkClient = museSparkClient;
    }

    // GET /api/ai/ping -> asks Muse Spark for a one-sentence reply
    @GetMapping("/ping")
    public Map<String, String> ping() {
        String reply = museSparkClient.chat(
                "You are the AI inside LeasePilot, a property management app. "
                + "Reply with one short sentence confirming you are online.");
        return Map.of("model", museSparkClient.getModel(), "reply", reply);
    }
}

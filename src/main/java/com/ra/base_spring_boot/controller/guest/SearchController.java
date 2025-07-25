package com.ra.base_spring_boot.controller.guest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.base_spring_boot.Configuration.VoskModelConfig;
import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.services.guest.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Tag(name="FullText Search Controller")
@RestController
@RequestMapping("/api/v1/guest")
@RequiredArgsConstructor
public class SearchController {
    @Autowired
    private VoskModelConfig voskModelConfig;
    @Autowired
    private GuestService guestService;

    private final Map<String, Model> modelMap = new HashMap<>();

    @PostConstruct
    public void initModels() {
        try {
            System.out.println("Vosk models initialization skipped - model files not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(
            summary = "Tìm kiếm toàn văn",
            description = "Tìm kiếm tất cả các nội dung liên quan đến từ khóa, bao gồm bài hát, người dùng, album và playlist."
    )
    @GetMapping("/fullTextSearch")
    public ResponseEntity<?> fullTextSearch(@RequestParam String keyword) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .data(guestService.getFullTextSearchResponseDTO(keyword))
                .code(200)
                .build());
    }
    @PostMapping("/speechToText")
    public ResponseEntity<?> speechToText(@RequestParam("audio") MultipartFile audio,
                                          @RequestParam("lang") String lang) {
        // Kiểm tra xem có model không
        if (modelMap.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ResponseWrapper.builder()
                            .status(HttpStatus.SERVICE_UNAVAILABLE)
                            .message("Speech-to-text service is currently unavailable. Vosk models not loaded.")
                            .code(503)
                            .build());
        }

        Model selectModel = modelMap.getOrDefault(lang, modelMap.get("vi"));
        try(InputStream inputStream = audio.getInputStream()) {
            Recognizer recognizer = new Recognizer(selectModel, 16000);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            String text = recognizer.getResult();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(text);
            String keyword = jsonNode.get("text").asText();
            System.out.println(keyword);
            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.OK)
                    .data(guestService.getFullTextSearchResponseDTO(keyword))
                    .code(200)
                    .build());

        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Test speech-to-text endpoint",
            description = "Endpoint để test chức năng speech-to-text với mock data"
    )
    @PostMapping("/speechToText/test")
    public ResponseEntity<?> testSpeechToText(@RequestParam("mockText") String mockText) {
        try {
            // Mô phỏng kết quả speech-to-text
            System.out.println("Mock speech-to-text result: " + mockText);

            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.OK)
                    .data(guestService.getFullTextSearchResponseDTO(mockText))
                    .message("Mock speech-to-text test successful")
                    .code(200)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Test failed: " + e.getMessage())
                            .code(500)
                            .build());
        }
    }

}

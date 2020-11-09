package com.uhmarcel.speechtotext.controllers;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.uhmarcel.speechtotext.services.FileStorageService;
import com.uhmarcel.speechtotext.services.TranscriberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@RestController()
@RequestMapping("/api/v1/transcribe/")
public class TranscriberController {

    private FileStorageService fileStorageService;
    private TranscriberService transcriberService;

    public TranscriberController(TranscriberService transcriberService, FileStorageService fileStorageService) {
        this.transcriberService = transcriberService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/beat")
    public String beat() {
        return Instant.now().toString();
    }

    @GetMapping("/transcribe")
    public String transcribe(
        @RequestParam(name = "parent") String filename,
        @RequestParam(name = "language", defaultValue = "en-US") String language
    ) throws InterruptedException, UnsupportedAudioFileException, ExecutionException, IOException {
        return transcriberService.transcribeFile(filename, language);
    }

    @GetMapping("/test")
    public String test() {
        Bucket bucket = fileStorageService.getBucket();

        String text = "My secret message";
        byte[] bytes = text.getBytes();
        Blob blob = bucket.create("first-text.txt", bytes);
        return text;
    }

}

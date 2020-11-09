package com.uhmarcel.speechtotext.controllers;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.uhmarcel.speechtotext.services.FileStorageService;
import com.uhmarcel.speechtotext.services.TranscriberService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@RestController()
@RequestMapping("/api/v1/")
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

    @PostMapping("/transcribe")
    public String transcribe(
        @RequestParam(name = "file") MultipartFile file,
        @RequestParam(name = "language", defaultValue = "en-US") String language
    ) throws InterruptedException, UnsupportedAudioFileException, ExecutionException, IOException {
        return transcriberService.transcribeFile(file, language);
    }

//    @PostMapping("/test")
//    public String test(@RequestParam("file") MultipartFile file) throws IOException, UnsupportedAudioFileException {
//        System.out.println("Loading file " + file.getOriginalFilename());
//        transcriberService.getAudioFrameRate(file);
//        return "works";
//    }

}

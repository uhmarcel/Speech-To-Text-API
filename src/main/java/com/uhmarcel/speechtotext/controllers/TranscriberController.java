package com.uhmarcel.speechtotext.controllers;

import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController()
@RequestMapping("/api/v1/")
public class TranscriberController {

    private ExecutorService executorService;
    private FileStorageService fileStorageService;
    private TranscriberService transcriberService;

    public TranscriberController(TranscriberService transcriberService, FileStorageService fileStorageService) {
        this.executorService = Executors.newFixedThreadPool(10);
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
    ) throws UnsupportedAudioFileException, IOException {
        final String filename = file.getOriginalFilename();
        final String resourceUri = fileStorageService.getApplicationBucketUri() + filename;
        final RecognitionConfig.AudioEncoding encoding = transcriberService.getAudioEncoding(filename);
        final Integer sampleRate = transcriberService.getAudioFrameRate(file);

        executorService.submit(() -> {
            try {
                Blob temp = fileStorageService.uploadMultipartFile(file);
                transcriberService.transcribeFile(filename, resourceUri, encoding, language, sampleRate);
                temp.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return filename;
    }

//    @PostMapping("/test")
//    public String test(@RequestParam("file") MultipartFile file) throws IOException, UnsupportedAudioFileException {
//        System.out.println("Loading file " + file.getOriginalFilename());
//        transcriberService.getAudioFrameRate(file);
//        return "works";
//    }

}

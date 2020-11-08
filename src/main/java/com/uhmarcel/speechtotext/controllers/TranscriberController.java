package com.uhmarcel.speechtotext.controllers;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.protobuf.ByteString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@RestController()
@RequestMapping("/api/v1/transcribe/")
public class TranscriberController {

  public TranscriberController() {}

  @GetMapping("/beat")
  public String beat() {
    return Instant.now().toString();
  }

  @GetMapping("/transcribe")
  public String transcribe(
          @RequestParam(name = "language", defaultValue = "en-US") language,
          @RequestParam(name = "parent", required = false) Long parent,
          String filename, String language) {

  }

  @GetMapping("/test")
  public String test() throws Exception {
    SpeechClient speech = SpeechClient.create();

    String googleUri = "gs://speech-to-test-sample/fabi.wav"; // "test.mp3"

//    byte[] data = new ClassPathResource(filename)
//            .getInputStream()
//            .readAllBytes();

//    System.out.println("Found file - Starting recognition");

    RecognitionAudio recognitionAudio =
            RecognitionAudio.newBuilder()
                    .setUri(googleUri)
//                    .setContent(ByteString.copyFrom(data))
                    .build();

    RecognitionConfig config =
            RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                    .setLanguageCode("es-CL")
                    .setSampleRateHertz(44100)
//                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                    .setLanguageCode("en-US")
//                    .setSampleRateHertz(8000)
                    .setEnableAutomaticPunctuation(true)
                    .build();

    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> recognizeResponse =
            speech.longRunningRecognizeAsync(config, recognitionAudio);
    System.out.println("Found file - Starting recognition");

    while (!recognizeResponse.isDone()) {
      System.out.println("Waiting for response...");
      Thread.sleep(10000);
    }


    List<SpeechRecognitionResult> recognizeResults = recognizeResponse.get().getResultsList();

    StringBuilder sb = new StringBuilder();
    for (SpeechRecognitionResult result : recognizeResults) {
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      sb.append(alternative.getTranscript());
      sb.append('\n');
    }

    String output = sb.toString();
    System.out.println(output);
    return output;
  }

}

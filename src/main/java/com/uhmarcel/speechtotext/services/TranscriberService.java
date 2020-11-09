package com.uhmarcel.speechtotext.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.*;
import org.springframework.stereotype.Service;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class TranscriberService {

    private FileStorageService fileStorageService;

    public TranscriberService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String transcribeFile(String filename, String language) throws UnsupportedAudioFileException, IOException, ExecutionException, InterruptedException {
        String resourceUri = fileStorageService.getApplicationBucketUri() + filename;
        RecognitionConfig.AudioEncoding encoding = getAudioEncoding(filename);
        Integer sampleRate = 44100;

        SpeechClient speech = SpeechClient.create();

        RecognitionAudio recognitionAudio = buildRecognitionAudio(resourceUri);
        RecognitionConfig recognitionConfig = buildRecognitionConfig(encoding, language, sampleRate);

        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> recognizeResponse =
            speech.longRunningRecognizeAsync(recognitionConfig, recognitionAudio);

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
        speech.close();

        String output = sb.toString();
        System.out.println(output);
        return output;
    }

    private RecognitionConfig buildRecognitionConfig(RecognitionConfig.AudioEncoding encoding, String language, Integer sampleRate) {
        RecognitionConfig.Builder builder =
            RecognitionConfig.newBuilder()
                .setLanguageCode(language)
                .setSampleRateHertz(sampleRate)
                .setEnableAutomaticPunctuation(true);

        if (encoding != null && encoding != RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED) {
            builder.setEncoding(encoding);
        }

        return builder.build();
    }

    private RecognitionAudio buildRecognitionAudio(String resourceUri) {
        RecognitionAudio.Builder builder =
            RecognitionAudio.newBuilder()
                .setUri(resourceUri);

        return builder.build();
    }

    private RecognitionConfig.AudioEncoding getAudioEncoding(String filename) throws UnsupportedAudioFileException {
        String[] tokens = filename.split(".");
        if (tokens.length < 2) {
            throw new UnsupportedAudioFileException("Provided file has no file extension");
        }

        String fileExtension = tokens[tokens.length - 1];

        switch (fileExtension.toLowerCase()) {
            case "wav": return RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED;
            case "flac": return RecognitionConfig.AudioEncoding.FLAC;
            case "mp3": return RecognitionConfig.AudioEncoding.MP3;
        }

        throw new UnsupportedAudioFileException(
            String.format("Unsupported file extension '%s'", fileExtension.toUpperCase())
        );
    }
}

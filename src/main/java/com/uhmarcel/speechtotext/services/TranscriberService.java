package com.uhmarcel.speechtotext.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.cloud.storage.Blob;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class TranscriberService {

    private FileStorageService fileStorageService;

    public TranscriberService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String transcribeFile(MultipartFile file, String language) throws UnsupportedAudioFileException, IOException, ExecutionException, InterruptedException {
        final String filename = file.getOriginalFilename();
        final String resourceUri = fileStorageService.getApplicationBucketUri() + filename;
        final RecognitionConfig.AudioEncoding encoding = getAudioEncoding(filename);
        final Integer sampleRate = getAudioFrameRate(file);

        Blob blob = fileStorageService.uploadFile(file);

        SpeechClient speech = SpeechClient.create();

        RecognitionAudio recognitionAudio = buildRecognitionAudio(resourceUri);
        RecognitionConfig recognitionConfig = buildRecognitionConfig(encoding, language, sampleRate);

        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> recognizeResponse = speech.longRunningRecognizeAsync(recognitionConfig, recognitionAudio);

        System.out.println("Starting recognition");
        while (!recognizeResponse.isDone()) {
            System.out.println("Waiting for response...");
            Thread.sleep(10000);
        }
        System.out.println("Recognition done");

        List<SpeechRecognitionResult> recognizeResults = recognizeResponse.get().getResultsList();

        StringBuilder transcription = new StringBuilder();
        for (SpeechRecognitionResult result : recognizeResults) {
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            transcription.append(alternative.getTranscript());
            transcription.append('\n');
        }

        speech.close();

        String output = transcription.toString();
        System.out.println(output);

        blob.delete();

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
        String[] tokens = filename.split("\\.");
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

    public int getAudioFrameRate(MultipartFile file) throws IOException, UnsupportedAudioFileException {
        BufferedInputStream bufferedStream = new BufferedInputStream(file.getInputStream());
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        return (int) audioFormat.getFrameRate();
    }
}

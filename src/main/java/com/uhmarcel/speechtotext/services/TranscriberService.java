package com.uhmarcel.speechtotext.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedRetryAlgorithm;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.cloud.storage.Blob;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;
import org.threeten.bp.Duration;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class TranscriberService {

    private FileStorageService fileStorageService;

    public TranscriberService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public String transcribeFile(String filename, String resourceUri, RecognitionConfig.AudioEncoding encoding, String language, Integer sampleRate) throws IOException, InterruptedException, ExecutionException {
        SpeechClient speech = buildSpeechClient();
        RecognitionAudio recognitionAudio = buildRecognitionAudio(resourceUri);
        RecognitionConfig recognitionConfig = buildRecognitionConfig(encoding, language, sampleRate);
        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> recognizeResponse =
            speech.longRunningRecognizeAsync(recognitionConfig, recognitionAudio);

        System.out.println("Starting recognition for " + filename);
        StopWatch watch = new StopWatch();
        int counter = 1;

        watch.start();

        while (!recognizeResponse.isDone()) {
            System.out.println(" - Waiting for response... " + counter);
            counter++;
            Thread.sleep(10000);
        }

        watch.stop();

        System.out.println(" - Recognition completed");
        System.out.println(" - Total time: " + (int) watch.getTotalTimeSeconds() + "s");

        List<SpeechRecognitionResult> recognizeResults = recognizeResponse.get().getResultsList();
        StringBuilder transcription = new StringBuilder();
        for (SpeechRecognitionResult result : recognizeResults) {
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            transcription.append(alternative.getTranscript() + '\n');
        }
        speech.close();

        String transcriptionContent = transcription.toString();
        fileStorageService.uploadTextFile(
            String.format("%s-transcript.txt", filename.replaceAll("\\.", "-")),
            transcriptionContent
        );
        return transcriptionContent;
    }

//    public String transcribeFileV2(MultipartFile file, String language) throws UnsupportedAudioFileException, IOException, ExecutionException, InterruptedException {
//        final String filename = file.getOriginalFilename();
//        final String resourceUri = fileStorageService.getApplicationBucketUri() + filename;
//        final RecognitionConfig.AudioEncoding encoding = getAudioEncoding(filename);
//        final Integer sampleRate = getAudioFrameRate(file);
//
//        Blob blob = fileStorageService.uploadMultipartFile(file);
//
//        SpeechClient speech = SpeechClient.create();
//
//        RecognitionAudio recognitionAudio = buildRecognitionAudio(resourceUri);
//        RecognitionConfig recognitionConfig = buildRecognitionConfig(encoding, language, sampleRate);
//
//        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> recognizeResponse = speech.longRunningRecognizeAsync(recognitionConfig, recognitionAudio);
//
//        System.out.println("Starting recognition for " + filename);
//        StopWatch watch = new StopWatch();
//        int counter = 1;
//
//        watch.start();
//        while (!recognizeResponse.isDone()) {
//            System.out.println(" - Waiting for response... " + counter);
//            Thread.sleep(10000);
//            counter++;
//        }
//        watch.stop();
//
//        System.out.println(" - Recognition completed");
//        System.out.println(" - Total time: " + (int) watch.getTotalTimeSeconds() + "s");
//
//        List<SpeechRecognitionResult> recognizeResults = recognizeResponse.get().getResultsList();
//
//        StringBuilder transcription = new StringBuilder();
//        for (SpeechRecognitionResult result : recognizeResults) {
//            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//            transcription.append(alternative.getTranscript() + '\n');
//        }
//        speech.close();
//
//        String transcriptionContent = transcription.toString();
//        fileStorageService.uploadTextFile(
//            String.format("%s-transcript.txt", filename.replaceAll("\\.", "-")),
//            transcriptionContent
//        );
//        blob.delete();
//
//        return transcriptionContent;
//    }

    public int getAudioFrameRate(MultipartFile file) throws IOException, UnsupportedAudioFileException {
        BufferedInputStream bufferedStream = new BufferedInputStream(file.getInputStream());
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        return (int) audioFormat.getFrameRate();
    }

    public RecognitionConfig.AudioEncoding getAudioEncoding(String filename) throws UnsupportedAudioFileException {
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

    private SpeechClient buildSpeechClient() throws IOException {
        SpeechSettings.Builder speechSettingsBuilder = SpeechSettings.newBuilder();

        TimedRetryAlgorithm timedRetryAlgorithm = OperationTimedPollAlgorithm.create(
            RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.ofMillis(500L))
                .setRetryDelayMultiplier(1.5)
                .setMaxRetryDelay(Duration.ofMillis(5000L))
                .setInitialRpcTimeout(Duration.ZERO)
                .setRpcTimeoutMultiplier(1.0)
                .setMaxRpcTimeout(Duration.ZERO)
                .setTotalTimeout(Duration.ofMillis(14400000L)) // Override to 4hrs
                .build());

        speechSettingsBuilder
            .longRunningRecognizeOperationSettings()
            .setPollingAlgorithm(timedRetryAlgorithm);

        return SpeechClient.create(speechSettingsBuilder.build());
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

}

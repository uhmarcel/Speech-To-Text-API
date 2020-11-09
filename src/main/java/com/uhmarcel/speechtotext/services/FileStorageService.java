package com.uhmarcel.speechtotext.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileStorageService {

    private String applicationBucketUri;
    private Storage storage;
    private Bucket bucket;

    public FileStorageService(@Value("${vars.gcs-application-bucket}") String applicationBucket) {
        this.applicationBucketUri = String.format("gs://%s/", applicationBucket);
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = storage.get(applicationBucket);
    }

    public String getApplicationBucketUri() {
        return applicationBucketUri;
    }

    public Blob uploadMultipartFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        byte[] content = file.getBytes();
        return bucket.create(filename, content);
    }

    public Blob uploadTextFile(String filename, String content) {
        return bucket.create(filename, content.getBytes());
    }

}

package com.uhmarcel.speechtotext.services;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private String applicationBucketUri;
    private Storage storage;
    private Bucket bucket;

    public FileStorageService(@Value("${vars.gcs-application-bucket}") String applicationBucketUri) {
        this.applicationBucketUri = applicationBucketUri;
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = storage.get(applicationBucketUri);
    }

    public String getApplicationBucketUri() {
        return applicationBucketUri;
    }

    public Bucket getBucket() {
        return bucket;
    }
}

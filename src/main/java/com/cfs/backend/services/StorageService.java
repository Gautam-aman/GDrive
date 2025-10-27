package com.cfs.backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String uploadFile (MultipartFile file , Long userId) throws IOException {
        String storagePath = "user-"+ userId +"/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest , RequestBody.fromInputStream(file.getInputStream() ,file.getSize()));
        return storagePath;
    }

}

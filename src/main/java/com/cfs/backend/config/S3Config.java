package com.cfs.backend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;


@Configuration
public class S3Config {

    @Value("${minio.url:http://127.0.0.1:9000}")
    private String minioUrl;

    @Value("${minio.access.key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret.key:minioadmin}")
    private String secretKey;

    @Bean
    public S3Client getS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(minioUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey,secretKey)
                ))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    // Create temporary download links
    @Bean
    public S3Presigner getS3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(minioUrl))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey,secretKey)
                ))
                .region(Region.US_EAST_1)
                .build();
    }


}

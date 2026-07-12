package com.websocial.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3StorageService {

    private final String bucketName;
    private final Region region;
    private final S3Client s3Client;

    public S3StorageService(@Value("${aws.s3.bucket}") String bucketName,
                            @Value("${aws.region}") String regionName) {
        this.bucketName = bucketName;
        this.region = Region.of(regionName);
        this.s3Client = S3Client.builder()
                .region(this.region)
                .build();
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        String normalizedFolder = folder == null ? "uploads" : folder.replaceAll("^/|/$", "");
        String key = normalizedFolder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return "https://" + bucketName + ".s3." + region.id() + ".amazonaws.com/" + key;
    }
}

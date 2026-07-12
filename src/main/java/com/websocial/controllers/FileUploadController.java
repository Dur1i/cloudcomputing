package com.websocial.controllers;

import com.websocial.services.S3StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private S3StorageService s3StorageService;

    @PostMapping("/")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(s3StorageService.upload(file, "uploads"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed");
        }
    }
}
package com.websocial.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    // Thư mục lưu trữ ảnh (Bạn có thể đổi đường dẫn này tùy theo máy ảo / máy thật của bạn)
    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename(); 
            
            // Tạo thư mục nếu chưa có
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());
            
            return ResponseEntity.ok("File đã được tải lên tại: " + filePath.toString());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Lỗi khi tải file lên server!");
        }
    }
}
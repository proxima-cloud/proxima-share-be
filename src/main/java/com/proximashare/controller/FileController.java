package com.proximashare.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

// import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Resource;
import com.proximashare.dto.FileMetadataResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.proximashare.entity.FileMetadata;
import com.proximashare.service.FileService;

@RestController
@RequestMapping("/api/public/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is missing"));
        }

        FileMetadata metadata = fileService.uploadFile(file);

        Map<String, String> response = Map.of(
                "uuid", metadata.getUuid()
                //, "link", "http://localhost:8080/api/files/download/" + metadata.getUuid()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable String uuid) throws FileNotFoundException {
        FileMetadata metadata = fileService.getFileMetadata(uuid);
        return ResponseEntity.ok(FileMetadataResponse.from(metadata));
    }

    @GetMapping("/download/{uuid}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String uuid) throws FileNotFoundException, IllegalAccessException {
        File file = fileService.downloadFile(uuid);
        FileMetadata metadata = fileService.getFileMetadata(uuid);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + metadata.getFilename() + "\"")
                .body(new FileSystemResource(file));
    }
}

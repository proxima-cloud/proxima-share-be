package com.proximashare.controller;

import com.proximashare.entity.FileMetadata;
import com.proximashare.service.FileService;

import java.io.File;
import java.util.Map;

// import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {
  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
    FileMetadata metadata = fileService.uploadFile(file);

    Map<String, String> response = Map.of(
        "uuid", metadata.getUuid()
        //, "link", "http://localhost:8080/api/files/download/" + metadata.getUuid()
      );

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{uuid}")
  public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable String uuid) {
    return ResponseEntity.ok(fileService.getFileMetadata(uuid));
  }

  @GetMapping("/download/{uuid}")
  public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String uuid) {
    File file = fileService.downloadFile(uuid);
    FileMetadata metadata = fileService.getFileMetadata(uuid);

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"" + metadata.getFilename() + "\"")
        .body(new FileSystemResource(file));
  }
}

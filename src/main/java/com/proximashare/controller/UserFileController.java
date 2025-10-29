package com.proximashare.controller;

import com.proximashare.dto.FileMetadataResponse;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.User;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/files")
public class UserFileController {
    private final FileService fileService;
    private final UserRepository userRepository;

    public UserFileController(FileService fileService, UserRepository userRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is missing"));
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        FileMetadata metadata = fileService.uploadFileForUser(file, user);

        Map<String, String> response = Map.of(
                "uuid", metadata.getUuid(),
                "message", "File uploaded successfully"
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> getUserFiles(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<FileMetadata> files = fileService.getUserFiles(user);

        // Convert to DTO
        List<FileMetadataResponse> response = files.stream()
                .map(FileMetadataResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(
            @PathVariable String uuid) throws FileNotFoundException {

        FileMetadata metadata = fileService.getFileMetadata(uuid);
        return ResponseEntity.ok(FileMetadataResponse.from(metadata));
    }

    @GetMapping("/download/{uuid}")
    public ResponseEntity<FileSystemResource> downloadFile(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws FileNotFoundException, IllegalAccessException, UsernameNotFoundException {

        File file = fileService.downloadFile(uuid);
        FileMetadata metadata = fileService.getFileMetadata(uuid);
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!(metadata.getOwner().equals(user) || metadata.isPublic())) {
            throw new IllegalAccessException("Not authorized to download this file");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + metadata.getFilename() + "\"")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable String uuid,
            @AuthenticationPrincipal UserDetails userDetails) throws FileNotFoundException, IllegalAccessException {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        fileService.deleteUserFile(uuid, user);

        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }
}
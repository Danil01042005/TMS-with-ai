package ru.tms.user.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.tms.user.controller.dto.PresignedUrlResponse;
import ru.tms.user.controller.dto.UserFilePageResponse;
import ru.tms.user.controller.dto.UserFileResponse;
import ru.tms.user.controller.support.OwnerIdResolver;
import ru.tms.user.service.UserFileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks-service")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class FileController {

    private final UserFileService userFileService;
    private final OwnerIdResolver ownerIdResolver;

    @PostMapping(
            value = "/banks/{bankId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserFileResponse> uploadBankFile(
            @PathVariable Long bankId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);

        UserFileResponse response = userFileService.uploadToBank(ownerId, bankId, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/tests/{testId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserFileResponse> uploadTestFile(
            @PathVariable Long testId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);
        UserFileResponse response = userFileService.uploadToTest(ownerId, testId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/banks/{bankId}/files")
    public ResponseEntity<UserFilePageResponse> listBankFiles(
            @PathVariable Long bankId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                UserFilePageResponse.from(userFileService.listBankFiles(ownerId, bankId, pageable)));
    }

    @GetMapping("/tests/{testId}/files")
    public ResponseEntity<UserFilePageResponse> listTestFiles(
            @PathVariable Long testId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                UserFilePageResponse.from(userFileService.listTestFiles(ownerId, testId, pageable)));
    }

    @GetMapping("/files/{fileId}/download-url")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable Long fileId,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);
        return ResponseEntity.ok(userFileService.getDownloadUrl(ownerId, fileId));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Map<String, Boolean>> deleteFile(
            @PathVariable Long fileId,
            JwtAuthenticationToken authentication
    ) {
        String ownerId = ownerIdResolver.resolve(authentication);
        userFileService.deleteFile(ownerId, fileId);
        // 200 + JSON вместо 204: часть шлюзов/клиентов некорректно обрабатывает No Content на DELETE.
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}


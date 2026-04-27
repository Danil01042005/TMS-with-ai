package ru.tms.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.tms.user.controller.dto.PresignedUrlResponse;
import ru.tms.user.controller.dto.UserFileResponse;
import ru.tms.user.entity.QuestionBank;
import ru.tms.user.entity.TestEntity;
import ru.tms.user.entity.UserFileEntity;
import ru.tms.user.repository.QuestionBankRepository;
import ru.tms.user.repository.TestRepository;
import ru.tms.user.repository.UserFileRepository;
import ru.tms.user.service.exception.NotFoundOrAccessDeniedException;
import ru.tms.user.service.storage.S3StorageService;

@Service
@RequiredArgsConstructor
public class UserFileService {

    private final UserFileRepository userFileRepository;
    private final QuestionBankRepository questionBankRepository;
    private final TestRepository testRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public UserFileResponse uploadToBank(String ownerId, Long bankId, MultipartFile file) {
        // Проверяем доступ к банку.
        QuestionBank bank = questionBankRepository.findByBankIdAndOwnerId(bankId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("Question bank not found or access denied"));

        String objectKey = s3StorageService.upload(file, ownerId);

        UserFileEntity entity = new UserFileEntity();
        entity.setOwnerId(ownerId);
        entity.setBankId(bank.getBankId());
        entity.setTestId(null);
        entity.setObjectKey(objectKey);
        entity.setOriginalFileName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setFileSizeBytes(file.getSize());

        UserFileEntity saved = userFileRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public UserFileResponse uploadToTest(String ownerId, Long testId, MultipartFile file) {
        TestEntity test = testRepository.findByTestIdAndOwnerId(testId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("Test not found or access denied"));

        String objectKey = s3StorageService.upload(file, ownerId);

        UserFileEntity entity = new UserFileEntity();
        entity.setOwnerId(ownerId);
        entity.setBankId(null);
        entity.setTestId(test.getTestId());
        entity.setObjectKey(objectKey);
        entity.setOriginalFileName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setFileSizeBytes(file.getSize());

        UserFileEntity saved = userFileRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserFileResponse> listBankFiles(String ownerId, Long bankId, Pageable pageable) {
        return userFileRepository.findByOwnerIdAndBankId(ownerId, bankId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserFileResponse> listTestFiles(String ownerId, Long testId, Pageable pageable) {
        return userFileRepository.findByOwnerIdAndTestId(ownerId, testId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse getDownloadUrl(String ownerId, Long fileId) {
        UserFileEntity entity = userFileRepository.findByFileIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("File not found or access denied"));

        String url = s3StorageService.presignedGetUrl(entity.getObjectKey());
        return new PresignedUrlResponse(url);
    }

    @Transactional
    public void deleteFile(String ownerId, Long fileId) {
        UserFileEntity entity = userFileRepository.findByFileIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new NotFoundOrAccessDeniedException("File not found or access denied"));

        // Удаляем в S3, затем очищаем метаданные.
        s3StorageService.deleteObject(entity.getObjectKey());
        userFileRepository.delete(entity);
    }

    private UserFileResponse toResponse(UserFileEntity entity) {
        return new UserFileResponse(
                entity.getFileId(),
                entity.getBankId(),
                entity.getTestId(),
                entity.getOriginalFileName(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getCreatedDate()
        );
    }
}


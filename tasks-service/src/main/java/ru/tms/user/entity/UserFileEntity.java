package ru.tms.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_files")
@Getter
@Setter
public class UserFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "object_key", nullable = false, unique = true, length = 1024)
    private String objectKey;

    @Column(name = "original_file_name", nullable = false, length = 1024)
    private String originalFileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @PrePersist
    private void prePersist() {
        if (createdDate == null) {
            createdDate = Instant.now();
        }
    }
}


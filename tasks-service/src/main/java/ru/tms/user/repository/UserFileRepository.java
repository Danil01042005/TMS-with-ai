package ru.tms.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tms.user.entity.UserFileEntity;

import java.util.Optional;

@Repository
public interface UserFileRepository extends JpaRepository<UserFileEntity, Long> {

    Optional<UserFileEntity> findByFileIdAndOwnerId(Long fileId, String ownerId);

    Page<UserFileEntity> findByOwnerIdAndBankId(String ownerId, Long bankId, Pageable pageable);

    Page<UserFileEntity> findByOwnerIdAndTestId(String ownerId, Long testId, Pageable pageable);
}


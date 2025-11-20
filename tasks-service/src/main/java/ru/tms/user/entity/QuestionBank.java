package ru.tms.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_banks")
@Getter
@Setter
@ToString(exclude = "tests")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_date")
    private LocalDate createdDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @OneToMany(mappedBy = "questionBank", fetch = FetchType.LAZY)
    private List<TestEntity> tests = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
    }
}

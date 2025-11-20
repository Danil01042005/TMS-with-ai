package ru.tms.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.tms.user.entity.enums.DifficultyLevel;
import ru.tms.user.entity.enums.TestStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Getter
@Setter
@ToString(exclude = {"questionBank", "questions"})
@AllArgsConstructor
@NoArgsConstructor
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Long testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private QuestionBank questionBank;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private DifficultyLevel difficulty;

    @Column(name = "time_limit")
    private Integer timeLimit;

    @Column(name = "num_questions")
    private Integer numQuestions;

    @Column(name = "attempts")
    private Integer attempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TestStatus status;

    @Column(name = "created_date")
    private LocalDate createdDate;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionEntity> questions = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
        if (status == null) {
            status = TestStatus.DRAFT;
        }
        if (ownerId == null && questionBank != null) {
            ownerId = questionBank.getOwnerId();
        }
    }
}

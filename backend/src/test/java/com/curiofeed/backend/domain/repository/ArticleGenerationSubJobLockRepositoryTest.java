package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.config.JpaConfig;
import com.curiofeed.backend.domain.entity.ArticleGenerationJob;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ArticleGenerationSubJobLockRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private ArticleGenerationJobRepository jobRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ArticleGenerationJob job;

    @BeforeEach
    void setUp() {
        job = new ArticleGenerationJob(UUID.randomUUID(), JobStatus.PENDING);
        entityManager.persistAndFlush(job);
    }

    @Test
    @DisplayName("PENDING 상태 SubJob에 tryLock → 1 반환, DB 상태 PROCESSING 확인")
    void tryLock_pendingSubJob_returnsOne() {
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        subJobRepository.saveAndFlush(subJob);
        entityManager.clear();

        int updated = subJobRepository.tryLockSubJob(subJob.getId());

        assertThat(updated).isEqualTo(1);
        entityManager.clear();
        ArticleGenerationSubJob found = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(JobStatus.PROCESSING);
    }

    @Test
    @DisplayName("PROCESSING 상태 SubJob에 tryLock → 0 반환, 상태 변경 없음")
    void tryLock_processingSubJob_returnsZero() {
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.MEDIUM, JobStatus.PROCESSING);
        subJobRepository.saveAndFlush(subJob);
        entityManager.clear();

        int updated = subJobRepository.tryLockSubJob(subJob.getId());

        assertThat(updated).isEqualTo(0);
        entityManager.clear();
        ArticleGenerationSubJob found = subJobRepository.findById(subJob.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(JobStatus.PROCESSING);
    }

    @Test
    @DisplayName("COMPLETED 상태 SubJob에 tryLock → 0 반환")
    void tryLock_completedSubJob_returnsZero() {
        // 생성자를 통해 COMPLETED 상태로 직접 생성 (테스트 셋업용)
        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.HARD, JobStatus.COMPLETED);
        subJobRepository.saveAndFlush(subJob);
        entityManager.clear();

        int updated = subJobRepository.tryLockSubJob(subJob.getId());

        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 id로 tryLock → 0 반환")
    void tryLock_nonExistentId_returnsZero() {
        int updated = subJobRepository.tryLockSubJob(UUID.randomUUID());
        assertThat(updated).isEqualTo(0);
    }
}

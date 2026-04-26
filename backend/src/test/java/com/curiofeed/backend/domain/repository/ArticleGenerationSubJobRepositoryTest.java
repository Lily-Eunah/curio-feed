package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.config.JpaConfig;
import com.curiofeed.backend.domain.entity.ArticleGenerationJob;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.entity.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ArticleGenerationSubJobRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ArticleGenerationSubJobRepository subJobRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("SubJob을 정상적으로 저장하고 조회할 수 있다")
    void saveAndFindSubJob() {
        // given
        ArticleGenerationJob job = new ArticleGenerationJob(UUID.randomUUID(), JobStatus.PENDING);
        entityManager.persistAndFlush(job);

        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        
        // when
        ArticleGenerationSubJob savedSubJob = subJobRepository.save(subJob);
        entityManager.flush();
        entityManager.clear();

        // then
        ArticleGenerationSubJob foundSubJob = subJobRepository.findById(savedSubJob.getId()).orElseThrow();
        assertThat(foundSubJob.getLevel()).isEqualTo(DifficultyLevel.EASY);
        assertThat(foundSubJob.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(foundSubJob.getRetryCount()).isEqualTo(0);
        assertThat(foundSubJob.getJob().getId()).isEqualTo(job.getId());
        assertThat(foundSubJob.getCreatedAt()).isNotNull();
        assertThat(foundSubJob.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("SubJob은 동일한 Job의 동일한 Level에 대해 Unique 속성을 가진다")
    void uniqueConstraintJobIdAndLevel() {
        // given
        ArticleGenerationJob job = new ArticleGenerationJob(UUID.randomUUID(), JobStatus.PENDING);
        entityManager.persistAndFlush(job);

        ArticleGenerationSubJob subJob1 = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PENDING);
        subJobRepository.saveAndFlush(subJob1);
        entityManager.clear();

        // when & then
        ArticleGenerationSubJob subJob2 = new ArticleGenerationSubJob(job, DifficultyLevel.EASY, JobStatus.PROCESSING);
        
        assertThatThrownBy(() -> subJobRepository.saveAndFlush(subJob2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("SubJob의 상태와 Heartbeat를 정상적으로 업데이트할 수 있다")
    void updateSubJobFields() {
        // given
        ArticleGenerationJob job = new ArticleGenerationJob(UUID.randomUUID(), JobStatus.PENDING);
        entityManager.persistAndFlush(job);

        ArticleGenerationSubJob subJob = new ArticleGenerationSubJob(job, DifficultyLevel.HARD, JobStatus.PENDING);
        ArticleGenerationSubJob savedSubJob = subJobRepository.saveAndFlush(subJob);
        
        // when
        savedSubJob.updateStatus(JobStatus.PROCESSING);
        savedSubJob.incrementRetryCount();
        Instant now = Instant.now();
        savedSubJob.updateHeartbeat(now);
        entityManager.flush();
        entityManager.clear();
        
        // then
        ArticleGenerationSubJob foundSubJob = subJobRepository.findById(savedSubJob.getId()).orElseThrow();
        assertThat(foundSubJob.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(foundSubJob.getRetryCount()).isEqualTo(1);
        assertThat(foundSubJob.getLastHeartbeatAt()).isNotNull();
    }
}

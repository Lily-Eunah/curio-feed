package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubJobLockService {

    private final ArticleGenerationSubJobRepository subJobRepository;

    public SubJobLockService(ArticleGenerationSubJobRepository subJobRepository) {
        this.subJobRepository = subJobRepository;
    }

    /**
     * SubJob을 PENDING → PROCESSING으로 원자적으로 전이.
     *
     * @return true: lock 획득 성공 / false: 이미 다른 상태 (다른 Worker가 처리 중)
     */
    @Transactional
    public boolean tryLock(UUID subJobId) {
        return subJobRepository.tryLockSubJob(subJobId) == 1;
    }
}

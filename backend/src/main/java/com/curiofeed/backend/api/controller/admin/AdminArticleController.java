package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.api.dto.admin.GenerationStatusResponse;
import com.curiofeed.backend.api.dto.admin.RegisterArticleRequest;
import com.curiofeed.backend.api.dto.admin.RegisterArticleResponse;
import com.curiofeed.backend.domain.entity.ArticleGenerationSubJob;
import com.curiofeed.backend.domain.entity.JobStatus;
import com.curiofeed.backend.domain.repository.ArticleGenerationJobRepository;
import com.curiofeed.backend.domain.repository.ArticleGenerationSubJobRepository;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.domain.service.ArticleIngestionService;
import com.curiofeed.backend.domain.service.ArticleIngestionService.ArticleAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleIngestionService ingestionService;
    private final ArticleRepository articleRepository;
    private final ArticleGenerationJobRepository jobRepository;
    private final ArticleGenerationSubJobRepository subJobRepository;

    public AdminArticleController(
            ArticleIngestionService ingestionService,
            ArticleRepository articleRepository,
            ArticleGenerationJobRepository jobRepository,
            ArticleGenerationSubJobRepository subJobRepository) {
        this.ingestionService = ingestionService;
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subJobRepository = subJobRepository;
    }

    /**
     * 엔드포인트 1: 기사 등록 + Job 생성 트리거
     */
    @PostMapping
    public ResponseEntity<?> registerArticle(@RequestBody RegisterArticleRequest request) {
        try {
            RegisterArticleResponse response = ingestionService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ArticleAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Article already exists", "articleId", e.getArticleId()));
        }
    }

    /**
     * 엔드포인트 2: Job 상태 조회
     */
    @GetMapping("/{articleId}/generation-status")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGenerationStatus(@PathVariable UUID articleId) {
        return articleRepository.findById(articleId)
                .map(article -> {
                    var jobOpt = jobRepository.findByArticleId(articleId);
                    GenerationStatusResponse.JobInfo jobInfo = jobOpt.map(job -> {
                        List<GenerationStatusResponse.SubJobInfo> subJobInfos = subJobRepository
                                .findByJobId(job.getId()).stream()
                                .map(s -> new GenerationStatusResponse.SubJobInfo(
                                        s.getLevel(), s.getStatus(), s.getRetryCount()))
                                .toList();
                        return new GenerationStatusResponse.JobInfo(job.getId(), subJobInfos);
                    }).orElse(null);

                    return ResponseEntity.ok(new GenerationStatusResponse(
                            article.getId(), article.getStatus(), jobInfo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 엔드포인트 3: SubJob 수동 재시도
     */
    @PostMapping("/{articleId}/generation-jobs/{jobId}/sub-jobs/{subJobId}/retry")
    @Transactional
    public ResponseEntity<?> retrySubJob(
            @PathVariable UUID articleId,
            @PathVariable UUID jobId,
            @PathVariable UUID subJobId) {

        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId)
                .orElse(null);

        if (subJob == null) {
            return ResponseEntity.notFound().build();
        }

        if (subJob.getStatus() != JobStatus.FAILED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "SubJob is not in FAILED state"));
        }

        subJobRepository.resetToPendingWithRetryReset(subJobId);
        return ResponseEntity.noContent().build();
    }
}

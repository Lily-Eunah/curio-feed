package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.api.dto.admin.AdminArticleListResponse;
import com.curiofeed.backend.api.dto.admin.GenerationStatusResponse;
import com.curiofeed.backend.api.dto.admin.RegisterArticleRequest;
import com.curiofeed.backend.api.dto.admin.RegisterArticleResponse;
import com.curiofeed.backend.api.dto.admin.UpdateArticleStatusRequest;
import com.curiofeed.backend.domain.entity.*;
import com.curiofeed.backend.domain.repository.*;
import com.curiofeed.backend.domain.service.ArticleIngestionService;
import com.curiofeed.backend.domain.service.ArticleIngestionService.ArticleAlreadyExistsException;
import com.curiofeed.backend.domain.service.StepRetryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ArticleGenerationStepJobRepository stepJobRepository;
    private final StepRetryService stepRetryService;

    public AdminArticleController(
            ArticleIngestionService ingestionService,
            ArticleRepository articleRepository,
            ArticleGenerationJobRepository jobRepository,
            ArticleGenerationSubJobRepository subJobRepository,
            ArticleGenerationStepJobRepository stepJobRepository,
            StepRetryService stepRetryService) {
        this.ingestionService = ingestionService;
        this.articleRepository = articleRepository;
        this.jobRepository = jobRepository;
        this.subJobRepository = subJobRepository;
        this.stepJobRepository = stepJobRepository;
        this.stepRetryService = stepRetryService;
    }

    /**
     * 엔드포인트 0: 기사 목록 조회 (관리자용)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AdminArticleListResponse>> listArticles(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AdminArticleListResponse> result;
        if (status != null && !status.isBlank()) {
            ArticleStatus articleStatus = ArticleStatus.valueOf(status);
            result = articleRepository.findAllByStatus(articleStatus, pageable)
                    .map(article -> new AdminArticleListResponse(
                            article.getId(),
                            article.getSourceTitle(),
                            article.getSourcePublisher(),
                            article.getStatus().name(),
                            article.getCategory().getDisplayName(),
                            article.getCreatedAt()));
        } else {
            result = articleRepository.findAll(pageable)
                    .map(article -> new AdminArticleListResponse(
                            article.getId(),
                            article.getSourceTitle(),
                            article.getSourcePublisher(),
                            article.getStatus().name(),
                            article.getCategory().getDisplayName(),
                            article.getCreatedAt()));
        }

        return ResponseEntity.ok(result);
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
     * 엔드포인트: 기사 상세 조회 (관리자용)
     */
    @GetMapping("/{articleId}")
    @Transactional(readOnly = true)
    public ResponseEntity<com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse> getArticleDetail(@PathVariable("articleId") UUID articleId) {
        return articleRepository.findById(articleId)
                .map(article -> {
                    var jobOpt = jobRepository.findByArticleId(articleId);
                    var jobInfo = jobOpt.map(job -> new com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse.JobInfo(job.getId(), job.getStatus().name())).orElse(null);

                    List<com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse.ContentInfo> contents = article.getContents().stream()
                            .map(c -> new com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse.ContentInfo(
                                    c.getId(),
                                    c.getLevel(),
                                    c.getContent(),
                                    c.getAudioUrl(),
                                    c.getVocabularies().stream().map(v -> new com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse.VocabInfo(v.getId(), v.getWord(), v.getDefinition(), v.getExampleSentence())).toList(),
                                    c.getQuizzes().stream().map(q -> new com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse.QuizInfo(q.getId(), q.getQuestion(), q.getType().name(), q.getOptions(), q.getCorrectAnswer(), q.getExplanation())).toList()
                            )).toList();

                    return ResponseEntity.ok(new com.curiofeed.backend.api.dto.admin.AdminArticleDetailResponse(
                            article.getId(),
                            article.getStatus(),
                            article.getTitle(),
                            article.getSourceTitle(),
                            article.getSourcePublisher(),
                            article.getSourceUrl(),
                            article.getCategory() != null ? article.getCategory().getId().toString() : null,
                            article.getCategory() != null ? article.getCategory().getDisplayName() : null,
                            article.getOriginalContent(),
                            article.getCreatedAt(),
                            article.getPublishedAt(),
                            jobInfo,
                            contents
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 엔드포인트 2: Job 상태 조회 (step-level 포함)
     */
    @GetMapping("/{articleId}/generation-status")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGenerationStatus(@PathVariable("articleId") UUID articleId) {
        return articleRepository.findById(articleId)
                .map(article -> {
                    var jobOpt = jobRepository.findByArticleId(articleId);
                    GenerationStatusResponse.JobInfo jobInfo = jobOpt.map(job -> {
                        List<GenerationStatusResponse.SubJobInfo> subJobInfos = subJobRepository
                                .findByJobId(job.getId()).stream()
                                .map(s -> {
                                    List<GenerationStatusResponse.StepJobInfo> steps =
                                            stepJobRepository.findBySubJobIdOrderByStepType(s.getId())
                                                    .stream()
                                                    .map(step -> new GenerationStatusResponse.StepJobInfo(
                                                            step.getId(),
                                                            step.getStepType(),
                                                            step.getStatus(),
                                                            step.getAttemptCount(),
                                                            step.getStartedAt(),
                                                            step.getCompletedAt(),
                                                            step.getValidationStatus(),
                                                            step.getValidationErrors(),
                                                            step.getErrorMessage()))
                                                    .toList();
                                    return new GenerationStatusResponse.SubJobInfo(
                                            s.getId(), s.getLevel(), s.getStatus(),
                                            s.getRetryCount(), s.getLastHeartbeatAt(), steps);
                                })
                                .toList();
                        return new GenerationStatusResponse.JobInfo(job.getId(), subJobInfos);
                    }).orElse(null);

                    return ResponseEntity.ok(new GenerationStatusResponse(
                            article.getId(), article.getStatus(), jobInfo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 엔드포인트 3: SubJob 수동 재시도 (전체 레벨 재시도)
     */
    @PostMapping("/{articleId}/generation-jobs/{jobId}/sub-jobs/{subJobId}/retry")
    @Transactional
    public ResponseEntity<?> retrySubJob(
            @PathVariable("articleId") UUID articleId,
            @PathVariable("jobId") UUID jobId,
            @PathVariable("subJobId") UUID subJobId) {

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

    /**
     * 엔드포인트 3b: 특정 Step 재시도 (step-level retry)
     * stepType: CONTENT | VOCABULARY | QUIZ
     * - CONTENT retry: clears VOCAB and QUIZ steps too
     * - VOCABULARY retry: clears QUIZ step
     * - QUIZ retry: resets QUIZ step only
     */
    @PostMapping("/{articleId}/generation-jobs/{jobId}/sub-jobs/{subJobId}/steps/{stepType}/retry")
    public ResponseEntity<?> retryStep(
            @PathVariable("articleId") UUID articleId,
            @PathVariable("jobId") UUID jobId,
            @PathVariable("subJobId") UUID subJobId,
            @PathVariable("stepType") String stepType) {

        GenerationStepType step;
        try {
            step = GenerationStepType.valueOf(stepType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Unknown step type: " + stepType));
        }

        ArticleGenerationSubJob subJob = subJobRepository.findById(subJobId).orElse(null);
        if (subJob == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            stepRetryService.retryStep(subJob, step);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Allowed status transitions ────────────────────────────────────────

    private record Transition(ArticleStatus from, ArticleStatus to) {}

    private static final java.util.Set<Transition> ALLOWED_TRANSITIONS = java.util.Set.of(
            new Transition(ArticleStatus.DRAFT, ArticleStatus.PUBLISHED),
            new Transition(ArticleStatus.PUBLISHED, ArticleStatus.ARCHIVED),
            new Transition(ArticleStatus.ARCHIVED, ArticleStatus.PUBLISHED)
    );

    /**
     * 엔드포인트 4: 기사 상태 변경 (Publish / Hide)
     */
    @PatchMapping("/{articleId}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(
            @PathVariable("articleId") UUID articleId,
            @RequestBody UpdateArticleStatusRequest request) {

        ArticleStatus newStatus;
        try {
            newStatus = ArticleStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid status: " + request.status()));
        }

        return articleRepository.findById(articleId)
                .map(article -> {
                    Transition t = new Transition(article.getStatus(), newStatus);
                    if (!ALLOWED_TRANSITIONS.contains(t)) {
                        return ResponseEntity.badRequest()
                                .body((Object) Map.of("message",
                                        "Invalid status transition: " + article.getStatus() + " → " + newStatus));
                    }

                    article.updateStatus(newStatus);
                    articleRepository.save(article);

                    return ResponseEntity.ok(Map.of(
                            "articleId", article.getId(),
                            "status", article.getStatus().name()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

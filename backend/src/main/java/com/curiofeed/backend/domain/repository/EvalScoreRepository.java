package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.EvalScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EvalScoreRepository extends JpaRepository<EvalScore, UUID> {
    List<EvalScore> findByArticleContentId(UUID articleContentId);

    @Query("SELECT AVG(e.overall) FROM EvalScore e WHERE e.evaluatorModel = :modelName")
    Double findAverageScoreByModel(@Param("modelName") String modelName);
}

package com.curiofeed.backend.domain.repository;

import com.curiofeed.backend.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
}

package com.curiofeed.backend.domain.service;

public enum SaveStatus {
    NO_CONTENT,           // content 자체가 없음
    FAILED,               // 저장 중 예외 발생 → 전체 롤백
    CONTENT_ONLY,         // content 저장 성공, vocab 없음
    CONTENT_WITH_VOCAB,   // content + vocab 저장 성공, quiz 없음
    FULL_SUCCESS          // content + vocab + quiz 모두 저장 성공
}

package com.aimeeting.interview.interview.service.model;

import java.util.List;

/**
 * AI 出题结果（或题库降级结果）。
 */
public record GeneratedQuestion(
        String title,
        List<String> referencePoints,
        String analysis,
        String difficulty,
        /** AI | BANK */
        String source,
        boolean degraded
) {
}

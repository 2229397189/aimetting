package com.aimeeting.interview.question.domain;

import com.aimeeting.interview.question.api.io.resp.DifficultyOption;
import java.util.Arrays;
import java.util.List;

/**
 * 难度枚举（与前端 DIFFICULTY_OPTIONS 对应）。
 */
public enum Difficulty {
    EASY("简单"),
    MEDIUM("中等"),
    HARD("困难");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static boolean isValid(String name) {
        if (name == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(d -> d.name().equals(name));
    }

    public static List<DifficultyOption> options() {
        return Arrays.stream(values())
                .map(d -> new DifficultyOption(d.name(), d.label, 0L))
                .toList();
    }
}

package com.aimeeting.interview.question.domain;

import com.aimeeting.interview.question.api.io.resp.DirectionOption;
import java.util.Arrays;
import java.util.List;

/**
 * 面试方向枚举（与前端 DIRECTION_OPTIONS 一一对应，8 类）。
 */
public enum Direction {
    JAVA_BACKEND("Java后端"),
    FRONTEND("前端"),
    DATABASE("数据库"),
    OS("操作系统"),
    NETWORK("计算机网络"),
    ALGORITHM("算法"),
    SYSTEM_DESIGN("系统设计"),
    BEHAVIORAL("行为面试");

    private final String label;

    Direction(String label) {
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

    public static List<DirectionOption> options() {
        return Arrays.stream(values())
                .map(d -> new DirectionOption(d.name(), d.label, 0L))
                .toList();
    }
}

package com.aimeeting.interview.ai.log;

import com.aimeeting.interview.ai.dao.mapper.AiCallLogMapper;
import com.aimeeting.interview.ai.guard.AiHealthSnapshot;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 调用日志服务：旁路落库（异步）+ 分页 + 健康快照。
 *
 * <p>请求 / 响应摘要各截断 {@code 512} 字符后入库，避免大段 prompt 写入日志表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    /** 摘要最大长度（与 t_ai_call_log 列宽一致）。 */
    private static final int DIGEST_MAX = 512;

    private final AiCallLogMapper aiCallLogMapper;

    /**
     * 异步记录一次 AI 调用（截断摘要、吞掉入库异常避免影响主流程）。
     *
     * @param log 调用日志
     */
    @Async
    public void record(AiCallLogDO log) {
        if (log == null) {
            return;
        }
        if (log.getRequestDigest() != null && log.getRequestDigest().length() > DIGEST_MAX) {
            log.setRequestDigest(log.getRequestDigest().substring(0, DIGEST_MAX));
        }
        if (log.getResponseDigest() != null && log.getResponseDigest().length() > DIGEST_MAX) {
            log.setResponseDigest(log.getResponseDigest().substring(0, DIGEST_MAX));
        }
        try {
            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("[AiCallLog] 落库失败: {}", e.getMessage());
        }
    }

    /**
     * 分页查询调用日志。
     *
     * @param pageNum  页码（1-based）
     * @param pageSize 页大小
     * @param bizType  业务类型（可空）
     * @param success  是否成功（可空）
     * @return 分页结果
     */
    public IPage<AiCallLogDO> page(long pageNum, long pageSize, String bizType, Boolean success) {
        long pn = pageNum < 1 ? 1 : pageNum;
        long ps = pageSize < 1 ? 10 : Math.min(pageSize, 200);
        LambdaQueryWrapper<AiCallLogDO> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(AiCallLogDO::getBizType, bizType);
        }
        if (success != null) {
            wrapper.eq(AiCallLogDO::getSuccess, success ? 1 : 0);
        }
        wrapper.orderByDesc(AiCallLogDO::getCreateTime);
        return aiCallLogMapper.selectPage(new Page<>(pn, ps), wrapper);
    }

    /**
     * 统计最近窗口内的调用质量：成功率 / 平均耗时 / 失败分布。
     *
     * @param window 统计窗口
     * @return 健康快照（仅成功率 / 平均耗时 / 失败分布 / 总数有值，其余字段由 Guard 补充）
     */
    public AiHealthSnapshot healthSnapshot(Duration window) {
        LocalDateTime since = LocalDateTime.now().minus(window);
        List<AiCallLogDO> recent = aiCallLogMapper.selectList(
                new LambdaQueryWrapper<AiCallLogDO>().ge(AiCallLogDO::getCreateTime, since));
        long total = recent.size();
        long successCount = 0L;
        long costSum = 0L;
        Map<String, Long> failureDistribution = new LinkedHashMap<>();
        for (AiCallLogDO log : recent) {
            if (log.getSuccess() != null && log.getSuccess() == 1) {
                successCount++;
            } else {
                String type = log.getErrorType() == null ? "UNKNOWN" : log.getErrorType();
                failureDistribution.merge(type, 1L, Long::sum);
            }
            if (log.getCostMs() != null) {
                costSum += log.getCostMs();
            }
        }
        Double successRate = total == 0 ? null : (double) successCount / total;
        Long avgCostMs = total == 0 ? null : costSum / total;
        return AiHealthSnapshot.builder()
                .successRate(successRate)
                .avgCostMs(avgCostMs)
                .failureDistribution(failureDistribution)
                .totalCalls(total)
                .build();
    }

    /**
     * 供管理端统一错误码包装（占位，保持与工程其它处一致）。
     *
     * @return 服务名
     */
    public String serviceName() {
        return BaseErrorCode.REMOTE_ERROR.code();
    }
}

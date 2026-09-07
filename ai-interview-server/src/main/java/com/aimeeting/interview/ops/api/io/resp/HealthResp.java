package com.aimeeting.interview.ops.api.io.resp;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查返回体。
 *
 * <p>暴露 DB 连通性、AI provider 与是否 mock 模式，供前端启动自检与运维巡检使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应用状态：UP / DOWN。 */
    private String status;

    /** 数据库状态：UP / DOWN。 */
    private String db;

    /** 当前 AI provider：deepseek / mock。 */
    private String aiProvider;

    /** 是否 mock 模式（无 Key 或 provider=mock）。 */
    private Boolean mockMode;

    /** 应用版本。 */
    private String version;

    /** 检查时间。 */
    private LocalDateTime timestamp;
}

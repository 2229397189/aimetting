-- ============================================================================
-- AI 在线模拟面试平台 - MySQL 8 建表脚本
--
-- 约定：
--   1. 全部使用 CREATE TABLE IF NOT EXISTS，重复启动幂等（spring.sql.init.mode=always）
--   2. 公共列：id / create_time / update_time / deleted（逻辑删除，MyBatis-Plus @TableLogic）
--   3. 时间统一 DATETIME（本地时间），实体用 LocalDateTime
--
-- M1 落盘：t_user / t_user_profile
-- 后续模块按序补充（占位位置已标注）
-- ============================================================================


-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL COMMENT '用户名 4-20',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt(10)',
  email         VARCHAR(128) DEFAULT NULL COMMENT '邮箱（可空）',
  nickname      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  avatar        VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  role          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER|ADMIN',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  last_login_at DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';


-- 2. 用户资料表（与 t_user 一对一）
CREATE TABLE IF NOT EXISTS t_user_profile (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL COMMENT '关联 t_user.id',
  target_position VARCHAR(64)  DEFAULT NULL COMMENT '目标岗位',
  work_years      INT          NOT NULL DEFAULT 0 COMMENT '工作年限',
  intro           VARCHAR(500) DEFAULT NULL COMMENT '自我介绍',
  phone           VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料';


-- 3. 题库表（M2）
CREATE TABLE IF NOT EXISTS t_question (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  direction       VARCHAR(32)  NOT NULL COMMENT '方向枚举名',
  difficulty      VARCHAR(16)  NOT NULL COMMENT 'EASY|MEDIUM|HARD',
  title           VARCHAR(500) NOT NULL COMMENT '题面',
  reference_points TEXT         DEFAULT NULL COMMENT '考察要点(JSON 数组)',
  tags            TEXT         DEFAULT NULL COMMENT '标签(JSON 数组)',
  analysis        TEXT         DEFAULT NULL COMMENT '参考答案/解析',
  source          VARCHAR(16)  NOT NULL DEFAULT 'SEED' COMMENT 'AI|BANK|SEED|ADMIN',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  created_by      BIGINT       DEFAULT NULL COMMENT '录入人',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_direction_difficulty (direction, difficulty),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

-- ============================================================================
-- 以下为后续模块建表占位（由 M3~M8 补充）
--   M4     面试会话：   t_interview_session / t_session_question
--   M5     答题与幂等： t_session_answer / t_idempotent_record
--   M3     AI 日志：    t_ai_call_log
--   M4     状态流水：   t_session_event
--   M5     简历：       t_resume
--   M6     报告：       t_interview_report
-- ============================================================================


-- 3. 简历表（M5）
CREATE TABLE IF NOT EXISTS t_resume (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL,
  title        VARCHAR(128) NOT NULL,
  raw_text     TEXT         DEFAULT NULL COMMENT '原文',
  file_url     VARCHAR(512) DEFAULT NULL COMMENT '文件地址',
  parsed_json  TEXT         DEFAULT NULL COMMENT 'AI 解析结果 JSON',
  score        INT          DEFAULT NULL COMMENT '0-100',
  advantage    TEXT         DEFAULT NULL COMMENT '优势(JSON 数组)',
  suggestions  TEXT         DEFAULT NULL COMMENT '建议(JSON 数组)',
  parsed_by    VARCHAR(16)  NOT NULL DEFAULT 'AI' COMMENT 'AI|RULE',
  is_default   TINYINT(1)   NOT NULL DEFAULT 0,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历';


-- 4. 面试会话主表（M4）
CREATE TABLE IF NOT EXISTS t_interview_session (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  session_no     VARCHAR(32)  NOT NULL COMMENT 'IM+yyyyMMdd+8位随机',
  user_id        BIGINT       NOT NULL,
  resume_id      BIGINT       DEFAULT NULL,
  directions     VARCHAR(255) NOT NULL COMMENT '逗号分隔方向',
  difficulty     VARCHAR(16)  NOT NULL,
  total_question INT          NOT NULL DEFAULT 8,
  current_index  INT          NOT NULL DEFAULT 0 COMMENT '当前题号(1-based)，0=未开始',
  status         VARCHAR(20)  NOT NULL DEFAULT 'INIT',
  prev_status    VARCHAR(20)  DEFAULT NULL COMMENT 'PAUSED 前的状态',
  score          DECIMAL(5,2) DEFAULT NULL COMMENT '总分',
  jd_text        TEXT         DEFAULT NULL COMMENT '目标岗位 JD',
  phase_plan     VARCHAR(255) DEFAULT NULL COMMENT '三阶段题量 JSON: {TECHNICAL:..,PROJECT:..,BEHAVIORAL:..}',
  started_at     DATETIME     DEFAULT NULL,
  finished_at    DATETIME     DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_no (session_no),
  KEY idx_user_status_time (user_id, status, create_time),
  KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话';


-- 5. 会话题目表（M4，冗余题干）
CREATE TABLE IF NOT EXISTS t_session_question (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  session_id       BIGINT       NOT NULL,
  question_no      INT          NOT NULL,
  question_id      BIGINT       DEFAULT NULL COMMENT '来自题库则为 ID，AI 生成则为 NULL',
  title            VARCHAR(1000) NOT NULL,
  reference_points TEXT         DEFAULT NULL COMMENT '考察要点 JSON',
  source           VARCHAR(16)  NOT NULL COMMENT 'AI|BANK',
  difficulty       VARCHAR(16)  NOT NULL,
  phase            VARCHAR(20)  DEFAULT NULL COMMENT 'TECHNICAL|PROJECT|BEHAVIORAL',
  skipped          TINYINT(1)   NOT NULL DEFAULT 0,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session_qno (session_id, question_no),
  KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话题目';


-- 6. 答题记录表（M4，追问以 parent_answer_id 串链）
CREATE TABLE IF NOT EXISTS t_session_answer (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  session_id         BIGINT      NOT NULL,
  session_question_id BIGINT     NOT NULL,
  user_id            BIGINT      NOT NULL,
  content            TEXT        NOT NULL,
  is_follow_up       TINYINT(1)  NOT NULL DEFAULT 0,
  parent_answer_id   BIGINT      DEFAULT NULL COMMENT '追问答案指向原答案',
  score              INT         DEFAULT NULL COMMENT '0-100',
  comment            TEXT        DEFAULT NULL COMMENT '点评(Markdown)',
  highlights         TEXT        DEFAULT NULL COMMENT 'JSON 数组',
  gaps               TEXT        DEFAULT NULL COMMENT 'JSON 数组',
  improved_answer    TEXT        DEFAULT NULL COMMENT '改进后参考答案',
  follow_up_question TEXT        DEFAULT NULL COMMENT 'AI 追问问题',
  authenticity       INT         DEFAULT NULL COMMENT '简历经历真实性/参与度判断 0-100',
  evaluated_by       VARCHAR(16) DEFAULT NULL COMMENT 'AI|RULE',
  follow_up_count    INT         NOT NULL DEFAULT 0,
  skipped            TINYINT(1)  NOT NULL DEFAULT 0,
  client_token       VARCHAR(64) DEFAULT NULL COMMENT '幂等令牌',
  create_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted            TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_session_question (session_id, session_question_id),
  KEY idx_parent (parent_answer_id),
  KEY idx_user (user_id),
  KEY idx_token (session_id, client_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录';


-- 7. AI 调用日志表（M3，脱敏）
CREATE TABLE IF NOT EXISTS t_ai_call_log (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  user_id          BIGINT      DEFAULT NULL,
  biz_type         VARCHAR(32) NOT NULL COMMENT 'QUESTION|EVALUATE|FOLLOW_UP|RESUME|REPORT',
  agent_id         VARCHAR(32) DEFAULT NULL COMMENT '业务 Agent：INTERVIEWER|EVALUATOR|FOLLOW_UP|RESUME_ANALYST|REPORTER',
  provider         VARCHAR(32) NOT NULL,
  model            VARCHAR(64) DEFAULT NULL,
  request_digest   VARCHAR(512) DEFAULT NULL,
  response_digest  VARCHAR(512) DEFAULT NULL,
  prompt_tokens    INT         DEFAULT 0,
  completion_tokens INT        DEFAULT 0,
  cost_ms          BIGINT      DEFAULT 0,
  success          TINYINT(1)  NOT NULL DEFAULT 1,
  error_type       VARCHAR(32) DEFAULT NULL,
  error_msg        VARCHAR(512) DEFAULT NULL,
  request_id       VARCHAR(64) DEFAULT NULL,
  create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_biz_time (biz_type, create_time),
  KEY idx_agent_time (agent_id, create_time),
  KEY idx_user_time (user_id, create_time),
  KEY idx_success (success, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志';


-- 8. 会话状态流水表（M4，审计）
CREATE TABLE IF NOT EXISTS t_session_event (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  session_id  BIGINT      NOT NULL,
  from_status VARCHAR(20) DEFAULT NULL,
  to_status   VARCHAR(20) NOT NULL,
  event       VARCHAR(64) NOT NULL COMMENT 'CREATE|START|SUBMIT|FOLLOW_UP|PAUSE|RESUME|FINISH|ABORT',
  operator    BIGINT      DEFAULT NULL,
  remark      VARCHAR(500) DEFAULT NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_time (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话状态流水';


-- 9. 面试报告表（M6）
CREATE TABLE IF NOT EXISTS t_interview_report (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  session_id      BIGINT        NOT NULL,
  user_id         BIGINT        NOT NULL,
  total_score     DECIMAL(5,2)  NOT NULL DEFAULT 0,
  dimension_json  TEXT         DEFAULT NULL COMMENT '五维 JSON',
  highlights      TEXT         DEFAULT NULL COMMENT 'JSON 数组',
  improvements    TEXT         DEFAULT NULL COMMENT 'JSON 数组',
  actions         TEXT         DEFAULT NULL COMMENT '后续行动 JSON 数组',
  overall_comment TEXT         DEFAULT NULL COMMENT '总评',
  generated_by    VARCHAR(16)   NOT NULL DEFAULT 'AI' COMMENT 'AI|RULE',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_session (session_id),
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试报告';

-- 历史库表增量迁移：仅当列不存在时添加 follow_up_question（MySQL 不支持 ADD COLUMN IF NOT EXISTS）
SET @db = DATABASE();
SET @cnt = (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = @db AND table_name = 't_session_answer' AND column_name = 'follow_up_question');
SET @sql = IF(@cnt = 0, 'ALTER TABLE t_session_answer ADD COLUMN follow_up_question TEXT DEFAULT NULL COMMENT ''AI 追问问题''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史库表增量迁移：仅当列不存在时添加 authenticity
SET @cnt2 = (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = @db AND table_name = 't_session_answer' AND column_name = 'authenticity');
SET @sql2 = IF(@cnt2 = 0, 'ALTER TABLE t_session_answer ADD COLUMN authenticity INT DEFAULT NULL COMMENT ''简历经历真实性/参与度判断 0-100''', 'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 历史库表增量迁移：仅当列不存在时添加 agent_id（M3 按 Agent 维度可观测）
SET @cnt3 = (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = @db AND table_name = 't_ai_call_log' AND column_name = 'agent_id');
SET @sql3 = IF(@cnt3 = 0, 'ALTER TABLE t_ai_call_log ADD COLUMN agent_id VARCHAR(32) DEFAULT NULL COMMENT ''业务 Agent：INTERVIEWER|EVALUATOR|FOLLOW_UP|RESUME_ANALYST|REPORTER''', 'SELECT 1');
PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;


-- 10. 字典表（M8，方向/难度展示）
CREATE TABLE IF NOT EXISTS t_dict (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  type       VARCHAR(64) NOT NULL COMMENT 'direction|difficulty',
  code       VARCHAR(64) NOT NULL,
  label      VARCHAR(128) NOT NULL,
  sort       INT         NOT NULL DEFAULT 0,
  create_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_code (type, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典';

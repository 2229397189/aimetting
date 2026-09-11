-- ============================================================================
-- AI 在线模拟面试平台 - H2 建表脚本（MODE=MySQL 兼容模式）
--
-- 与 MySQL 版本的差异：
--   1. 不使用 ENGINE=InnoDB / DEFAULT CHARSET / COMMENT 等 MySQL 专属语法
--   2. 不使用 ON UPDATE CURRENT_TIMESTAMP（H2 不支持，由 MetaObjectHandler 填充 update_time）
--   3. 大文本用 VARCHAR(n)，避免 TEXT/CLOB 差异
--
-- M1 落盘：t_user / t_user_profile
-- ============================================================================


-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  email         VARCHAR(128) DEFAULT NULL,
  nickname      VARCHAR(64)  DEFAULT NULL,
  avatar        VARCHAR(255) DEFAULT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
  status        TINYINT      NOT NULL DEFAULT 1,
  last_login_at DATETIME     DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_create_time (create_time)
);


-- 2. 用户资料表（与 t_user 一对一）
CREATE TABLE IF NOT EXISTS t_user_profile (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  user_id         BIGINT       NOT NULL,
  target_position VARCHAR(64)  DEFAULT NULL,
  work_years      INT          NOT NULL DEFAULT 0,
  intro           VARCHAR(500) DEFAULT NULL,
  phone           VARCHAR(32)  DEFAULT NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted         TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id)
);


-- 3. 题库表（M2）
CREATE TABLE IF NOT EXISTS t_question (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  direction       VARCHAR(32)  NOT NULL,
  difficulty      VARCHAR(16)  NOT NULL,
  title           VARCHAR(500) NOT NULL,
  reference_points VARCHAR(2000) DEFAULT NULL,
  tags            VARCHAR(1000) DEFAULT NULL,
  analysis        VARCHAR(4000) DEFAULT NULL,
  source          VARCHAR(16)  NOT NULL DEFAULT 'SEED',
  status          INT          NOT NULL DEFAULT 1,
  created_by      BIGINT       DEFAULT NULL,
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted         INT          NOT NULL DEFAULT 0
);


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
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT       NOT NULL,
  title        VARCHAR(128) NOT NULL,
  raw_text     VARCHAR(8000) DEFAULT NULL,
  file_url     VARCHAR(512)  DEFAULT NULL,
  parsed_json  VARCHAR(4000) DEFAULT NULL,
  score        INT          DEFAULT NULL,
  advantage    VARCHAR(2000) DEFAULT NULL,
  suggestions  VARCHAR(2000) DEFAULT NULL,
  parsed_by    VARCHAR(16)  NOT NULL DEFAULT 'AI',
  is_default   INT          NOT NULL DEFAULT 0,
  create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted      INT          NOT NULL DEFAULT 0
);


-- 4. 面试会话主表（M4）
CREATE TABLE IF NOT EXISTS t_interview_session (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_no     VARCHAR(32)  NOT NULL,
  user_id        BIGINT       NOT NULL,
  resume_id      BIGINT       DEFAULT NULL,
  directions     VARCHAR(255) NOT NULL,
  difficulty     VARCHAR(16)  NOT NULL,
  total_question INT          NOT NULL DEFAULT 8,
  current_index  INT          NOT NULL DEFAULT 0,
  status         VARCHAR(20)  NOT NULL DEFAULT 'INIT',
  prev_status    VARCHAR(20)  DEFAULT NULL,
  score          DECIMAL(5,2) DEFAULT NULL,
  jd_text        VARCHAR(4000) DEFAULT NULL,
  phase_plan     VARCHAR(255) DEFAULT NULL,
  started_at     TIMESTAMP    DEFAULT NULL,
  finished_at    TIMESTAMP    DEFAULT NULL,
  create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted        INT          NOT NULL DEFAULT 0
);


-- 5. 会话题目表（M4）
CREATE TABLE IF NOT EXISTS t_session_question (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id       BIGINT       NOT NULL,
  question_no      INT          NOT NULL,
  question_id      BIGINT       DEFAULT NULL,
  title            VARCHAR(1000) NOT NULL,
  reference_points VARCHAR(2000) DEFAULT NULL,
  source           VARCHAR(16)  NOT NULL,
  difficulty       VARCHAR(16)  NOT NULL,
  phase            VARCHAR(20)  DEFAULT NULL,
  skipped          INT          NOT NULL DEFAULT 0,
  create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted          INT          NOT NULL DEFAULT 0
);


-- 6. 答题记录表（M4）
CREATE TABLE IF NOT EXISTS t_session_answer (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id         BIGINT       NOT NULL,
  session_question_id BIGINT     NOT NULL,
  user_id            BIGINT       NOT NULL,
  content            VARCHAR(8000) NOT NULL,
  is_follow_up       INT          NOT NULL DEFAULT 0,
  parent_answer_id   BIGINT       DEFAULT NULL,
  score              INT          DEFAULT NULL,
  comment            VARCHAR(4000) DEFAULT NULL,
  highlights         VARCHAR(2000) DEFAULT NULL,
  gaps               VARCHAR(2000) DEFAULT NULL,
  improved_answer    VARCHAR(4000) DEFAULT NULL,
  follow_up_question VARCHAR(2000) DEFAULT NULL,
  authenticity       INT          DEFAULT NULL COMMENT '简历经历真实性/参与度判断 0-100',
  evaluated_by       VARCHAR(16)  DEFAULT NULL,
  follow_up_count    INT          NOT NULL DEFAULT 0,
  skipped            INT          NOT NULL DEFAULT 0,
  client_token       VARCHAR(64)  DEFAULT NULL,
  create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted            INT          NOT NULL DEFAULT 0
);


-- 7. AI 调用日志表（M3）
CREATE TABLE IF NOT EXISTS t_ai_call_log (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT       DEFAULT NULL,
  biz_type         VARCHAR(32)  NOT NULL,
  agent_id         VARCHAR(32)  DEFAULT NULL,
  provider         VARCHAR(32)  NOT NULL,
  model            VARCHAR(64)  DEFAULT NULL,
  request_digest   VARCHAR(512) DEFAULT NULL,
  response_digest  VARCHAR(512) DEFAULT NULL,
  prompt_tokens    INT          DEFAULT 0,
  completion_tokens INT         DEFAULT 0,
  cost_ms          BIGINT       DEFAULT 0,
  success          INT          NOT NULL DEFAULT 1,
  error_type       VARCHAR(32)  DEFAULT NULL,
  error_msg        VARCHAR(512) DEFAULT NULL,
  request_id       VARCHAR(64)  DEFAULT NULL,
  create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);


-- 8. 会话状态流水表（M4）
CREATE TABLE IF NOT EXISTS t_session_event (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id  BIGINT       NOT NULL,
  from_status VARCHAR(20)  DEFAULT NULL,
  to_status   VARCHAR(20)  NOT NULL,
  event       VARCHAR(64)  NOT NULL,
  operator    BIGINT       DEFAULT NULL,
  remark      VARCHAR(500) DEFAULT NULL,
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);


-- 9. 面试报告表（M6）
CREATE TABLE IF NOT EXISTS t_interview_report (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id      BIGINT       NOT NULL,
  user_id         BIGINT       NOT NULL,
  total_score     DECIMAL(5,2) NOT NULL DEFAULT 0,
  dimension_json  VARCHAR(2000) DEFAULT NULL,
  highlights      VARCHAR(2000) DEFAULT NULL,
  improvements    VARCHAR(2000) DEFAULT NULL,
  actions         VARCHAR(2000) DEFAULT NULL,
  overall_comment VARCHAR(4000) DEFAULT NULL,
  generated_by    VARCHAR(16)  NOT NULL DEFAULT 'AI',
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  deleted         INT          NOT NULL DEFAULT 0
);


-- 10. 字典表（M8）
CREATE TABLE IF NOT EXISTS t_dict (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  type       VARCHAR(64) NOT NULL,
  code       VARCHAR(64) NOT NULL,
  label      VARCHAR(128) NOT NULL,
  sort       INT          NOT NULL DEFAULT 0,
  create_time TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
  deleted    INT          NOT NULL DEFAULT 0
);


-- ============================================================================
-- 历史库表增量迁移（幂等，仅当列不存在时添加）
-- 必须放在所有 CREATE TABLE 之后：全新库已由上面的 CREATE 自带这些列，
-- 存量库则通过 IF NOT EXISTS 补齐，避免「ALTER 早于建表」导致建表脚本失败。
-- ============================================================================
ALTER TABLE t_session_answer ADD COLUMN IF NOT EXISTS follow_up_question VARCHAR(2000) DEFAULT NULL;
ALTER TABLE t_session_answer ADD COLUMN IF NOT EXISTS authenticity INT DEFAULT NULL COMMENT '简历经历真实性/参与度判断 0-100';
ALTER TABLE t_ai_call_log ADD COLUMN IF NOT EXISTS agent_id VARCHAR(32) DEFAULT NULL;

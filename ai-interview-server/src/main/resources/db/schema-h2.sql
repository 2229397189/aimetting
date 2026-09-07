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

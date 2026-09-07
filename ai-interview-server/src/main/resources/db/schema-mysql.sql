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

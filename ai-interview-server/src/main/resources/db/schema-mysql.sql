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


-- ============================================================================
-- 以下为后续模块建表占位（由 M2~M8 补充，此处仅留位置说明，不创建）
--   M2/M3  题库：       t_question
--   M4     面试会话：   t_interview_session / t_session_question
--   M5     答题与幂等： t_session_answer / t_idempotent_record
--   M5     AI 日志：    t_ai_call_log
--   M4     状态流水：   t_session_event
--   M6     简历：       t_resume
--   M7     报告：       t_interview_report
--   公共   字典：       t_dict
-- ============================================================================

-- Flyway V11 — 管理员角色（admin 模块基础）
-- auth_user 加 role 列（枚举走 INT，对齐项目约定）

ALTER TABLE auth_user
  ADD COLUMN role INT NOT NULL DEFAULT 0 COMMENT '0=USER 1=ADMIN';

-- Flyway V2: 种子数据（可重复执行）
-- INF-05: 测试账号请走 /api/auth/register 注册（phone_cipher 依赖 AES，不宜硬编码 INSERT）

INSERT IGNORE INTO admin_forbidden_word (word) VALUES
  ('兼职'),
  ('代写'),
  ('刷课');

-- 二手砍价（offer/counter-offer）：单行 ping-pong 议价，accept 后生成 trade_order。
-- 枚举列用 INT（V15 起全模块约定），不用 TINYINT。
CREATE TABLE IF NOT EXISTS trade_offer (
  id              BIGINT   NOT NULL AUTO_INCREMENT,
  item_id         BIGINT   NOT NULL,
  buyer_id        BIGINT   NOT NULL,
  seller_id       BIGINT   NOT NULL,
  price_point     INT      NOT NULL,
  status          INT      NOT NULL DEFAULT 0 COMMENT '0=PENDING 1=ACCEPTED 2=REJECTED 3=CANCELED',
  awaiting_party  INT      NOT NULL DEFAULT 1 COMMENT '0=BUYER 1=SELLER 的回合',
  order_id        BIGINT   NULL COMMENT 'accept 后回填成单的 trade_order.id',
  version         INT      NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at      DATETIME NULL,
  creator_id      BIGINT   NULL,
  updater_id      BIGINT   NULL,
  PRIMARY KEY (id),
  KEY idx_trade_offer_buyer_status  (buyer_id, status),
  KEY idx_trade_offer_seller_status (seller_id, status),
  KEY idx_trade_offer_item_id       (item_id),
  CONSTRAINT fk_trade_offer_item FOREIGN KEY (item_id) REFERENCES trade_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

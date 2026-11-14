-- Optional: the app creates all of this automatically on first run.
CREATE DATABASE IF NOT EXISTS unishare CHARACTER SET utf8mb4;
USE unishare;

CREATE TABLE IF NOT EXISTS users (
  user_id       VARCHAR(30) PRIMARY KEY,
  full_name     VARCHAR(80) NOT NULL,
  department    VARCHAR(60),
  password_hash VARCHAR(128) NOT NULL,
  salt          VARCHAR(64) NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS resources (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(120) NOT NULL,
  category_id INT NOT NULL,
  description VARCHAR(500),
  owner_id    VARCHAR(30) NOT NULL,
  available   BOOLEAN NOT NULL DEFAULT TRUE,
  issued_to   VARCHAR(30) NULL,
  issued_date DATE NULL,
  added_on    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_res_cat    FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_res_owner  FOREIGN KEY (owner_id)  REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_res_issued FOREIGN KEY (issued_to) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

INSERT IGNORE INTO categories(name) VALUES
  ('Books'),('Lab Equipment'),('Electronics'),('Stationery'),('Sports'),('Notes & Papers'),('Other');


-- Each borrowing cycle is stored separately, including past returned items.
CREATE TABLE IF NOT EXISTS borrowing_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resource_id INT NULL,
  resource_name VARCHAR(120) NOT NULL,
  owner_id VARCHAR(30) NOT NULL,
  borrower_id VARCHAR(30) NOT NULL,
  borrowed_on DATE NOT NULL,
  due_date DATE NOT NULL,
  returned_on DATE NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_history_borrower (borrower_id),
  INDEX idx_history_resource (resource_id),
  CONSTRAINT fk_history_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL,
  CONSTRAINT fk_history_owner FOREIGN KEY (owner_id) REFERENCES users(user_id),
  CONSTRAINT fk_history_borrower FOREIGN KEY (borrower_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Campus marketplace: independent from the existing borrow/lend resources.
CREATE TABLE IF NOT EXISTS marketplace_products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(50) NOT NULL,
  description VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 1,
  seller_id VARCHAR(30) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_marketplace_category (category),
  INDEX idx_marketplace_seller (seller_id),
  CONSTRAINT fk_marketplace_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS marketplace_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id VARCHAR(30) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  pickup_point VARCHAR(160) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PLACED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_marketplace_buyer (buyer_id),
  CONSTRAINT fk_marketplace_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS marketplace_order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id INT NULL,
  product_title VARCHAR(120) NOT NULL,
  seller_id VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_marketplace_item_order FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_marketplace_item_product FOREIGN KEY (product_id) REFERENCES marketplace_products(id) ON DELETE SET NULL,
  CONSTRAINT fk_marketplace_item_seller FOREIGN KEY (seller_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

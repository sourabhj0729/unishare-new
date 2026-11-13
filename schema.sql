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


-- Campus digital library: metadata is stored here; files live in uploads/documents.
CREATE TABLE IF NOT EXISTS documents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(180) NOT NULL,
  description VARCHAR(1000),
  original_file_name VARCHAR(255) NOT NULL,
  stored_file_name VARCHAR(100) NOT NULL UNIQUE,
  file_path VARCHAR(500) NOT NULL,
  file_type VARCHAR(10) NOT NULL,
  subject VARCHAR(120) NOT NULL,
  department VARCHAR(100) NOT NULL,
  semester TINYINT NOT NULL,
  faculty VARCHAR(120),
  uploaded_by VARCHAR(30) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_documents_subject (subject),
  INDEX idx_documents_department_semester (department, semester),
  FULLTEXT INDEX ft_documents_search (title, description, subject, faculty),
  CONSTRAINT fk_documents_uploader FOREIGN KEY (uploaded_by)
    REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

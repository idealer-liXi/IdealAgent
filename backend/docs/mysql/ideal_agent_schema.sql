CREATE DATABASE IF NOT EXISTS ideal_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ideal_agent;

CREATE TABLE IF NOT EXISTS ai_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  user_role VARCHAR(32) NOT NULL DEFAULT 'user',
  user_avatar VARCHAR(512) DEFAULT NULL,
  user_status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_name (user_name),
  KEY idx_user_role (user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_api (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  api_id VARCHAR(64) NOT NULL,
  api_name VARCHAR(128) NOT NULL,
  base_url VARCHAR(512) NOT NULL,
  api_key VARCHAR(1024) DEFAULT NULL,
  api_type VARCHAR(32) NOT NULL DEFAULT 'openai',
  api_status TINYINT NOT NULL DEFAULT 1,
  api_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_api_id (api_id),
  KEY idx_api_from (api_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_model (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  model_id VARCHAR(64) NOT NULL,
  api_id VARCHAR(64) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  model_type VARCHAR(32) NOT NULL DEFAULT 'model',
  model_status TINYINT NOT NULL DEFAULT 1,
  model_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_model_id (model_id),
  KEY idx_model_api_id (api_id),
  KEY idx_model_from (model_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_mcp (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mcp_id VARCHAR(64) NOT NULL,
  mcp_name VARCHAR(128) NOT NULL,
  mcp_type VARCHAR(32) NOT NULL,
  mcp_config TEXT NOT NULL,
  mcp_secret TEXT DEFAULT NULL,
  mcp_timeout INT NOT NULL DEFAULT 3,
  mcp_status TINYINT NOT NULL DEFAULT 1,
  mcp_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mcp_id (mcp_id),
  KEY idx_mcp_from (mcp_from),
  KEY idx_mcp_type (mcp_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_advisor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  advisor_id VARCHAR(64) NOT NULL,
  advisor_name VARCHAR(128) NOT NULL,
  advisor_type VARCHAR(64) NOT NULL,
  advisor_config TEXT DEFAULT NULL,
  advisor_status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_advisor_id (advisor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_prompt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  prompt_id VARCHAR(64) NOT NULL,
  prompt_name VARCHAR(128) NOT NULL,
  prompt_type VARCHAR(32) NOT NULL,
  prompt_content TEXT NOT NULL,
  prompt_status TINYINT NOT NULL DEFAULT 1,
  prompt_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_prompt_id (prompt_id),
  KEY idx_prompt_from (prompt_from),
  KEY idx_prompt_type (prompt_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_client (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  client_id VARCHAR(64) NOT NULL,
  client_type VARCHAR(32) NOT NULL,
  client_role VARCHAR(64) DEFAULT NULL,
  model_id VARCHAR(64) NOT NULL,
  model_name VARCHAR(128) DEFAULT NULL,
  client_name VARCHAR(128) NOT NULL,
  client_status TINYINT NOT NULL DEFAULT 1,
  client_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_client_id (client_id),
  KEY idx_client_model_id (model_id),
  KEY idx_client_type (client_type),
  KEY idx_client_from (client_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_agent (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_id VARCHAR(64) NOT NULL,
  agent_name VARCHAR(128) NOT NULL,
  agent_type VARCHAR(32) NOT NULL,
  agent_desc TEXT DEFAULT NULL,
  model_id VARCHAR(64) DEFAULT NULL,
  template_id VARCHAR(64) DEFAULT NULL,
  agent_status TINYINT NOT NULL DEFAULT 1,
  agent_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_agent_id (agent_id),
  KEY idx_agent_type (agent_type),
  KEY idx_agent_from (agent_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_id VARCHAR(64) NOT NULL,
  owner_id VARCHAR(64) NOT NULL,
  owner_type VARCHAR(32) NOT NULL,
  config_type VARCHAR(32) NOT NULL,
  ref_id VARCHAR(64) NOT NULL,
  config_status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_config_id (config_id),
  KEY idx_config_owner (owner_id, owner_type),
  KEY idx_config_ref (ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_id VARCHAR(64) NOT NULL,
  client_id VARCHAR(64) NOT NULL,
  client_role VARCHAR(64) NOT NULL,
  user_prompt TEXT NOT NULL,
  flow_seq INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flow_agent_client (agent_id, client_id),
  KEY idx_flow_agent (agent_id),
  KEY idx_flow_client (client_id),
  KEY idx_flow_seq (agent_id, flow_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  agent_id VARCHAR(64) NOT NULL,
  task_name VARCHAR(128) NOT NULL,
  task_param TEXT DEFAULT NULL,
  cron_expression VARCHAR(128) NOT NULL,
  task_status TINYINT NOT NULL DEFAULT 1,
  task_from BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_id (task_id),
  KEY idx_task_agent (agent_id),
  KEY idx_task_from (task_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  session_type VARCHAR(32) NOT NULL,
  session_title VARCHAR(255) DEFAULT NULL,
  user_id BIGINT NOT NULL,
  target_id VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_session_id (session_id),
  KEY idx_session_user (user_id),
  KEY idx_session_type (session_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  message_id VARCHAR(64) NOT NULL,
  session_id VARCHAR(64) NOT NULL,
  message_type VARCHAR(32) NOT NULL,
  message_role VARCHAR(32) NOT NULL,
  message_content MEDIUMTEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_id (message_id),
  KEY idx_message_session (session_id),
  KEY idx_message_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  template_desc TEXT DEFAULT NULL,
  template_type VARCHAR(32) NOT NULL,
  template_status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_template_id (template_id),
  KEY idx_template_type (template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_repo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repo_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  agent_id VARCHAR(64) NOT NULL,
  repo_type VARCHAR(32) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_repo_id (repo_id),
  KEY idx_repo_user (user_id),
  KEY idx_repo_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_plaza (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plaza_id VARCHAR(64) NOT NULL,
  agent_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description TEXT DEFAULT NULL,
  cover_url VARCHAR(512) DEFAULT NULL,
  like_count INT NOT NULL DEFAULT 0,
  favor_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  plaza_status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plaza_id (plaza_id),
  KEY idx_plaza_agent (agent_id),
  KEY idx_plaza_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_plaza_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plaza_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_like_user_plaza (plaza_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_plaza_favor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plaza_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favor_user_plaza (plaza_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_plaza_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id VARCHAR(64) NOT NULL,
  plaza_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_comment_id (comment_id),
  KEY idx_comment_plaza (plaza_id),
  KEY idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_stat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_id VARCHAR(64) NOT NULL,
  stat_type VARCHAR(64) NOT NULL,
  stat_key VARCHAR(128) NOT NULL,
  stat_value BIGINT NOT NULL DEFAULT 0,
  stat_date DATE NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_stat_id (stat_id),
  KEY idx_stat_type_date (stat_type, stat_date),
  KEY idx_stat_key (stat_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

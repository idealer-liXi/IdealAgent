USE ideal_agent;

INSERT INTO ai_user (user_name, password, user_role, user_avatar, user_status)
VALUES ('admin', '$2a$10$yDKp/kH2Tw245JMbIfEPT.wXo5BZkxMWqzZI5T.AAibK2/vXfXMYC', 'admin', NULL, 1)
ON DUPLICATE KEY UPDATE user_role = VALUES(user_role), user_status = VALUES(user_status);

INSERT INTO ai_api (api_id, api_name, base_url, api_key, api_type, api_status, api_from)
VALUES ('api_openai_default', 'OpenAI Compatible Placeholder', 'https://api.openai.com', '', 'openai', 0, 0)
ON DUPLICATE KEY UPDATE api_name = VALUES(api_name), base_url = VALUES(base_url);

INSERT INTO ai_model (model_id, api_id, model_name, model_type, model_status, model_from)
VALUES ('model_default_chat', 'api_openai_default', 'gpt-4o-mini', 'chat', 0, 0)
ON DUPLICATE KEY UPDATE model_name = VALUES(model_name), model_type = VALUES(model_type);

INSERT INTO ai_prompt (prompt_id, prompt_name, prompt_type, prompt_content, prompt_status, prompt_from)
VALUES ('prompt_default_system', 'Default System Prompt', 'system', 'You are IdealAgent, a helpful learning assistant. Answer in Chinese when the user writes Chinese.', 1, 0)
ON DUPLICATE KEY UPDATE prompt_content = VALUES(prompt_content);

INSERT INTO ai_advisor (advisor_id, advisor_name, advisor_type, advisor_config, advisor_status)
VALUES ('advisor_memory_default', 'Default Chat Memory', 'memory', '{"retrieveSize":20}', 1)
ON DUPLICATE KEY UPDATE advisor_config = VALUES(advisor_config);

INSERT INTO ai_client (client_id, client_type, client_role, model_id, model_name, client_name, client_status, client_from)
VALUES ('client_default_chat', 'chat', 'assistant', 'model_default_chat', 'gpt-4o-mini', 'Default Chat Client', 0, 0)
ON DUPLICATE KEY UPDATE client_name = VALUES(client_name), model_id = VALUES(model_id);

INSERT INTO ai_config (config_id, owner_id, owner_type, config_type, ref_id, config_status)
VALUES
  ('config_client_default_prompt', 'client_default_chat', 'client', 'prompt', 'prompt_default_system', 1),
  ('config_client_default_advisor', 'client_default_chat', 'client', 'advisor', 'advisor_memory_default', 1)
ON DUPLICATE KEY UPDATE ref_id = VALUES(ref_id), config_status = VALUES(config_status);

INSERT INTO ai_template (template_id, template_name, template_desc, template_type, template_status)
VALUES ('template_default_chat', 'Default Chat Template', 'Minimal template for first chat learning flow.', 'chat', 1)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name), template_desc = VALUES(template_desc);

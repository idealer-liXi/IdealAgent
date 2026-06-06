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

INSERT INTO ai_prompt (prompt_id, prompt_name, prompt_type, prompt_content, prompt_status, prompt_from)
VALUES
  ('prompt_work_step_inspector', 'Work Step Inspector', 'work', '你是任务审查专家。根据用户任务列出可能需要的工具或信息来源。用户任务：%s。只输出 JSON 数组。', 1, 0),
  ('prompt_work_step_planner', 'Work Step Planner', 'work', '你是任务规划专家。用户任务：%s。审查结果：%s。拆解为可执行步骤，只输出 JSON 数组。', 1, 0),
  ('prompt_work_step_runner', 'Work Step Runner', 'work', '你是任务运行专家。用户任务：%s。审查结果：%s。当前步骤：%s。只输出 JSON 对象，字段包含 runner_result 和 runner_status。', 1, 0),
  ('prompt_work_step_replier', 'Work Step Replier', 'work', '你是任务回复专家。用户任务：%s。执行历史：%s。只输出 JSON 对象，字段包含 replier_overview。', 1, 0),
  ('prompt_work_loop_analyzer', 'Work Loop Analyzer', 'work', '你是任务分析专家。当前第 %s 轮，最大 %s 轮。用户任务：%s。当前任务：%s。执行历史：%s。只输出 JSON 对象，可包含 analyzer_demand、analyzer_history、analyzer_strategy、analyzer_progress、analyzer_status。', 1, 0),
  ('prompt_work_loop_performer', 'Work Loop Performer', 'work', '你是任务执行专家。用户任务：%s。任务分析：%s。只输出 JSON 对象，可包含 performer_target、performer_process、performer_result。', 1, 0),
  ('prompt_work_loop_supervisor', 'Work Loop Supervisor', 'work', '你是任务监督专家。用户任务：%s。任务分析：%s。任务执行：%s。只输出 JSON 对象，可包含 supervisor_issue、supervisor_suggestion、supervisor_score、supervisor_status。', 1, 0),
  ('prompt_work_loop_summarizer', 'Work Loop Summarizer', 'work', '你是任务总结专家。用户任务：%s。执行历史：%s。只输出 JSON 对象，字段包含 summarizer_overview。', 1, 0),
  ('prompt_work_react_observer', 'Work React Observer', 'work', '你是任务观察专家。当前第 %s 步，最大 %s 步。用户任务：%s。当前任务：%s。执行历史：%s。只输出 JSON 对象，可包含 observer_demand、observer_history、observer_judgement、observer_status。', 1, 0),
  ('prompt_work_react_reasoner', 'Work React Reasoner', 'work', '你是任务推理专家。用户任务：%s。观察结果：%s。执行历史：%s。只输出 JSON 对象，可包含 reasoner_target、reasoner_action、reasoner_acceptance。', 1, 0),
  ('prompt_work_react_actor', 'Work React Actor', 'work', '你是任务行动专家。用户任务：%s。观察结果：%s。推理结果：%s。只输出 JSON 对象，可包含 actor_target、actor_process、actor_result。', 1, 0),
  ('prompt_work_react_evaluator', 'Work React Evaluator', 'work', '你是任务评估专家。用户任务：%s。执行历史：%s。只输出 JSON 对象，字段包含 evaluator_overview。', 1, 0)
ON DUPLICATE KEY UPDATE prompt_content = VALUES(prompt_content), prompt_status = VALUES(prompt_status);

INSERT INTO ai_advisor (advisor_id, advisor_name, advisor_type, advisor_config, advisor_status)
VALUES ('advisor_memory_default', 'Default Chat Memory', 'Memory', '{"maxMessages":20}', 1)
ON DUPLICATE KEY UPDATE advisor_config = VALUES(advisor_config);

INSERT INTO ai_advisor (advisor_id, advisor_name, advisor_type, advisor_config, advisor_status)
VALUES ('advisor_rag_sample', 'Sample RAG Advisor', 'Rag', '{"topK":4,"filterExpression":"source == \"note.md\""}', 0)
ON DUPLICATE KEY UPDATE advisor_config = VALUES(advisor_config), advisor_status = VALUES(advisor_status);

INSERT INTO ai_client (client_id, client_type, client_role, model_id, model_name, client_name, client_status, client_from)
VALUES ('client_default_chat', 'chat', 'assistant', 'model_default_chat', 'gpt-4o-mini', 'Default Chat Client', 0, 0)
ON DUPLICATE KEY UPDATE client_name = VALUES(client_name), model_id = VALUES(model_id);

INSERT INTO ai_agent (agent_id, agent_name, agent_type, agent_desc, model_id, template_id, agent_status, agent_from)
VALUES
  ('agent_default_step', 'Default Step Work Agent', 'step', '默认 Step 任务执行智能体', NULL, NULL, 0, 0),
  ('agent_default_loop', 'Default Loop Work Agent', 'loop', '默认 Loop 任务执行智能体', NULL, NULL, 0, 0),
  ('agent_default_react', 'Default React Work Agent', 'react', '默认 React 任务执行智能体', NULL, NULL, 0, 0)
ON DUPLICATE KEY UPDATE agent_name = VALUES(agent_name), agent_type = VALUES(agent_type), agent_desc = VALUES(agent_desc);

INSERT INTO ai_flow (flow_id, agent_id, client_id, role_type, sort_order, flow_status)
VALUES
  ('flow_default_step_inspector', 'agent_default_step', 'client_default_chat', 'inspector', 1, 1),
  ('flow_default_step_planner', 'agent_default_step', 'client_default_chat', 'planner', 2, 1),
  ('flow_default_step_runner', 'agent_default_step', 'client_default_chat', 'runner', 3, 1),
  ('flow_default_step_replier', 'agent_default_step', 'client_default_chat', 'replier', 4, 1),
  ('flow_default_loop_analyzer', 'agent_default_loop', 'client_default_chat', 'analyzer', 1, 1),
  ('flow_default_loop_performer', 'agent_default_loop', 'client_default_chat', 'performer', 2, 1),
  ('flow_default_loop_supervisor', 'agent_default_loop', 'client_default_chat', 'supervisor', 3, 1),
  ('flow_default_loop_summarizer', 'agent_default_loop', 'client_default_chat', 'summarizer', 4, 1),
  ('flow_default_react_observer', 'agent_default_react', 'client_default_chat', 'observer', 1, 1),
  ('flow_default_react_reasoner', 'agent_default_react', 'client_default_chat', 'reasoner', 2, 1),
  ('flow_default_react_actor', 'agent_default_react', 'client_default_chat', 'actor', 3, 1),
  ('flow_default_react_evaluator', 'agent_default_react', 'client_default_chat', 'evaluator', 4, 1)
ON DUPLICATE KEY UPDATE client_id = VALUES(client_id), role_type = VALUES(role_type), sort_order = VALUES(sort_order), flow_status = VALUES(flow_status);

INSERT INTO ai_config (config_id, owner_id, owner_type, config_type, ref_id, config_status)
VALUES
  ('config_client_default_prompt', 'client_default_chat', 'client', 'prompt', 'prompt_default_system', 1),
  ('config_client_default_advisor', 'client_default_chat', 'client', 'advisor', 'advisor_memory_default', 1),
  ('config_flow_default_step_inspector_prompt', 'flow_default_step_inspector', 'flow', 'prompt', 'prompt_work_step_inspector', 1),
  ('config_flow_default_step_planner_prompt', 'flow_default_step_planner', 'flow', 'prompt', 'prompt_work_step_planner', 1),
  ('config_flow_default_step_runner_prompt', 'flow_default_step_runner', 'flow', 'prompt', 'prompt_work_step_runner', 1),
  ('config_flow_default_step_replier_prompt', 'flow_default_step_replier', 'flow', 'prompt', 'prompt_work_step_replier', 1),
  ('config_flow_default_loop_analyzer_prompt', 'flow_default_loop_analyzer', 'flow', 'prompt', 'prompt_work_loop_analyzer', 1),
  ('config_flow_default_loop_performer_prompt', 'flow_default_loop_performer', 'flow', 'prompt', 'prompt_work_loop_performer', 1),
  ('config_flow_default_loop_supervisor_prompt', 'flow_default_loop_supervisor', 'flow', 'prompt', 'prompt_work_loop_supervisor', 1),
  ('config_flow_default_loop_summarizer_prompt', 'flow_default_loop_summarizer', 'flow', 'prompt', 'prompt_work_loop_summarizer', 1),
  ('config_flow_default_react_observer_prompt', 'flow_default_react_observer', 'flow', 'prompt', 'prompt_work_react_observer', 1),
  ('config_flow_default_react_reasoner_prompt', 'flow_default_react_reasoner', 'flow', 'prompt', 'prompt_work_react_reasoner', 1),
  ('config_flow_default_react_actor_prompt', 'flow_default_react_actor', 'flow', 'prompt', 'prompt_work_react_actor', 1),
  ('config_flow_default_react_evaluator_prompt', 'flow_default_react_evaluator', 'flow', 'prompt', 'prompt_work_react_evaluator', 1)
ON DUPLICATE KEY UPDATE ref_id = VALUES(ref_id), config_status = VALUES(config_status);

INSERT INTO ai_template (template_id, template_name, template_desc, template_type, template_status)
VALUES ('template_default_chat', 'Default Chat Template', 'Minimal template for first chat learning flow.', 'chat', 1)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name), template_desc = VALUES(template_desc);

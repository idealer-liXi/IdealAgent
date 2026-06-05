package com.idealagent.domain.ai.service.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WorkJsonParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode parseArray(String content) {
        return parse(content, '[', ']', "JSON 数组解析失败");
    }

    public JsonNode parseObject(String content) {
        return parse(content, '{', '}', "JSON 对象解析失败");
    }

    private JsonNode parse(String content, char startChar, char endChar, String message) {
        try {
            String cleaned = clean(content);
            int start = cleaned.indexOf(startChar);
            int end = cleaned.lastIndexOf(endChar);
            if (start < 0 || end < start) {
                throw new WorkException(message);
            }
            return objectMapper.readTree(cleaned.substring(start, end + 1));
        } catch (WorkException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkException(message, e);
        }
    }

    private String clean(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replaceAll("(?s)<think>.*?</think>", "")
                .replace("<think>", "")
                .replace("</think>", "")
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}

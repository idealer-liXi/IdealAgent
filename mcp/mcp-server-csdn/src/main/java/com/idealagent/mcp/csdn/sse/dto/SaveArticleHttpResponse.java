package com.idealagent.mcp.csdn.sse.dto;

import lombok.Data;

@Data
public class SaveArticleHttpResponse {

    private Integer code;
    private String traceId;
    private ArticleData data;
    private String msg;

    @Data
    public static class ArticleData {
        private String url;
        private String qrcode;
        private Long id;
        private String title;
        private String description;
    }
}

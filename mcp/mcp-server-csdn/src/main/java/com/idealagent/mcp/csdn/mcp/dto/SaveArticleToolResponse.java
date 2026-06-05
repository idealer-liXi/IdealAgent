package com.idealagent.mcp.csdn.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class SaveArticleToolResponse {

    @JsonProperty(required = true, value = "code")
    @JsonPropertyDescription("发送文章的状态码")
    private Integer code;

    @JsonProperty(required = true, value = "info")
    @JsonPropertyDescription("发送文章的状态信息")
    private String info;

    @JsonProperty(required = true, value = "articleId")
    @JsonPropertyDescription("文章的发布id")
    private Long articleId;

    @JsonProperty(required = true, value = "url")
    @JsonPropertyDescription("文章的发布网址")
    private String url;

    @JsonProperty(required = true, value = "qrcode")
    @JsonPropertyDescription("文章的发布二维码")
    private String qrcode;
}

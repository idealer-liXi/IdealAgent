package com.idealagent.mcp.csdn.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.idealagent.mcp.csdn.util.MarkdownConverter;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaveArticleToolRequest {

    @JsonProperty(required = true, value = "title")
    @JsonPropertyDescription("文章标题")
    private String title;

    @JsonProperty(required = true, value = "markdownContent")
    @JsonPropertyDescription("文章内容")
    private String markdownContent;

    public String getHtmlContent() {
        return MarkdownConverter.convertToHtml(markdownContent);
    }
}

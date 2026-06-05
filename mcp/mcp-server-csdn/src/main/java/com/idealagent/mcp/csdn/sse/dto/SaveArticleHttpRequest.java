package com.idealagent.mcp.csdn.sse.dto;

import lombok.Data;

import java.util.List;

@Data
public class SaveArticleHttpRequest {

    private String title;
    private String markdowncontent;
    private String content;
    private String tags;
    private String categories;
    private List<String> cover_images;
    private String Description = "有关 Java 的有趣知识，文章标题和内容为 AI 生成，请仔细甄别信息！";
    private String readType = "public";
    private String level = "0";
    private Integer status = 0;
    private String type = "original";
    private String original_link = "";
    private Boolean authorized_status = false;
    private String resource_url = "";
    private String not_auto_saved = "1";
    private String source = "pc_mdeditor";
    private Integer cover_type = 1;
    private Integer is_new = 1;
    private Integer vote_id = 0;
    private String resource_id = "";
    private String pubStatus = "publish";
    private Integer sync_git_code = 0;
    private String creator_activity_id = "";
}

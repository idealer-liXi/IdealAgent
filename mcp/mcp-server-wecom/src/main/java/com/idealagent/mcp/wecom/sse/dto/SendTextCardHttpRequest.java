package com.idealagent.mcp.wecom.sse.dto;

import lombok.Data;

@Data
public class SendTextCardHttpRequest {

    private Integer agentid;
    private TextCard textcard;
    private String touser = "@all";
    private String msgtype = "textcard";
    private Integer enable_id_trans = 0;
    private Integer enable_duplicate_check = 1;
    private Integer duplicate_check_interval = 1800;

    @Data
    public static class TextCard {
        private String title;
        private String description;
        private String url;
        private String btntxt = "点击跳转查看";
    }
}

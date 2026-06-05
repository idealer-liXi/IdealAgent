package com.idealagent.mcp.wecom.sse.dto;

import lombok.Data;

@Data
public class SendTextHttpRequest {

    private Integer agentid;
    private Text text;
    private String touser = "@all";
    private String msgtype = "text";
    private Integer safe = 0;
    private Integer enable_id_trans = 0;
    private Integer enable_duplicate_check = 1;
    private Integer duplicate_check_interval = 1800;

    @Data
    public static class Text {
        private String content;
    }
}

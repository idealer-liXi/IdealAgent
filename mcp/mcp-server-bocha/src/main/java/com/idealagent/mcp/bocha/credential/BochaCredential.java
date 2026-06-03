package com.idealagent.mcp.bocha.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BochaCredential {

    private String apiKey;
    private String userId;

    public boolean checkValid() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId);
    }
}

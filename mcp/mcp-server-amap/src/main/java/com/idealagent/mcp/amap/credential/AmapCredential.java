package com.idealagent.mcp.amap.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmapCredential {

    private String apiKey;

    private String userId;

    public boolean checkValid() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId);
    }
}

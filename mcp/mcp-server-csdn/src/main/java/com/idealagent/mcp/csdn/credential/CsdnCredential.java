package com.idealagent.mcp.csdn.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsdnCredential {

    private String cookie;
    private String categories;
    private String tags;
    private String coverUrl;
    private String userId;

    public boolean checkValid() {
        return StringUtils.hasText(cookie)
                && StringUtils.hasText(categories)
                && StringUtils.hasText(tags)
                && StringUtils.hasText(coverUrl)
                && StringUtils.hasText(userId);
    }
}

package com.idealagent.domain.ai.repository;

import java.util.List;

public interface IRagTagRepository {
    List<String> listTags(Long userId);
}

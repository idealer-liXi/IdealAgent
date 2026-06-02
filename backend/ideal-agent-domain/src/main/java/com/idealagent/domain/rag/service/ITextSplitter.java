package com.idealagent.domain.rag.service;

import java.util.List;

public interface ITextSplitter {
    List<String> split(String text);
}

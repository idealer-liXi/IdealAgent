package com.idealagent.infrastructure.rag;

import com.idealagent.domain.rag.model.dto.RagUploadDTO;
import com.idealagent.domain.rag.model.entity.RagFile;
import com.idealagent.domain.rag.service.IGitImportService;
import com.idealagent.domain.rag.service.RagException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LocalGitImportService implements IGitImportService {
    @Override
    public List<RagFile> importRepo(RagUploadDTO request) {
        Path target = null;
        try {
            target = Files.createTempDirectory("idealagent-rag-git-");
            Process process = new ProcessBuilder("git", "clone", "--depth", "1", request.repoUrl(), target.toString())
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RagException("Git 仓库导入失败");
            }
            return readTextFiles(target);
        } catch (IOException e) {
            throw new RagException("Git 仓库导入失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RagException("Git 仓库导入被中断", e);
        } finally {
            deleteDirectory(target);
        }
    }

    private List<RagFile> readTextFiles(Path root) throws IOException {
        List<RagFile> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (isTextFile(path)) {
                    files.add(new RagFile(root.relativize(path).toString().replace('\\', '/'), Files.readString(path, StandardCharsets.UTF_8)));
                }
            }
        }
        if (files.isEmpty()) {
            throw new RagException("Git 仓库未发现可导入文本文件");
        }
        return files;
    }

    private boolean isTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".java") || name.endsWith(".html");
    }

    private void deleteDirectory(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }
}

package com.example.autogen.service;

import com.example.autogen.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码提取服务 - 从对话历史中提取代码块并写入文件系统
 */
@Service
public class CodeExtractorService {

    private static final Logger log = LoggerFactory.getLogger(CodeExtractorService.class);

    /**
     * 匹配带文件名注释的代码块，例如：
     * ```java
     * // filename: src/main/java/App.java
     * ...代码...
     * ```
     * 也支持 // 文件名: 或 /* filename: 或 # filename: 等格式
     */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(\\w+)?\\s*\\n(.*?)```",
            Pattern.DOTALL
    );

    /**
     * 匹配代码块内首行的文件路径注释
     */
    private static final Pattern FILENAME_COMMENT_PATTERN = Pattern.compile(
            "^\\s*(?://|/\\*|#|--|<!--)\\s*(?:filename|文件名|file|路径)[：:]\\s*(.+?)\\s*(?:\\*/|-->)?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    /**
     * 匹配代码块前一行的文件路径描述，例如：
     * **`src/main/java/App.java`**
     * 或 `application.properties`：
     * 或 ### 1. src/main/java/com/example/App.java
     * 或 **src/main/java/App.java**
     * 或 文件：src/main/java/App.java
     * 或 2. application.properties
     */
    private static final Pattern PRECEDING_FILENAME_PATTERN = Pattern.compile(
            "(?:" +
                "`([\\w./\\-]+\\.\\w+)`" +                          // `filename.ext`
                "|\\*\\*([\\w./\\-]+\\.\\w+)\\*\\*" +               // **filename.ext**
                "|(?:^|\\n)\\s*#+\\s*\\d*\\.?\\s*`?([\\w./\\-]+\\.\\w+)`?" + // ### 1. filename.ext
                "|(?:文件|File|路径|Path)[：:]\\s*`?([\\w./\\-]+\\.\\w+)`?" + // 文件：filename.ext
                "|(?:^|\\n)\\s*\\d+\\.\\s+`?([\\w./\\-]+\\.\\w+)`?" +       // 1. filename.ext
            ")",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 从对话历史中提取代码块并保存到指定目录
     *
     * @param chatHistory 对话历史
     * @param outputDir   输出目录
     * @return 生成的文件列表
     */
    public List<GeneratedFile> extractAndSave(List<ChatMessage> chatHistory, String outputDir) throws IOException {
        Path outputPath = Path.of(outputDir).toAbsolutePath().normalize();

        // 安全检查：确保输出路径不含路径遍历
        if (outputPath.toString().contains("..")) {
            throw new IllegalArgumentException("输出路径不允许包含 '..'");
        }

        List<CodeBlock> codeBlocks = extractCodeBlocks(chatHistory);
        List<GeneratedFile> generatedFiles = new ArrayList<>();

        Files.createDirectories(outputPath);

        for (CodeBlock block : codeBlocks) {
            if (block.filePath() == null || block.filePath().isBlank()) {
                continue;
            }

            Path filePath = outputPath.resolve(block.filePath()).normalize();

            // 安全检查：确保生成的文件在输出目录内
            if (!filePath.startsWith(outputPath)) {
                log.warn("跳过路径遍历文件: {}", block.filePath());
                continue;
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, block.content());
            generatedFiles.add(new GeneratedFile(block.filePath(), block.language()));
            log.info("已生成文件: {}", filePath);
        }

        return generatedFiles;
    }

    /**
     * 从对话历史中提取所有代码块
     */
    public List<CodeBlock> extractCodeBlocks(List<ChatMessage> chatHistory) {
        List<CodeBlock> blocks = new ArrayList<>();

        for (ChatMessage msg : chatHistory) {
            String content = msg.content();
            if (content == null) continue;

            Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
            int lastEnd = 0;

            while (matcher.find()) {
                String language = matcher.group(1);
                String code = matcher.group(2);

                if (code == null || code.isBlank()) continue;

                // 尝试从代码块内首行提取文件名
                String filePath = extractFilenameFromCode(code);

                // 如果代码块内没有文件名，尝试从前面的文本提取
                if (filePath == null) {
                    String precedingText = content.substring(
                            Math.max(0, matcher.start() - 500), matcher.start());
                    filePath = extractFilenameFromPrecedingText(precedingText, language);
                }

                // 如果仍然没有文件名，尝试从代码内容推断
                if (filePath == null) {
                    filePath = inferFilenameFromCode(code, language);
                }

                // 如果提取到了文件名注释，从代码中移除该行
                if (filePath != null) {
                    code = removeFilenameComment(code);
                }

                blocks.add(new CodeBlock(filePath, language, code.strip()));
                lastEnd = matcher.end();
            }
        }

        return blocks;
    }

    private String extractFilenameFromCode(String code) {
        Matcher matcher = FILENAME_COMMENT_PATTERN.matcher(code);
        if (matcher.find()) {
            return sanitizePath(matcher.group(1).trim());
        }
        return null;
    }

    private String extractFilenameFromPrecedingText(String text, String language) {
        // 从后往前查找最近的文件名
        Matcher matcher = PRECEDING_FILENAME_PATTERN.matcher(text);
        String lastMatch = null;
        while (matcher.find()) {
            // 多个捕获组，取第一个非 null 的
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    lastMatch = matcher.group(i);
                    break;
                }
            }
        }
        return lastMatch != null ? sanitizePath(lastMatch) : null;
    }

    private String removeFilenameComment(String code) {
        return FILENAME_COMMENT_PATTERN.matcher(code).replaceFirst("").stripLeading();
    }

    /**
     * 从代码内容推断文件路径（Java: package + class name → path）
     */
    private String inferFilenameFromCode(String code, String language) {
        if ("java".equalsIgnoreCase(language)) {
            return inferJavaFilename(code);
        }
        if ("xml".equalsIgnoreCase(language) && code.contains("<project")) {
            return "pom.xml";
        }
        if ("properties".equalsIgnoreCase(language) || "yaml".equalsIgnoreCase(language) || "yml".equalsIgnoreCase(language)) {
            if (code.contains("spring.") || code.contains("server.port")) {
                String ext = "properties".equalsIgnoreCase(language) ? "properties" : "yml";
                return "src/main/resources/application." + ext;
            }
        }
        if ("html".equalsIgnoreCase(language)) {
            return "src/main/resources/static/index.html";
        }
        return null;
    }

    private String inferJavaFilename(String code) {
        // 提取 package
        Matcher pkgMatcher = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE).matcher(code);
        String pkg = pkgMatcher.find() ? pkgMatcher.group(1) : null;

        // 提取 class/interface/record/enum name
        Matcher classMatcher = Pattern.compile(
                "(?:public\\s+)?(?:class|interface|record|enum)\\s+(\\w+)").matcher(code);
        String className = classMatcher.find() ? classMatcher.group(1) : null;

        if (className == null) return null;

        String dir = "src/main/java/";
        if (pkg != null) {
            dir += pkg.replace('.', '/') + "/";
        }
        return dir + className + ".java";
    }

    private String sanitizePath(String path) {
        if (path == null) return null;
        // 移除开头的 / 避免绝对路径
        String sanitized = path.replaceAll("^/+", "");
        // 过滤危险路径
        if (sanitized.contains("..") || sanitized.startsWith("~")) {
            return null;
        }
        return sanitized;
    }

    public record CodeBlock(String filePath, String language, String content) {}
    public record GeneratedFile(String filePath, String language) {}
}

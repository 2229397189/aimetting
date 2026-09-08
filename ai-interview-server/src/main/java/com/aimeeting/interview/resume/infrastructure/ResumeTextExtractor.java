package com.aimeeting.interview.resume.infrastructure;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历文件文本抽取。
 *
 * <p>支持 TXT / MD / PDF：
 * <ul>
 *   <li>TXT / MD：直接按 UTF-8 读取；</li>
 *   <li>PDF：工程当前未引入 PDF 解析库（pom 禁止修改），使用「抽取内容流中括号文本」的
 *       启发式方式还原可读文本，仅用于演示/兜底，复杂排版可能失真。</li>
 * </ul>
 *
 * <p>类型不符抛 {@link BaseErrorCode#FILE_TYPE_UNSUPPORTED}（A0402）。
 */
public final class ResumeTextExtractor {

    /** 单个 PDF 文本算子最大长度，过滤超长噪声。 */
    private static final int MAX_TOKEN_LEN = 300;

    private static final Pattern PDF_TEXT_TOKEN =
            Pattern.compile("\\(((?:[^()\\\\]|\\\\[()\\\\]){1," + MAX_TOKEN_LEN + "})\\)");

    private ResumeTextExtractor() {
    }

    /**
     * 抽取简历文本。
     *
     * @param file 上传的文件
     * @return 抽取出的纯文本
     * @throws ClientException 类型不支持时
     */
    public static String extract(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean pdf = name.endsWith(".pdf") || contentType.contains("pdf");
        boolean text = name.endsWith(".txt") || name.endsWith(".md")
                || contentType.contains("text/plain") || contentType.contains("text/markdown");
        if (!pdf && !text) {
            throw new ClientException("仅支持 TXT / MD / PDF 文件", BaseErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        try {
            byte[] bytes = file.getBytes();
            if (pdf) {
                return extractPdf(bytes);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ClientException("文件读取失败", BaseErrorCode.FILE_TYPE_UNSUPPORTED);
        }
    }

    /**
     * 启发式抽取 PDF 文本：扫描内容流中的 {@code (...)} 文本算子，反转义后拼接。
     */
    private static String extractPdf(byte[] bytes) {
        String raw;
        try {
            raw = new String(bytes, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            raw = new String(bytes, StandardCharsets.UTF_8);
        }
        Matcher matcher = PDF_TEXT_TOKEN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String token = unescape(matcher.group(1));
            if (isLikelyText(token)) {
                sb.append(token).append(' ');
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            // 兜底：去掉控制字符直接返回可读片段
            result = raw.replaceAll("[^\\p{Print}\\p{InCJKUnifiedIdeographs}\\s]", " ")
                    .replaceAll("\\s+", " ").trim();
        }
        return result;
    }

    private static String unescape(String token) {
        return token.replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\");
    }

    private static boolean isLikelyText(String token) {
        if (token.length() < 2) {
            return false;
        }
        int printable = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetterOrDigit(c) || c > 0x2E80 || c == ' ' || c == '-' || c == '.' || c == '@') {
                printable++;
            }
        }
        return (double) printable / token.length() > 0.6;
    }
}

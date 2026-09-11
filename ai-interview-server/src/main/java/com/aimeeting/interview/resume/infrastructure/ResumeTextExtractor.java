package com.aimeeting.interview.resume.infrastructure;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历文件文本抽取。
 *
 * <p>支持 TXT / MD / DOCX / PDF：
 * <ul>
 *   <li>TXT / MD：直接按 UTF-8 读取；</li>
 *   <li>DOCX：标准库 zip + XML 解析 {@code word/document.xml} 中的 {@code <w:t>} 文本节点；</li>
 *   <li>PDF：优先用 Apache PDFBox 抽取，抽取为空或加密文件则回退到内容流启发式。</li>
 * </ul>
 *
 * <p>类型不符抛 {@link BaseErrorCode#FILE_TYPE_UNSUPPORTED}（A0402）。
 */
public final class ResumeTextExtractor {

    /** 单个 PDF 文本算子最大长度，过滤超长噪声。 */
    private static final int MAX_TOKEN_LEN = 300;

    private static final Pattern PDF_TEXT_TOKEN =
            Pattern.compile("\\(((?:[^()\\\\]|\\\\[()\\\\]){1," + MAX_TOKEN_LEN + "})\\)");

    private static final Pattern DOCX_TEXT = Pattern.compile("<w:t[^>]*>([^<]*)</w:t>");

    private static final Pattern DOCX_PARAGRAPH = Pattern.compile("<w:p[ >].*?</w:p>", Pattern.DOTALL);

    private ResumeTextExtractor() {
    }

    /**
     * 抽取简历文本。
     *
     * @param file 上传的文件
     * @return 抽取出的纯文本
     * @throws ClientException 类型不支持或解析失败时
     */
    public static String extract(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        boolean docx = name.endsWith(".docx") || contentType.contains("officedocument.wordprocessingml");
        boolean pdf = name.endsWith(".pdf") || contentType.contains("pdf");
        boolean text = name.endsWith(".txt") || name.endsWith(".md")
                || contentType.contains("text/plain") || contentType.contains("text/markdown");
        if (!docx && !pdf && !text) {
            throw new ClientException("仅支持 TXT / MD / DOCX / PDF 文件", BaseErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        try {
            byte[] bytes = file.getBytes();
            if (docx) {
                return extractDocx(bytes);
            }
            if (pdf) {
                return extractPdf(bytes);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ClientException("文件读取失败", BaseErrorCode.FILE_TYPE_UNSUPPORTED);
        }
    }

    /**
     * 解析 DOCX：读 {@code word/document.xml}，按段落拼接 {@code <w:t>} 文本节点。
     */
    private static String extractDocx(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zip.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                    return docxToText(out.toString(StandardCharsets.UTF_8));
                }
            }
        }
        throw new ClientException("DOCX 解析失败：未找到 word/document.xml", BaseErrorCode.FILE_TYPE_UNSUPPORTED);
    }

    private static String docxToText(String xml) {
        Matcher pm = DOCX_PARAGRAPH.matcher(xml);
        StringBuilder para = new StringBuilder();
        boolean any = false;
        while (pm.find()) {
            Matcher tm = DOCX_TEXT.matcher(pm.group());
            StringBuilder line = new StringBuilder();
            while (tm.find()) {
                line.append(tm.group(1));
            }
            if (line.length() > 0) {
                para.append(line).append('\n');
                any = true;
            }
        }
        if (any) {
            return para.toString().trim();
        }
        // 回退：直接拼所有文本节点（极少数无 <w:p> 的畸形文档）
        Matcher tm = DOCX_TEXT.matcher(xml);
        StringBuilder all = new StringBuilder();
        while (tm.find()) {
            all.append(tm.group(1));
        }
        return all.toString().trim();
    }

    /**
     * 抽取 PDF 文本：优先 PDFBox，失败或空则回退启发式。
     */
    private static String extractPdf(byte[] bytes) {
        String real = pdfBoxExtract(bytes);
        if (real != null && !real.isBlank()) {
            return real;
        }
        return extractPdfHeuristic(bytes);
    }

    private static String pdfBoxExtract(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            if (doc.isEncrypted()) {
                return null;
            }
            return new PDFTextStripper().getText(doc).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 启发式抽取 PDF 文本：扫描内容流中的 {@code (...)} 文本算子，反转义后拼接。
     */
    private static String extractPdfHeuristic(byte[] bytes) {
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

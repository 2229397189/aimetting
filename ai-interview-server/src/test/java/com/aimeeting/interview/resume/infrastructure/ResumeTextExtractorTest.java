package com.aimeeting.interview.resume.infrastructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.aimeeting.interview.common.convention.exception.ClientException;

/**
 * {@link ResumeTextExtractor} 解析能力自测：TXT / DOCX / PDF 走通，非法类型抛错。
 */
class ResumeTextExtractorTest {

    @Test
    void extractTxt() {
        MultipartFile f = new MockMultipartFile("file", "resume.txt", "text/plain",
                "陆强 Java 后端".getBytes(StandardCharsets.UTF_8));
        assertTrue(ResumeTextExtractor.extract(f).contains("陆强"));
    }

    @Test
    void extractDocx() throws Exception {
        byte[] docx = buildDocx("陆强", "Java 后端实习", "字节跳动 秋招");
        MultipartFile f = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);
        String text = ResumeTextExtractor.extract(f);
        assertTrue(text.contains("陆强"), "DOCX 应抽出行内姓名");
        assertTrue(text.contains("Java 后端实习"), "DOCX 应抽出行内经历");
        assertTrue(text.contains("字节跳动 秋招"), "DOCX 应抽取多段落");
    }

    @Test
    void extractPdf() throws Exception {
        // 注：PDFBox 标准 14 字体不含中文字形，这里用 ASCII 验证抽取链路
        byte[] pdf = buildPdf("Lu Qiang Backend Developer intern at ByteDance");
        MultipartFile f = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf);
        String text = ResumeTextExtractor.extract(f);
        assertTrue(text.contains("Lu Qiang"), "PDF 应抽出姓名");
        assertTrue(text.contains("Backend"), "PDF 应抽出关键词");
    }

    @Test
    void unsupportedTypeThrows() {
        MultipartFile f = new MockMultipartFile("file", "pic.png", "image/png", new byte[]{1, 2, 3});
        assertThrows(ClientException.class, () -> ResumeTextExtractor.extract(f));
    }

    private static byte[] buildDocx(String... lines) throws Exception {
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            body.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(line).append("</w:t></w:r></w:p>");
        }
        String xml = "<?xml version=\"1.0\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>" + body + "</w:body></w:document>";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write("<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>"
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private static byte[] buildPdf(String text) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(bos);
        }
        return bos.toByteArray();
    }
}

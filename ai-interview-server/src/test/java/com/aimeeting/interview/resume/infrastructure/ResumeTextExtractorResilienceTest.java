package com.aimeeting.interview.resume.infrastructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimeeting.interview.common.convention.exception.ClientException;
import org.junit.jupiter.api.DisplayName;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link ResumeTextExtractor} 解析鲁棒性测试：扫描件 / 空 PDF 必须给出清晰错误，
 * 而不是返回二进制乱码或被误判为「不是简历」。
 */
class ResumeTextExtractorResilienceTest {

    @Test
    @DisplayName("扫描件/空 PDF：给出清晰错误而非二进制乱码")
    void blankPdfThrowsClearError() {
        byte[] pdf = buildBlankPdf();
        MultipartFile f = new MockMultipartFile("file", "scan.pdf", "application/pdf", pdf);
        ClientException ex = assertThrows(ClientException.class, () -> ResumeTextExtractor.extract(f));
        assertTrue(ex.getMessage().contains("PDF"), "应明确提示 PDF 文本不可提取：" + ex.getMessage());
        assertTrue(ex.getMessage().indexOf('�') < 0, "不应返回二进制乱码");
    }

    @Test
    @DisplayName("正常文本 PDF 仍可抽取（修复不能破坏既有链路）")
    void normalPdfStillWorks() throws Exception {
        byte[] pdf = buildTextPdf("Lu Qiang Java Backend intern ByteDance");
        MultipartFile f = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf);
        String text = ResumeTextExtractor.extract(f);
        assertTrue(text.contains("Lu Qiang"), "PDF 应抽出姓名");
        assertTrue(text.contains("Backend"), "PDF 应抽出关键词");
    }

    private static byte[] buildBlankPdf() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new PDPage());
                doc.save(bos);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] buildTextPdf(String text) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

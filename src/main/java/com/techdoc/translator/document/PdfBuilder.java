package com.techdoc.translator.document;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.techdoc.translator.common.ApiException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 결과를 PDF로 만든다. 한글이 깨지지 않도록 NanumGothic(SIL OFL 라이선스)을 파일에 포함한다.
 * 글꼴 파일: src/main/resources/fonts/NanumGothic-Regular.ttf (라이선스는 같은 폴더의 NanumGothic-OFL.txt)
 */
final class PdfBuilder {

    private static final String FONT_RESOURCE = "/fonts/NanumGothic-Regular.ttf";

    private static volatile byte[] fontBytes;

    private PdfBuilder() {}

    static byte[] build(String title, String body) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BaseFont baseFont = BaseFont.createFont("NanumGothic-Regular.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, loadFont(), null);
            Font titleFont = new Font(baseFont, 18);
            Font bodyFont = new Font(baseFont, 11);

            Document pdf = new Document(PageSize.A4, 56, 56, 64, 64);
            PdfWriter.getInstance(pdf, out);
            pdf.open();

            Paragraph heading = new Paragraph(title, titleFont);
            heading.setSpacingAfter(18);
            pdf.add(heading);

            for (String block : body.split("\\n\\s*\\n")) {
                String text = block.trim();
                if (text.isEmpty()) {
                    continue;
                }
                Paragraph paragraph = new Paragraph(text, bodyFont);
                paragraph.setLeading(18f);
                paragraph.setSpacingAfter(10);
                pdf.add(paragraph);
            }
            pdf.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_BUILD_FAILED", "PDF 파일을 만들지 못했습니다.");
        }
    }

    private static byte[] loadFont() throws IOException {
        byte[] bytes = fontBytes;
        if (bytes == null) {
            try (InputStream in = PdfBuilder.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in == null) {
                    throw new IOException("글꼴 파일을 찾을 수 없습니다: " + FONT_RESOURCE);
                }
                bytes = in.readAllBytes();
            }
            fontBytes = bytes;
        }
        return bytes;
    }
}

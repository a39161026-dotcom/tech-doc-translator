package com.techdoc.translator.summary;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:summary-flow;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class SummaryFlowTest {

    private static final String SIX = "A.\n\nB.\n\nC.\n\nD.\n\nE.\n\nF.";

    @Autowired
    MockMvc mockMvc;

    private long upload(String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", "text/plain", content.getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long translatedDocument(String content) throws Exception {
        long id = upload(content);
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());
        return id;
    }

    private void summarize(long id, String length) throws Exception {
        mockMvc.perform(post("/api/documents/" + id + "/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"length\":\"" + length + "\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void 길이를_골라_요약하고_다시_요약하면_덮어쓴다() throws Exception {
        long id = translatedDocument(SIX);

        summarize(id, "SHORT");
        mockMvc.perform(get("/api/documents/" + id + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.length").value("SHORT"))
                .andExpect(jsonPath("$.text").value("[요약] [번역] A."));

        summarize(id, "MEDIUM");
        mockMvc.perform(get("/api/documents/" + id + "/summary"))
                .andExpect(jsonPath("$.length").value("MEDIUM"))
                .andExpect(jsonPath("$.text").value("[요약] [번역] A.\n\n[번역] B.\n\n[번역] C."));

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.summaryStatus").value("DONE"))
                .andExpect(jsonPath("$.summaryLength").value("MEDIUM"));
    }

    @Test
    void 번역_전에는_요약할_수_없다() throws Exception {
        long id = upload("Not translated yet.");

        mockMvc.perform(post("/api/documents/" + id + "/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"length\":\"SHORT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_READY"));
    }

    @Test
    void 길이가_없거나_잘못되면_400() throws Exception {
        long id = translatedDocument("One.");

        mockMvc.perform(post("/api/documents/" + id + "/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/documents/" + id + "/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"length\":\"HUGE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 요약이_실패하면_FAILED로_표시된다() throws Exception {
        long id = translatedDocument("Good.\n\nSUMMARY_ERROR here.");

        summarize(id, "SHORT");
        mockMvc.perform(get("/api/documents/" + id + "/summary"))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void 요약본을_docx로_내려받는다() throws Exception {
        long id = translatedDocument(SIX);
        summarize(id, "SHORT");

        byte[] bytes = mockMvc.perform(get("/api/documents/" + id + "/result")
                        .param("type", "summary").param("format", "docx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andReturn().getResponse().getContentAsByteArray();

        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = docx.getParagraphs().stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
            assertTrue(text.contains("guide (요약)"), text);
            assertTrue(text.contains("[요약] [번역] A."), text);
        }
    }

    @Test
    void 번역_전문도_docx로_받을_수_있다() throws Exception {
        long id = translatedDocument("Hello.\n\nWorld.");

        byte[] bytes = mockMvc.perform(get("/api/documents/" + id + "/result").param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = docx.getParagraphs().stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
            assertTrue(text.contains("[번역] Hello."), text);
            assertTrue(text.contains("[번역] World."), text);
        }
    }

    @Test
    void 요약_전에_요약본을_받으려_하면_409() throws Exception {
        long id = translatedDocument("Only translated.");

        mockMvc.perform(get("/api/documents/" + id + "/result").param("type", "summary"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUMMARY_NOT_READY"));
    }

    @Test
    void 잘못된_type은_400() throws Exception {
        long id = translatedDocument("Any.");

        mockMvc.perform(get("/api/documents/" + id + "/result").param("type", "all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_TYPE"));
    }
}

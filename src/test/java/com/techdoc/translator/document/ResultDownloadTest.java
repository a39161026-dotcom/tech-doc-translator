package com.techdoc.translator.document;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
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
        "spring.datasource.url=jdbc:h2:mem:result-download;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class ResultDownloadTest {

    @Autowired
    MockMvc mockMvc;

    private long upload(String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", "text/plain", content.getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 번역이_끝난_문서는_파일로_내려받는다() throws Exception {
        long id = upload("Hello.\n\nWorld.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id + "/result"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string("[번역] Hello.\n\n[번역] World.\n"));
    }

    @Test
    void txt_형식도_받을_수_있다() throws Exception {
        long id = upload("Only one.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id + "/result").param("format", "txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("[번역] Only one.\n"));
    }

    @Test
    void 번역_전에는_409() throws Exception {
        long id = upload("Not yet.");

        mockMvc.perform(get("/api/documents/" + id + "/result"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_READY"));
    }

    @Test
    void 지원하지_않는_형식은_400() throws Exception {
        long id = upload("Any.");

        mockMvc.perform(get("/api/documents/" + id + "/result").param("format", "exe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
    }

    @Test
    void 없는_문서는_404() throws Exception {
        mockMvc.perform(get("/api/documents/9999/result"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }
}

package com.techdoc.translator.document;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "spring.datasource.url=jdbc:h2:mem:document-upload;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class DocumentUploadTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void txt를_올리면_문단으로_나뉘어_저장된다() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "guide.txt", "text/plain",
                "Hello world.\n\nSecond paragraph.\n\nThird.".getBytes());

        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("guide"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.segmentCount").value(3))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/documents/" + id + "/segments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].seq").value(1))
                .andExpect(jsonPath("$[0].sourceText").value("Hello world."))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void 지원하지_않는_형식은_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", "x".getBytes());

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void 없는_문서는_404() throws Exception {
        mockMvc.perform(get("/api/documents/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }
}

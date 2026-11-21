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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:document-list;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class DocumentListTest {

    @Autowired
    MockMvc mockMvc;

    private long upload(String title, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", title + ".txt", "text/plain", content.getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 올린_문서가_최신순으로_목록에_나온다() throws Exception {
        long first = upload("first", "One.");
        long second = upload("second", "Two.\n\nThree.");
        mockMvc.perform(post("/api/documents/" + second + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + first + ")].title").value("first"))
                .andExpect(jsonPath("$[?(@.id==" + second + ")].status").value("DONE"))
                .andExpect(jsonPath("$[?(@.id==" + second + ")].doneCount").value(2))
                .andExpect(jsonPath("$[0].id").value((int) second));
    }
}

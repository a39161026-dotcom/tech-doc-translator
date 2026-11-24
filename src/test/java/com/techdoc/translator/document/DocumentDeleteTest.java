package com.techdoc.translator.document;

import com.jayway.jsonpath.JsonPath;
import com.techdoc.translator.segment.SegmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:document-delete;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class DocumentDeleteTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    SegmentRepository segmentRepository;

    private long upload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "d.txt", "text/plain", "One.\n\nTwo.".getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 문서를_삭제하면_문서와_문단이_사라진다() throws Exception {
        long id = upload();

        mockMvc.perform(delete("/api/documents/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/" + id)).andExpect(status().isNotFound());
        assertEquals(0, segmentRepository.countByDocumentId(id));
    }

    @Test
    void 없는_문서를_삭제하면_404() throws Exception {
        mockMvc.perform(delete("/api/documents/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void 번역_중인_문서는_삭제할_수_없다() throws Exception {
        long id = upload();
        Document document = documentRepository.findById(id).orElseThrow();
        document.changeStatus(DocumentStatus.TRANSLATING);
        documentRepository.save(document);

        mockMvc.perform(delete("/api/documents/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_BUSY"));
    }
}

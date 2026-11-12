package com.techdoc.translator.term;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping
    public List<TermResponse> list() {
        return termService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermResponse create(@Valid @RequestBody TermRequest request) {
        return termService.create(request);
    }

    @PutMapping("/{id}")
    public TermResponse update(@PathVariable Long id, @Valid @RequestBody TermRequest request) {
        return termService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        termService.delete(id);
    }
}

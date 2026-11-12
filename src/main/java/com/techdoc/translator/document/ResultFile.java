package com.techdoc.translator.document;

import org.springframework.http.MediaType;

/** 다운로드용 번역 결과 파일. */
public record ResultFile(String filename, MediaType mediaType, byte[] content) {
}

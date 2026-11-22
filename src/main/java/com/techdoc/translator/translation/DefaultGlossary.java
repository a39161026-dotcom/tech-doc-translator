package com.techdoc.translator.translation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 기본 용어집. 회원이 따로 등록하지 않아도 번역할 때 쓰는 용어.
 *
 * 작은 로컬 모델(7b)이 영어 단어를 다른 언어(러시아어 등)로 번역하며 새는 경우가 있어서,
 * 자주 나오는 IT 용어는 번역어를 미리 알려 준다. 원문에 들어 있는 용어만 프롬프트에 붙는다.
 *
 * 확인된 것: "operations"가 러시아어로 새는 경우 (qwen2.5:7b, 문서 1개).
 * 나머지는 일반적으로 쓰는 번역어를 고른 것이고 모델에서 따로 시험하지 않았다.
 * 회원 용어집에 같은 원어가 있으면 회원 용어집이 우선한다.
 */
final class DefaultGlossary {

    private static final Map<String, String> TERMS = new LinkedHashMap<>();

    static {
        TERMS.put("operations", "작업");
        TERMS.put("operation", "작업");
        TERMS.put("dependency injection", "의존성 주입");
        TERMS.put("dependency", "의존성");
        TERMS.put("request", "요청");
        TERMS.put("response", "응답");
        TERMS.put("endpoint", "엔드포인트");
        TERMS.put("repository", "리포지토리");
        TERMS.put("transaction", "트랜잭션");
        TERMS.put("thread", "스레드");
        TERMS.put("cache", "캐시");
        TERMS.put("query", "쿼리");
        TERMS.put("database", "데이터베이스");
        TERMS.put("configuration", "설정");
        TERMS.put("deployment", "배포");
        TERMS.put("authentication", "인증");
        TERMS.put("authorization", "인가");
        TERMS.put("exception", "예외");
        TERMS.put("annotation", "애노테이션");
        TERMS.put("framework", "프레임워크");
        TERMS.put("library", "라이브러리");
        TERMS.put("middleware", "미들웨어");
        TERMS.put("container", "컨테이너");
        TERMS.put("deprecated", "사용 중단(deprecated)");
    }

    private DefaultGlossary() {}

    /** 기본 용어에 회원 용어를 덧씌운다. 같은 원어는 대소문자 구분 없이 회원 용어가 이긴다. */
    static Map<String, String> merge(Map<String, String> userTerms) {
        Map<String, String> merged = new LinkedHashMap<>(TERMS);
        for (String userKey : userTerms.keySet()) {
            merged.keySet().removeIf(defaultKey -> defaultKey.equalsIgnoreCase(userKey));
        }
        merged.putAll(userTerms);
        return merged;
    }
}

package com.guan.rag.module.evaluation;

import com.guan.rag.module.evaluation.response.EvaluationTestCaseResponse;

import java.util.List;

/**
 * V7 内置评测用例（与 Debug / Chat 快捷问题对齐）。
 */
public final class EvaluationTestCatalog {

    private EvaluationTestCatalog() {
    }

    public static List<EvaluationTestCaseResponse> builtinCases() {
        return List.of(
                caseOf("sms_429", "SMS_429 是什么意思？", "02-api-spec.md"),
                caseOf("send_code", "send-code 接口路径是什么？", "02-api-spec.md"),
                caseOf("send_code_path", "/api/sms/send-code 是什么接口？", "02-api-spec.md"),
                caseOf("sms_troubleshoot", "短信验证码发不出去怎么排查？", "03-troubleshooting.md"),
                caseOf("pgvector", "这个项目为什么后续会使用 PgVector？", "01-project-guideline.md")
        );
    }

    private static EvaluationTestCaseResponse caseOf(String id, String question, String expectedDocument) {
        return EvaluationTestCaseResponse.builder()
                .caseId(id)
                .question(question)
                .expectedDocument(expectedDocument)
                .build();
    }
}

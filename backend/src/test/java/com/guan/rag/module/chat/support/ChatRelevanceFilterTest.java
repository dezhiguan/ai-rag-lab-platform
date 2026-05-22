package com.guan.rag.module.chat.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRelevanceFilterTest {

    @Test
    void relatedQuestionOverlapsTroubleshootingContent() {
        assertTrue(ChatRelevanceFilter.hasNgramOverlap(
                "短信验证码发不出去怎么排查？",
                "当用户反馈收不到验证码时，确认未触发 SMS_429，检查 Redis 缓存。"
        ));
    }

    @Test
    void unrelatedQuestionDoesNotOverlapKbContent() {
        assertFalse(ChatRelevanceFilter.hasNgramOverlap(
                "公司年终奖发几个月？",
                "POST /api/sms/send-code 发送短信验证码，PgVector 存储向量。"
        ));
    }
}

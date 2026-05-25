package com.guan.rag.module.token;

import com.guan.rag.module.chat.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private final PromptBuilder promptBuilder;

    private record ModelPrice(double inputPerMillionYuan, double outputPerMillionYuan) {}

    private static final Map<String, ModelPrice> MODEL_PRICES = Map.of(
            "deepseek-chat", new ModelPrice(1.0, 2.0),
            "deepseek-v4-pro", new ModelPrice(3.0, 6.0),
            "mock", new ModelPrice(0.0, 0.0),
            "local", new ModelPrice(0.0, 0.0)
    );

    private static final Map<String, ModelPrice> PROVIDER_PRICES = Map.of(
            "deepseek", new ModelPrice(1.0, 2.0),
            "mock", new ModelPrice(0.0, 0.0),
            "local", new ModelPrice(0.0, 0.0)
    );

    public int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isCjk(c)) {
                cjk++;
            } else {
                other++;
            }
        }
        return cjk + (other + 3) / 4;
    }

    public TokenUsageResult buildUsage(
            String question,
            String context,
            String answer,
            String chatProvider,
            String chatModel,
            Integer apiPromptTokens,
            Integer apiCompletionTokens
    ) {
        String safeQuestion = question == null ? "" : question;
        String safeContext = context == null ? "" : context;
        String safeAnswer = answer == null ? "" : answer;

        int questionTokens = estimateTokens(safeQuestion);
        int contextTokens = estimateTokens(safeContext);
        int systemPromptTokens = estimateTokens(promptBuilder.buildSystemShell());
        int answerTokens = apiCompletionTokens != null && apiCompletionTokens > 0
                ? apiCompletionTokens
                : estimateTokens(safeAnswer);

        int inputTokens = questionTokens + contextTokens + systemPromptTokens;
        if (apiPromptTokens != null && apiPromptTokens > 0) {
            inputTokens = apiPromptTokens;
        }
        int outputTokens = answerTokens;
        int totalTokens = inputTokens + outputTokens;

        Optional<ModelPrice> price = resolvePrice(chatProvider, chatModel);
        boolean priceConfigured = price.isPresent();
        BigDecimal estimatedCost = BigDecimal.ZERO;
        String costNote = "未配置价格";
        if (priceConfigured) {
            ModelPrice p = price.get();
            estimatedCost = BigDecimal.valueOf(inputTokens)
                    .multiply(BigDecimal.valueOf(p.inputPerMillionYuan()))
                    .divide(MILLION, 8, RoundingMode.HALF_UP)
                    .add(BigDecimal.valueOf(outputTokens)
                            .multiply(BigDecimal.valueOf(p.outputPerMillionYuan()))
                            .divide(MILLION, 8, RoundingMode.HALF_UP))
                    .setScale(6, RoundingMode.HALF_UP);
            costNote = "按模型单价估算（元）";
        }

        return TokenUsageResult.builder()
                .questionTokens(questionTokens)
                .contextTokens(contextTokens)
                .systemPromptTokens(systemPromptTokens)
                .answerTokens(answerTokens)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .estimatedCost(estimatedCost)
                .priceConfigured(priceConfigured)
                .costNote(costNote)
                .chatProvider(chatProvider)
                .chatModel(chatModel)
                .build();
    }

    public TokenUsageResult fromPersisted(
            Integer questionTokens,
            Integer contextTokens,
            Integer systemPromptTokens,
            Integer answerTokens,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            BigDecimal estimatedCost,
            Boolean priceConfigured,
            String chatProvider,
            String chatModel
    ) {
        if (totalTokens == null || totalTokens <= 0) {
            return null;
        }
        return TokenUsageResult.builder()
                .questionTokens(nullToZero(questionTokens))
                .contextTokens(nullToZero(contextTokens))
                .systemPromptTokens(nullToZero(systemPromptTokens))
                .answerTokens(nullToZero(answerTokens))
                .inputTokens(nullToZero(inputTokens))
                .outputTokens(nullToZero(outputTokens))
                .totalTokens(totalTokens)
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .priceConfigured(Boolean.TRUE.equals(priceConfigured))
                .costNote(Boolean.TRUE.equals(priceConfigured) ? "按模型单价估算（元）" : "未配置价格")
                .chatProvider(chatProvider)
                .chatModel(chatModel)
                .build();
    }

    private Optional<ModelPrice> resolvePrice(String chatProvider, String chatModel) {
        if (StringUtils.hasText(chatModel)) {
            String key = chatModel.trim().toLowerCase(Locale.ROOT);
            if (MODEL_PRICES.containsKey(key)) {
                return Optional.of(MODEL_PRICES.get(key));
            }
        }
        if (StringUtils.hasText(chatProvider)) {
            String key = chatProvider.trim().toLowerCase(Locale.ROOT);
            if (PROVIDER_PRICES.containsKey(key)) {
                return Optional.of(PROVIDER_PRICES.get(key));
            }
        }
        return Optional.empty();
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }
}

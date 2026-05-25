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
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /** qwen text-embedding-v4：元 / 千 tokens */
    private static final BigDecimal QWEN_EMBEDDING_PRICE_PER_1K = new BigDecimal("0.0007");

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
            String embeddingProvider,
            String embeddingModel,
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

        int chatInputTokens = questionTokens + contextTokens + systemPromptTokens;
        if (apiPromptTokens != null && apiPromptTokens > 0) {
            chatInputTokens = apiPromptTokens;
        }
        int chatOutputTokens = answerTokens;

        BigDecimal chatCost = computeChatCost(chatProvider, chatModel, chatInputTokens, chatOutputTokens);

        int embeddingTokens = estimateTokens(safeQuestion);
        BigDecimal embeddingCost = computeEmbeddingCost(embeddingProvider, embeddingModel, embeddingTokens);

        int totalTokens = embeddingTokens + chatInputTokens + chatOutputTokens;
        BigDecimal totalCost = embeddingCost.add(chatCost).setScale(8, RoundingMode.HALF_UP);

        boolean priceConfigured = resolveChatPrice(chatProvider, chatModel).isPresent()
                || isQwenEmbedding(embeddingProvider, embeddingModel);
        String costNote = priceConfigured ? "Embedding + Chat 分项估算（元）" : "未配置价格";

        return TokenUsageResult.builder()
                .questionTokens(questionTokens)
                .contextTokens(contextTokens)
                .systemPromptTokens(systemPromptTokens)
                .answerTokens(answerTokens)
                .inputTokens(chatInputTokens)
                .outputTokens(chatOutputTokens)
                .embeddingProvider(embeddingProvider)
                .embeddingModel(embeddingModel)
                .embeddingTokens(embeddingTokens)
                .embeddingCost(embeddingCost)
                .chatProvider(chatProvider)
                .chatModel(chatModel)
                .chatInputTokens(chatInputTokens)
                .chatOutputTokens(chatOutputTokens)
                .chatCost(chatCost)
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .estimatedCost(totalCost)
                .priceConfigured(priceConfigured)
                .costNote(costNote)
                .build();
    }

    public TokenUsageResult fromPersisted(
            DebugQueryLogTokenFields fields
    ) {
        if (fields.totalTokens() == null || fields.totalTokens() <= 0) {
            if (fields.inputTokens() == null && fields.embeddingTokens() == null) {
                return null;
            }
        }

        int embeddingTokens = nullToLongInt(fields.embeddingTokens());
        BigDecimal embeddingCost = fields.embeddingCost() != null
                ? fields.embeddingCost()
                : BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);

        int chatInput = nullToZero(fields.inputTokens());
        int chatOutput = nullToZero(fields.outputTokens());
        BigDecimal chatCost = fields.estimatedCost() != null
                ? fields.estimatedCost()
                : BigDecimal.ZERO;

        int totalTokens = fields.totalTokens() != null && fields.totalTokens() > 0
                ? fields.totalTokens()
                : embeddingTokens + chatInput + chatOutput;

        BigDecimal totalCost = fields.totalCost() != null
                ? fields.totalCost()
                : embeddingCost.add(chatCost);

        boolean priceConfigured = (fields.priceConfigured() != null && fields.priceConfigured() == 1)
                || totalCost.compareTo(BigDecimal.ZERO) > 0;

        return TokenUsageResult.builder()
                .questionTokens(nullToZero(fields.questionTokens()))
                .contextTokens(nullToZero(fields.contextTokens()))
                .systemPromptTokens(nullToZero(fields.systemPromptTokens()))
                .answerTokens(nullToZero(fields.answerTokens()))
                .inputTokens(chatInput)
                .outputTokens(chatOutput)
                .embeddingProvider(fields.embeddingProvider())
                .embeddingModel(fields.embeddingModel())
                .embeddingTokens(embeddingTokens)
                .embeddingCost(scale8(embeddingCost))
                .chatProvider(fields.chatProvider())
                .chatModel(fields.chatModel())
                .chatInputTokens(chatInput)
                .chatOutputTokens(chatOutput)
                .chatCost(scale8(chatCost))
                .totalTokens(totalTokens)
                .totalCost(scale8(totalCost))
                .estimatedCost(scale8(totalCost))
                .priceConfigured(priceConfigured)
                .costNote(priceConfigured ? "Embedding + Chat 分项估算（元）" : "未配置价格")
                .build();
    }

    /** 兼容旧版 fromPersisted 参数列表 */
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
        return fromPersisted(new DebugQueryLogTokenFields(
                questionTokens,
                contextTokens,
                systemPromptTokens,
                answerTokens,
                inputTokens,
                outputTokens,
                totalTokens,
                estimatedCost,
                null,
                null,
                null,
                priceConfigured == null ? null : (priceConfigured ? 1 : 0),
                null,
                null,
                chatProvider,
                chatModel
        ));
    }

    public record DebugQueryLogTokenFields(
            Integer questionTokens,
            Integer contextTokens,
            Integer systemPromptTokens,
            Integer answerTokens,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            BigDecimal estimatedCost,
            Long embeddingTokens,
            BigDecimal embeddingCost,
            BigDecimal totalCost,
            Integer priceConfigured,
            String embeddingProvider,
            String embeddingModel,
            String chatProvider,
            String chatModel
    ) {
    }

    private BigDecimal computeChatCost(String chatProvider, String chatModel, int inputTokens, int outputTokens) {
        Optional<ModelPrice> price = resolveChatPrice(chatProvider, chatModel);
        if (price.isEmpty()) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        ModelPrice p = price.get();
        return BigDecimal.valueOf(inputTokens)
                .multiply(BigDecimal.valueOf(p.inputPerMillionYuan()))
                .divide(MILLION, 8, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(outputTokens)
                        .multiply(BigDecimal.valueOf(p.outputPerMillionYuan()))
                        .divide(MILLION, 8, RoundingMode.HALF_UP))
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal computeEmbeddingCost(String embeddingProvider, String embeddingModel, int embeddingTokens) {
        if (!isQwenEmbedding(embeddingProvider, embeddingModel) || embeddingTokens <= 0) {
            if ("mock".equalsIgnoreCase(normalize(embeddingProvider))
                    || "local".equalsIgnoreCase(normalize(embeddingProvider))) {
                return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(embeddingTokens)
                .divide(THOUSAND, 8, RoundingMode.HALF_UP)
                .multiply(QWEN_EMBEDDING_PRICE_PER_1K)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private boolean isQwenEmbedding(String embeddingProvider, String embeddingModel) {
        if ("qwen".equalsIgnoreCase(normalize(embeddingProvider))) {
            String model = normalize(embeddingModel);
            return !StringUtils.hasText(model)
                    || model.contains("text-embedding")
                    || "text-embedding-v4".equals(model);
        }
        return "text-embedding-v4".equalsIgnoreCase(normalize(embeddingModel));
    }

    private Optional<ModelPrice> resolveChatPrice(String chatProvider, String chatModel) {
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static int nullToLongInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static BigDecimal scale8(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP)
                : value.setScale(8, RoundingMode.HALF_UP);
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

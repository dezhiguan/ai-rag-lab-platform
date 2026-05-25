package com.guan.rag.module.debug.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_query_log")
public class DebugQueryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String question;

    private String prompt;

    private String context;

    private String answer;

    private Integer topK;

    private String searchMode;

    /** 1=启用轻量 Reranker，0=未启用 */
    private Integer enableRerank;

    private String embeddingProvider;

    private String embeddingModel;

    private String chatProvider;

    private String chatModel;

    private Long retrievalTimeMs;

    private Long generationTimeMs;

    private Long totalTimeMs;

    private Integer questionTokens;
    private Integer contextTokens;
    private Integer systemPromptTokens;
    private Integer answerTokens;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    /** Chat 分项预估费用（元） */
    private BigDecimal estimatedCost;
    private Long embeddingTokens;
    private BigDecimal embeddingCost;
    /** Embedding + Chat 总费用（元） */
    private BigDecimal totalCost;
    /** 1=已配置模型单价，0=未配置 */
    private Integer priceConfigured;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}

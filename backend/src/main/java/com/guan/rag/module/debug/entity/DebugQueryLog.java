package com.guan.rag.module.debug.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    private String embeddingProvider;

    private String embeddingModel;

    private String chatProvider;

    private String chatModel;

    private Long retrievalTimeMs;

    private Long generationTimeMs;

    private Long totalTimeMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}

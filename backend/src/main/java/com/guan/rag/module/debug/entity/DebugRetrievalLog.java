package com.guan.rag.module.debug.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_log")
public class DebugRetrievalLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long queryLogId;

    private Long kbId;

    private Long documentId;

    private String documentName;

    private Long chunkId;

    private Integer chunkIndex;

    private Double score;

    private String content;

    private Integer rankPosition;

    private Integer usedInPrompt;

    private String filterReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

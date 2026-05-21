package com.guan.rag.module.embedding.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chunk_embedding")
public class ChunkEmbedding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private Long documentId;

    private Long chunkId;

    private String embeddingModel;

    private Integer embeddingDimension;

    @TableField("embedding")
    private String embeddingVector;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

package com.guan.rag.module.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guan.rag.module.document.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}

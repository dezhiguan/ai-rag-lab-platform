package com.guan.rag.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guan.rag.module.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}

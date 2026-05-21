package com.guan.rag.module.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.common.util.HashUtils;
import com.guan.rag.config.RagProperties;
import com.guan.rag.module.document.entity.Document;
import com.guan.rag.module.document.entity.DocumentChunk;
import com.guan.rag.module.document.enums.DocumentStatus;
import com.guan.rag.module.document.mapper.DocumentChunkMapper;
import com.guan.rag.module.document.mapper.DocumentMapper;
import com.guan.rag.module.document.parser.DocumentParser;
import com.guan.rag.module.document.response.DocumentChunkResponse;
import com.guan.rag.module.document.response.DocumentResponse;
import com.guan.rag.module.document.splitter.DocumentSplitter;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final List<DocumentParser> documentParsers;
    private final DocumentSplitter documentSplitter;
    private final RagProperties ragProperties;

    @Transactional
    public DocumentResponse upload(Long kbId, MultipartFile file) {
        knowledgeBaseService.requireKb(kbId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名无效");
        }

        String fileType = extractFileType(originalFilename);
        DocumentParser parser = resolveParser(fileType);

        Document document = new Document();
        document.setKbId(kbId);
        document.setFileName(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.UPLOADED.name());
        documentMapper.insert(document);

        try {
            Path savedPath = saveFile(kbId, document.getId(), file);
            document.setStoragePath(savedPath.toString());
            document.setContentHash(HashUtils.sha256(savedPath.toString() + file.getSize()));
            documentMapper.updateById(document);

            processDocument(document, parser);
            return toResponse(documentMapper.selectById(document.getId()));
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED.name());
            document.setErrorMessage(e.getMessage());
            documentMapper.updateById(document);
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    @Transactional
    public DocumentResponse importFromClasspath(Long kbId, String fileName, byte[] content) throws IOException {
        knowledgeBaseService.requireKb(kbId);
        String fileType = extractFileType(fileName);
        DocumentParser parser = resolveParser(fileType);

        Document document = new Document();
        document.setKbId(kbId);
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFileSize((long) content.length);
        document.setStatus(DocumentStatus.UPLOADED.name());
        documentMapper.insert(document);

        Path savedPath = saveBytes(kbId, document.getId(), fileName, content);
        document.setStoragePath(savedPath.toString());
        document.setContentHash(HashUtils.sha256(new String(content)));
        documentMapper.updateById(document);

        processDocument(document, parser);
        return toResponse(documentMapper.selectById(document.getId()));
    }

    public List<DocumentResponse> listByKbId(Long kbId) {
        knowledgeBaseService.requireKb(kbId);
        return documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKbId, kbId)
                        .orderByDesc(Document::getCreatedAt)
        ).stream().map(this::toResponse).toList();
    }

    public DocumentResponse getById(Long documentId) {
        return toResponse(requireDocument(documentId));
    }

    public List<DocumentChunkResponse> listChunks(Long documentId) {
        Document document = requireDocument(documentId);
        return documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, document.getId())
                        .orderByAsc(DocumentChunk::getChunkIndex)
        ).stream().map(this::toChunkResponse).toList();
    }

    public long count() {
        return documentMapper.selectCount(null);
    }

    public long countChunks() {
        return documentChunkMapper.selectCount(null);
    }

    public int countByKbId(Long kbId) {
        return documentMapper.selectCount(
                new LambdaQueryWrapper<Document>().eq(Document::getKbId, kbId)
        ).intValue();
    }

    public boolean existsByKbIdAndFileName(Long kbId, String fileName) {
        return documentMapper.selectCount(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKbId, kbId)
                        .eq(Document::getFileName, fileName)
        ) > 0;
    }

    private void processDocument(Document document, DocumentParser parser) throws IOException {
        updateStatus(document, DocumentStatus.PARSING);
        Path filePath = Paths.get(document.getStoragePath());
        String text = parser.parse(filePath);
        updateStatus(document, DocumentStatus.PARSED);

        updateStatus(document, DocumentStatus.CHUNKING);
        documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, document.getId())
        );

        List<DocumentSplitter.SplitChunk> chunks = documentSplitter.split(text);
        for (DocumentSplitter.SplitChunk chunk : chunks) {
            DocumentChunk entity = new DocumentChunk();
            entity.setKbId(document.getKbId());
            entity.setDocumentId(document.getId());
            entity.setChunkIndex(chunk.chunkIndex());
            entity.setContent(chunk.content());
            entity.setTokenCount(chunk.tokenCount());
            entity.setContentHash(chunk.contentHash());
            documentChunkMapper.insert(entity);
        }

        document.setContentHash(HashUtils.sha256(text));
        updateStatus(document, DocumentStatus.COMPLETED);
    }

    private void updateStatus(Document document, DocumentStatus status) {
        document.setStatus(status.name());
        document.setErrorMessage(null);
        documentMapper.updateById(document);
    }

    private Path saveFile(Long kbId, Long documentId, MultipartFile file) throws IOException {
        Path dir = ensureUploadDir(kbId, documentId);
        String safeName = sanitizeFileName(file.getOriginalFilename());
        Path target = dir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private Path saveBytes(Long kbId, Long documentId, String fileName, byte[] content) throws IOException {
        Path dir = ensureUploadDir(kbId, documentId);
        Path target = dir.resolve(sanitizeFileName(fileName));
        Files.write(target, content);
        return target;
    }

    private Path ensureUploadDir(Long kbId, Long documentId) throws IOException {
        Path dir = Paths.get(ragProperties.getStorage().getPath(), String.valueOf(kbId), String.valueOf(documentId));
        Files.createDirectories(dir);
        return dir;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private DocumentParser resolveParser(String fileType) {
        return documentParsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new BusinessException("仅支持 Markdown(.md) 和 TXT(.txt) 文件"));
    }

    private String extractFileType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException("无法识别文件类型");
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        return document;
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .kbId(document.getKbId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .contentHash(document.getContentHash())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private DocumentChunkResponse toChunkResponse(DocumentChunk chunk) {
        return DocumentChunkResponse.builder()
                .id(chunk.getId())
                .kbId(chunk.getKbId())
                .documentId(chunk.getDocumentId())
                .chunkIndex(chunk.getChunkIndex())
                .titlePath(chunk.getTitlePath())
                .content(chunk.getContent())
                .tokenCount(chunk.getTokenCount())
                .contentHash(chunk.getContentHash())
                .createdAt(chunk.getCreatedAt())
                .build();
    }
}

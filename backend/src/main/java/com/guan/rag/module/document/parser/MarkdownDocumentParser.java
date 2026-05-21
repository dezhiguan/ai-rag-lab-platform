package com.guan.rag.module.document.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
public class MarkdownDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED = Set.of("md", "markdown");

    @Override
    public boolean supports(String fileType) {
        return SUPPORTED.contains(fileType.toLowerCase());
    }

    @Override
    public String parse(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
}

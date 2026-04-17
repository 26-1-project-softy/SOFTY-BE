package com.softy.be.report.service;

import com.softy.be.chat.entity.Message;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportPdfRenderService {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final float MARGIN = 50f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float LINE_HEIGHT = 16f;

    private final String configuredFontPath;

    public ReportPdfRenderService(@Value("${report.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath;
    }

    public byte[] render(Long chatRoomId, String intentLabel, List<Message> messages) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);

            List<String> lines = new ArrayList<>();
            lines.add("증빙 리포트");
            lines.add("채팅방 ID: " + chatRoomId);
            lines.add("의도 태그: " + safe(intentLabel));
            lines.add("");

            if (messages.isEmpty()) {
                lines.add("메시지가 없습니다.");
            } else {
                for (Message message : messages) {
                    String sender = message.getSender() == null ? "알 수 없음" : safe(message.getSender().getName());
                    String ts = message.getCreatedAt() == null ? "-" : message.getCreatedAt().format(TS_FORMATTER);
                    String content = resolveContent(message);
                    lines.add(String.format("[%s] %s: %s", ts, sender, content));
                }
            }

            float maxWidth = PDRectangle.A4.getWidth() - (MARGIN * 2);
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) {
                wrapped.addAll(wrapText(line, font, BODY_FONT_SIZE, maxWidth));
            }

            int linesPerPage = (int) ((PDRectangle.A4.getHeight() - (MARGIN * 2)) / LINE_HEIGHT);
            int from = 0;
            while (from < wrapped.size()) {
                int to = Math.min(from + linesPerPage, wrapped.size());
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float y = page.getMediaBox().getHeight() - MARGIN;
                    for (int i = from; i < to; i++) {
                        float size = (i == 0) ? TITLE_FONT_SIZE : BODY_FONT_SIZE;
                        y = writeLine(stream, font, size, MARGIN, y, wrapped.get(i));
                    }
                }
                from = to;
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PDF 생성에 실패했습니다.", e);
        }
    }

    private String resolveContent(Message message) {
        if (hasText(message.getModifyContent())) {
            return message.getModifyContent().trim();
        }
        if (hasText(message.getContent())) {
            return message.getContent().trim();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private float writeLine(PDPageContentStream stream, PDFont font, float fontSize, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        try {
            stream.showText(text);
        } catch (IllegalArgumentException e) {
            stream.showText(text.replaceAll("[^\\x20-\\x7E]", "?"));
        }
        stream.endText();
        return y - LINE_HEIGHT;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String next = current.toString() + ch;
            float width = (font.getStringWidth(next) / 1000f) * fontSize;
            if (width > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder().append(ch);
            } else {
                current.append(ch);
            }
        }
        lines.add(current.toString());
        return lines;
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        for (Path candidate : fontCandidates()) {
            if (candidate != null && Files.exists(candidate)) {
                try (InputStream in = new FileInputStream(candidate.toFile())) {
                    return PDType0Font.load(document, in);
                } catch (Exception ignored) {
                    // try next
                }
            }
        }
        return PDType1Font.HELVETICA;
    }

    private List<Path> fontCandidates() {
        List<Path> candidates = new ArrayList<>();
        if (configuredFontPath != null && !configuredFontPath.trim().isEmpty()) {
            candidates.add(Paths.get(configuredFontPath.trim()));
        }
        candidates.add(Paths.get("C:/Windows/Fonts/malgun.ttf"));
        candidates.add(Paths.get("/usr/share/fonts/truetype/nanum/NanumGothic.ttf"));
        candidates.add(Paths.get("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"));
        return candidates;
    }
}


package com.softy.be.report.entity;

import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.common.entity.BaseEntity;
import com.softy.be.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pdf_file")
@Getter
@NoArgsConstructor
public class PdfFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    public static PdfFile create(ChatRoom chatRoom, User createdBy, String fileUrl, String fileName) {
        PdfFile pdfFile = new PdfFile();
        pdfFile.chatRoom = chatRoom;
        pdfFile.createdBy = createdBy;
        pdfFile.fileUrl = fileUrl;
        pdfFile.fileName = fileName;
        return pdfFile;
    }
}


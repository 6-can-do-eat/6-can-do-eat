package com.team6.backend.ai.domain.entity;

import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_ai_request_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ai_log_id", columnDefinition = "uuid")
    private UUID aiLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "username", nullable = false)
    private User user;

    @Column(name = "request_text", nullable = false, length = 100)
    private String requestText;

    @Column(name = "response_text", columnDefinition = "text")
    private String responseText;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private AiRequestType requestType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    public AiRequestLog(User user, String requestText, String responseText, AiRequestType requestType) {
        this.user = user;
        this.requestText = requestText;
        this.responseText = responseText;
        this.requestType = requestType;
    }
}

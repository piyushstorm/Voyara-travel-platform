package com.travelplatform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false)
    private String targetUserEmail;

    private String oldRole;

    private String newRole;

    private String details;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String action, String actorEmail, Long targetUserId, String targetUserEmail, String oldRole, String newRole) {
        this.action = action;
        this.actorEmail = actorEmail;
        this.targetUserId = targetUserId;
        this.targetUserEmail = targetUserEmail;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getTargetUserEmail() { return targetUserEmail; }
    public void setTargetUserEmail(String targetUserEmail) { this.targetUserEmail = targetUserEmail; }
    public String getOldRole() { return oldRole; }
    public void setOldRole(String oldRole) { this.oldRole = oldRole; }
    public String getNewRole() { return newRole; }
    public void setNewRole(String newRole) { this.newRole = newRole; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

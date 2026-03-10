package com.luopc.platform.web.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "form_submission")
public class FormSubmission {
    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的Schema ID */
    @Column(name = "schema_id", nullable = false)
    private String schemaId;

    /** 关联的Schema版本 */
    @Column(name = "schema_version", nullable = false)
    private String schemaVersion;

    /** 提交的表单数据（JSON字符串） */
    @Column(name = "submission_data", columnDefinition = "TEXT", nullable = false)
    private String submissionData;

    /** 提交人（可扩展：关联用户表） */
    @Column(name = "submitter")
    private String submitter;

    /** 提交时间 */
    @CreationTimestamp
    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    /** 验证结果（成功/失败） */
    @Column(name = "validate_result")
    private Boolean validateResult;

    /** 验证失败原因 */
    @Column(name = "validate_error", columnDefinition = "TEXT")
    private String validateError;
}

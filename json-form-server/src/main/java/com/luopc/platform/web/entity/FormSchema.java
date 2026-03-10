package com.luopc.platform.web.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "form_schema")
public class FormSchema {
    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Schema唯一标识（前端请求用） */
    @Column(name = "schema_id", unique = true, nullable = false)
    private String schemaId;

    /** Schema名称 */
    @Column(name = "name", nullable = false)
    private String name;

    /** 分类（如：用户表单、订单表单） */
    @Column(name = "category")
    private String category;

    /** JSON Schema内容 */
    @Column(name = "schema_content", columnDefinition = "TEXT", nullable = false)
    private String schemaContent;

    /** 版本号（如：1.0.0） */
    @Column(name = "version", nullable = false)
    private String version;

    /** 是否为当前生效版本 */
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

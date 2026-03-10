package com.luopc.platform.web.service;

import com.luopc.platform.web.entity.FormSchema;
import com.luopc.platform.web.loader.SchemaLoader;
import com.luopc.platform.web.repository.FormSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormSchemaService {
    private final FormSchemaRepository schemaRepository;
    private final SchemaLoader schemaLoader;

    /**
     * 获取表单配置（优先查数据库，无则查本地文件）
     */
    public String getFormConfig(String schemaId) {
        // 1. 查数据库当前生效版本
        Optional<FormSchema> dbSchema = schemaRepository.findBySchemaIdAndIsCurrentTrue(schemaId);
        if (dbSchema.isPresent()) {
            return dbSchema.get().getSchemaContent();
        }

        // 2. 查本地Schema文件
        String localSchema = schemaLoader.getLocalSchema(schemaId);
        if (localSchema != null) {
            // 本地Schema同步到数据库（默认版本1.0.0）
            saveSchema(FormSchema.builder()
                    .schemaId(schemaId)
                    .name(schemaId)
                    .category("本地默认")
                    .schemaContent(localSchema)
                    .version("1.0.0")
                    .isCurrent(true)
                    .build());
            return localSchema;
        }

        throw new RuntimeException("Schema不存在：" + schemaId);
    }

    /**
     * 创建新的表单Schema（版本控制：新版本设为current，旧版本设为false）
     */
    @Transactional
    public FormSchema createSchema(FormSchema formSchema) {
        String schemaId = formSchema.getSchemaId();
        // 1. 检查是否已存在，存在则更新旧版本为非current
        if (schemaRepository.existsBySchemaId(schemaId)) {
            List<FormSchema> oldSchemas = schemaRepository.findBySchemaIdOrderByVersionDesc(schemaId);
            oldSchemas.forEach(old -> {
                old.setIsCurrent(false);
                schemaRepository.save(old);
            });
        }

        // 2. 保存新版本
        formSchema.setIsCurrent(true);
        return schemaRepository.save(formSchema);
    }

    /**
     * 保存Schema（内部调用）
     */
    @Transactional
    public FormSchema saveSchema(FormSchema formSchema) {
        return schemaRepository.save(formSchema);
    }
}

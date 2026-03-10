package com.luopc.platform.web.loader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaLoader {

    /** 本地 Schema 文件路径 */
    @Value("${form.schema.path}")
    private String schemaPath;

    /** 缓存本地 Schema（schemaId -> schemaContent） */
    private Map<String, String> localSchemaCache = new HashMap<>();

    /** 初始化加载本地 Schema 文件到缓存 */
    @PostConstruct
    public void loadLocalSchemas() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(schemaPath + "/*.json");

            if (resources == null || resources.length == 0) {
                log.warn("本地 Schema 目录下无文件：{}", schemaPath);
                return;
            }

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) continue;
                // 文件名作为 schemaId（去掉.json 后缀）
                String schemaId = fileName.substring(0, fileName.lastIndexOf("."));
                String schemaContent = FileCopyUtils.copyToString(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                localSchemaCache.put(schemaId, schemaContent);
                log.info("加载本地 Schema 成功：{}", schemaId);
            }
        } catch (IOException e) {
            log.error("加载本地 Schema 失败", e);
            throw new RuntimeException("加载 Schema 文件异常");
        }
    }

    /** 根据 schemaId 获取本地 Schema 内容 */
    public String getLocalSchema(String schemaId) {
        return localSchemaCache.get(schemaId);
    }
}

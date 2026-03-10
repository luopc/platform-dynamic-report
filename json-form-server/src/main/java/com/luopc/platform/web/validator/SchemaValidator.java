package com.luopc.platform.web.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaValidator {
    private final ObjectMapper objectMapper;

    /**
     * 验证数据是否符合JSON Schema
     * @param schemaContent JSON Schema内容
     * @param data 待验证的JSON数据
     * @return 验证结果（空集合表示验证通过）
     */
    public Set<ValidationMessage> validate(String schemaContent, String data) {
        try {
            // 1. 解析Schema
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = objectMapper.readTree(schemaContent);
            JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);

            // 2. 解析待验证数据
            JsonNode dataNode = objectMapper.readTree(data);

            // 3. 执行验证
            return jsonSchema.validate(dataNode);
        } catch (Exception e) {
            log.error("Schema验证失败", e);
            throw new RuntimeException("Schema验证异常：" + e.getMessage());
        }
    }

    /**
     * 字段级实时验证（简化版：可扩展只验证指定字段）
     * @param schemaContent JSON Schema内容
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @return 验证结果
     */
    public Set<ValidationMessage> validateField(String schemaContent, String fieldName, Object fieldValue) {
        // 模拟字段级验证（实际可根据Schema提取字段规则单独验证）
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaContent);
            JsonNode fieldSchema = schemaNode.get("properties").get(fieldName);
            if (fieldSchema == null) {
                throw new RuntimeException("字段" + fieldName + "不存在于Schema中");
            }

            // 构建仅包含当前字段的临时数据
            String tempData = objectMapper.writeValueAsString(objectMapper.createObjectNode().put(fieldName, fieldValue.toString()));
            return validate(schemaContent, tempData);
        } catch (Exception e) {
            log.error("字段验证失败", e);
            throw new RuntimeException("字段验证异常：" + e.getMessage());
        }
    }
}

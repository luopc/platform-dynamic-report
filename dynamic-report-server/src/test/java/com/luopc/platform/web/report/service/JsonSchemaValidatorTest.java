package com.luopc.platform.web.report.service;

import static org.junit.jupiter.api.Assertions.*;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luopc.platform.web.report.model.FormSchema;
import com.luopc.platform.web.report.model.ValidationError;
import com.luopc.platform.web.report.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JsonSchemaValidator 单元测试
 */
@Slf4j
@DisplayName("JsonSchemaValidator 单元测试")
class JsonSchemaValidatorTest {

    private JsonSchemaValidator validator;

    @Mock
    private FormSchemaService schemaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();

        // 初始化默认的 Schema Mock
        setupDefaultSchemaMock();

        validator = new JsonSchemaValidator(objectMapper, schemaService);
    }

    /**
     * 设置默认的 Schema Mock 数据
     */
    private void setupDefaultSchemaMock() throws Exception {
        String userRegistrationSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "title": "用户注册表单",
              "required": ["username", "email", "password"],
              "properties": {
                "username": {
                  "type": "string",
                  "minLength": 3,
                  "maxLength": 20
                },
                "email": {
                  "type": "string",
                  "format": "email"
                },
                "password": {
                  "type": "string",
                  "minLength": 8
                },
                "age": {
                  "type": "integer",
                  "minimum": 18,
                  "maximum": 100
                },
                "profile": {
                  "type": "object",
                  "properties": {
                    "firstName": {
                      "type": "string"
                    },
                    "lastName": {
                      "type": "string"
                    }
                  }
                },
                "tags": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                }
              }
            }
            """;

        FormSchema schema = FormSchema.builder()
                .schemaId("test-schema")
                .name("测试表单")
                .schemaDefinition(userRegistrationSchema)
                .build();

        when(schemaService.getSchema(anyString())).thenReturn(schema);
        when(schemaService.getActiveSchema(anyString())).thenReturn(schema);
    }

    @Test
    @DisplayName("验证成功 - 所有必填字段都提供")
    void testValidate_Success() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("验证失败 - 缺少必填字段")
    void testValidate_MissingRequiredField() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        // 缺少 email 和 password

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());

        List<ValidationError> errors = result.getErrors();
        log.info("Errors: {}", errors);
        for (ValidationError error : errors){
            log.info("file = [{}], msg=[{}]", error.getField(), error.getMessage());
        }
//        assertTrue(errors.stream().anyMatch(e ->
//                e.getField().contains("email") || e.getField().contains("password")));
    }

    @Test
    @DisplayName("验证失败 - 字段长度不符合要求")
    void testValidate_FieldLengthValidation() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "jo"); // 太短，最少 3 位
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertFalse(result.isValid());
        List<ValidationError> errors = result.getErrors();
        log.info("Errors: {}", errors);
        for (ValidationError error : errors){
            log.info("file = [{}], msg=[{}]", error.getField(), error.getMessage());
        }
        assertTrue(errors.stream().anyMatch(e ->
                e.getField().contains("username") && e.getCode().contains("minLength")));
    }

    @Test
    @DisplayName("验证失败 - 邮箱格式不正确")
    void testValidate_InvalidEmailFormat() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "invalid-email"); // 无效的邮箱格式
        data.put("password", "Password123");

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertFalse(result.isValid());
        List<ValidationError> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e ->
                e.getField().contains("email")));
    }

    @Test
    @DisplayName("验证成功 - 可选字段为空时跳过验证")
    void testValidate_EmptyOptionalFieldIgnored() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.putNull("age"); // 可选字段为空

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证成功 - 可选字段有值时进行验证")
    void testValidate_OptionalFieldWithValue() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.put("age", 25); // 有效的可选字段值

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证失败 - 可选字段值超出范围")
    void testValidate_OptionalFieldOutOfRange() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.put("age", 150); // 超出最大值 100

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertFalse(result.isValid());
        List<ValidationError> errors = result.getErrors();

        log.info("Validation errors: {}", result.getErrors());
        assertTrue(errors.stream().anyMatch(e ->
                e.getField().contains("age")));
    }

    @Test
    @DisplayName("验证成功 - 嵌套对象字段验证")
    void testValidate_NestedObjectField() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        ObjectNode profile = objectMapper.createObjectNode();
        profile.put("firstName", "John");
        profile.put("lastName", "Doe");
        data.set("profile", profile);

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证成功 - 嵌套对象字段为空时跳过")
    void testValidate_EmptyNestedObject() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        ObjectNode profile = objectMapper.createObjectNode();
        // profile 为空对象
        data.set("profile", profile);

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证成功 - 数组字段验证")
    void testValidate_ArrayField() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        ArrayNode tags = objectMapper.createArrayNode();
        tags.add("java");
        tags.add("spring");
        data.set("tags", tags);

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证成功 - 空数组字段跳过")
    void testValidate_EmptyArray() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        ArrayNode tags = objectMapper.createArrayNode();
        // 空数组
        data.set("tags", tags);

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证失败 - Schema 不存在")
    void testValidate_SchemaNotFound() throws Exception {
        // Given
        when(schemaService.getSchema("non-existent-schema")).thenReturn(null);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");

        // When
        ValidationResult result = validator.validate("non-existent-schema", data);

        // Then
        assertFalse(result.isValid());
        log.info("Validation errors: {}", result.getErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("Schema not found")));
    }

    @Test
    @DisplayName("单个字段验证 - 成功")
    void testValidateField_Success() throws Exception {
        // Given
        JsonNode fieldValue = objectMapper.readTree("\"john_doe\"");

        // When
        ValidationResult result = validator.validateField("test-schema", "username", fieldValue);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("单个字段验证 - 失败（长度不足）")
    void testValidateField_Failure() throws Exception {
        // Given
        JsonNode fieldValue = objectMapper.readTree("\"jo\"");

        // When
        ValidationResult result = validator.validateField("test-schema", "username", fieldValue);

        // Then
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("单个字段验证 - 对象类型字段跳过验证")
    void testValidateField_ObjectTypeSkipped() throws Exception {
        // Given
        ObjectNode fieldValue = objectMapper.createObjectNode();
        fieldValue.put("firstName", "John");

        // When
        ValidationResult result = validator.validateField("test-schema", "profile", fieldValue);

        // Then
        assertTrue(result.isValid()); // 对象类型字段应该跳过验证
    }

    @Test
    @DisplayName("清除单个 Schema 缓存")
    void testClearCache() {
        // Given
        // 先触发一次验证以缓存 Schema
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        validator.validate("test-schema", data);

        // When
        validator.clearCache("test-schema");

        // Then
        // 验证没有异常，缓存已清除
        verify(schemaService, atLeastOnce()).getSchema("test-schema");
    }

    @Test
    @DisplayName("清除所有 Schema 缓存")
    void testClearAllCache() {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        validator.validate("test-schema", data);

        // When
        validator.clearAllCache();

        // Then
        verify(schemaService, atLeastOnce()).getSchema("test-schema");
    }

    @Test
    @DisplayName("验证 null 值字段")
    void testValidate_NullField() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.putNull("age");

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证空字符串字段")
    void testValidate_EmptyString() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.put("age", ""); // 空字符串

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        // 空字符串应该被视为无效值并跳过验证
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证整数类型字段")
    void testValidate_IntegerField() throws Exception {
        // Given
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");
        data.put("age", 25);

        // When
        ValidationResult result = validator.validate("test-schema", data);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("验证嵌套路径字段名提取")
    void testExtractFieldName() throws Exception {
        // 这个测试验证内部方法的处理逻辑
        ObjectNode data = objectMapper.createObjectNode();
        data.put("username", "john_doe");
        data.put("email", "john@example.com");
        data.put("password", "Password123");

        ObjectNode profile = objectMapper.createObjectNode();
        profile.put("firstName", "John");
        data.set("profile", profile);

        ValidationResult result = validator.validate("test-schema", data);

        assertTrue(result.isValid());
    }
}

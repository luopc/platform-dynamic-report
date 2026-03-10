package com.luopc.platform.web.controller;

import com.luopc.platform.web.entity.FormSchema;
import com.luopc.platform.web.entity.FormSubmission;
import com.luopc.platform.web.service.FormSchemaService;
import com.luopc.platform.web.service.FormSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/forms")
@RequiredArgsConstructor
public class FormController {
    private final FormSchemaService schemaService;
    private final FormSubmissionService submissionService;

    /**
     * 1. 获取表单配置（用于前端渲染）
     */
    @GetMapping("/{schemaId}/config")
    public ResponseEntity<String> getFormConfig(@PathVariable String schemaId) {
        try {
            String schemaContent = schemaService.getFormConfig(schemaId);
            return ResponseEntity.ok(schemaContent);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 2. 提交表单数据
     */
    @PostMapping("/{schemaId}/submit")
    public ResponseEntity<FormSubmission> submitForm(
            @PathVariable String schemaId,
            @RequestBody String submissionData,
            @RequestParam(required = false) String submitter) {
        try {
            FormSubmission submission = submissionService.submitForm(schemaId, submissionData, submitter);
            return ResponseEntity.ok(submission);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * 3. 实时字段验证
     */
    @PostMapping("/{schemaId}/validate-field")
    public ResponseEntity<String> validateField(
            @PathVariable String schemaId,
            @RequestBody Map<String, Object> fieldData) {
        try {
            String fieldName = fieldData.get("fieldName").toString();
            Object fieldValue = fieldData.get("fieldValue");
            String result = submissionService.validateField(schemaId, fieldName, fieldValue);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 4. 创建新的表单Schema
     */
    @PostMapping("/schemas")
    public ResponseEntity<FormSchema> createSchema(@RequestBody FormSchema formSchema) {
        try {
            FormSchema savedSchema = schemaService.createSchema(formSchema);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSchema);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}

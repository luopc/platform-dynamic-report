package com.luopc.platform.web.service;

import com.luopc.platform.web.entity.FormSchema;
import com.luopc.platform.web.entity.FormSubmission;
import com.luopc.platform.web.repository.FormSchemaRepository;
import com.luopc.platform.web.repository.FormSubmissionRepository;
import com.luopc.platform.web.validator.SchemaValidator;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormSubmissionService {
    private final FormSubmissionRepository submissionRepository;
    private final FormSchemaRepository schemaRepository;
    private final SchemaValidator schemaValidator;

    /**
     * 提交表单数据并验证
     */
    @Transactional
    public FormSubmission submitForm(String schemaId, String submissionData, String submitter) {
        // 1. 获取当前生效的Schema
        FormSchema schema = schemaRepository.findBySchemaIdAndIsCurrentTrue(schemaId)
                .orElseThrow(() -> new RuntimeException("Schema不存在：" + schemaId));

        // 2. 执行验证
        Set<ValidationMessage> validateResult = schemaValidator.validate(schema.getSchemaContent(), submissionData);
        boolean validateSuccess = validateResult.isEmpty();
        String validateError = validateSuccess ? null : validateResult.toString();

        // 3. 保存提交记录
        FormSubmission submission = FormSubmission.builder()
                .schemaId(schemaId)
                .schemaVersion(schema.getVersion())
                .submissionData(submissionData)
                .submitter(submitter)
                .validateResult(validateSuccess)
                .validateError(validateError)
                .build();

        return submissionRepository.save(submission);
    }

    /**
     * 实时字段验证
     */
    public String validateField(String schemaId, String fieldName, Object fieldValue) {
        // 1. 获取当前Schema
        FormSchema schema = schemaRepository.findBySchemaIdAndIsCurrentTrue(schemaId)
                .orElseThrow(() -> new RuntimeException("Schema不存在：" + schemaId));

        // 2. 字段验证
        Set<ValidationMessage> validateResult = schemaValidator.validateField(schema.getSchemaContent(), fieldName, fieldValue);
        if (validateResult.isEmpty()) {
            return "字段验证通过";
        } else {
            return "字段验证失败：" + validateResult.toString();
        }
    }
}

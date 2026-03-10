package com.luopc.platform.web.repository;

import com.luopc.platform.web.entity.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {
    /** 根据schemaId查询提交记录 */
    List<FormSubmission> findBySchemaIdOrderBySubmitTimeDesc(String schemaId);
}

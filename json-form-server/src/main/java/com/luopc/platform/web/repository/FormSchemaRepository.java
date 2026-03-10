package com.luopc.platform.web.repository;

import com.luopc.platform.web.entity.FormSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSchemaRepository extends JpaRepository<FormSchema, Long> {
    /** 根据schemaId查询当前生效版本 */
    Optional<FormSchema> findBySchemaIdAndIsCurrentTrue(String schemaId);

    /** 根据schemaId查询所有版本 */
    List<FormSchema> findBySchemaIdOrderByVersionDesc(String schemaId);

    /** 检查schemaId是否存在 */
    boolean existsBySchemaId(String schemaId);
}

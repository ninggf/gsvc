package com.apzda.cloud.gsvc.jpa.entity;

import com.apzda.cloud.gsvc.jpa.SnowflakeId;
import com.apzda.cloud.gsvc.model.Tenantable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class TestEntity extends AuditableEntity<String, String, Long> implements Tenantable<Long> {

    @Id
    @SnowflakeId(prefix = "1_")
    private String id;

    private Long tenantId;

}

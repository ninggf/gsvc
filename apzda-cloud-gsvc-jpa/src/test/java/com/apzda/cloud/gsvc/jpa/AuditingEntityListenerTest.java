package com.apzda.cloud.gsvc.jpa;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.jpa.entity.AuditableEntity;
import com.apzda.cloud.gsvc.jpa.entity.TestEntity;
import com.apzda.cloud.gsvc.jpa.repository.TestEntityRepository;
import com.apzda.cloud.gsvc.model.Tenantable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@DataJpaTest
@ContextConfiguration(classes = AuditingEntityListenerTest.class)
@EntityScan(basePackageClasses = AuditingEntityListenerTest.class)
@EnableJpaRepositories(basePackageClasses = AuditingEntityListenerTest.class)
class AuditingEntityListenerTest {

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Test
    void fillMetaData() {
        // given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity();
        try (val ms = Mockito.mockStatic(CurrentUserProvider.class); val ts = Mockito.mockStatic(TenantManager.class)) {
            ms.when(CurrentUserProvider::getCurrentUser).thenReturn(CurrentUser.builder().id("1").uid("1").build());
            ms.when(TenantManager::tenantId).thenReturn(2L);
            // when
            listener.fillMetaData(entity);

            // then
            assertThat(entity.getCreatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getTenantId()).isEqualTo(2L);
        }
    }

    @Test
    void fillMetaData2() {
        // given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity2();
        try (val ms = Mockito.mockStatic(CurrentUserProvider.class); val ts = Mockito.mockStatic(TenantManager.class)) {
            ms.when(CurrentUserProvider::getCurrentUser).thenReturn(CurrentUser.builder().id("1").uid("1").build());
            ms.when(TenantManager::tenantId).thenReturn("2L");
            // when
            listener.fillMetaData(entity);

            // then
            assertThat(entity.getCreatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getTenantId()).isEqualTo("2L");
        }
    }

    @Test
    void fillMetaData3() {
        // given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity2();
        // when
        listener.fillMetaData(entity);
        // then
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getTenantId()).isNull();

    }

    @Test
    void saveData() {
        // given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity();
        try (val ms = Mockito.mockStatic(CurrentUserProvider.class); val ts = Mockito.mockStatic(TenantManager.class)) {
            ms.when(CurrentUserProvider::getCurrentUser).thenReturn(CurrentUser.builder().id("1").uid("1").build());
            ms.when(TenantManager::tenantId).thenReturn(2L);
            // when
            val e = testEntityRepository.save(entity);

            // then
            assertThat(e.getId()).startsWith("1_");
            assertThat(e.getCreatedBy()).isEqualTo("1");
            assertThat(e.getUpdatedBy()).isEqualTo("1");
            assertThat(e.getUpdatedAt()).isNotNull();
            assertThat(e.getCreatedAt()).isNotNull();
            assertThat(e.getTenantId()).isEqualTo(2L);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class TestEntity2 extends AuditableEntity<Long, String, Long> implements Tenantable<String> {

        @Id
        private Long id;

        private String tenantId;

    }

}

package com.apzda.cloud.gsvc.domain;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.model.Tenantable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class AuditingEntityListenerTest {
    @Test
    void fillMetaData() {
        //given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity();
        try (val ms = Mockito.mockStatic(CurrentUserProvider.class); val ts = Mockito.mockStatic(TenantManager.class)) {
            ms.when(CurrentUserProvider::getCurrentUser).thenReturn(CurrentUser.builder().uid("1").build());
            ms.when(TenantManager::tenantId).thenReturn(2L);
            // when
            listener.fillMetaData(entity);

            //then
            assertThat(entity.getCreatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getTenantId()).isEqualTo(2L);
        }
    }

    @Test
    void fillMetaData2() {
        //given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity2();
        try (val ms = Mockito.mockStatic(CurrentUserProvider.class); val ts = Mockito.mockStatic(TenantManager.class)) {
            ms.when(CurrentUserProvider::getCurrentUser).thenReturn(CurrentUser.builder().uid("1").build());
            ms.when(TenantManager::tenantId).thenReturn("2L");
            // when
            listener.fillMetaData(entity);

            //then
            assertThat(entity.getCreatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedBy()).isEqualTo("1");
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getTenantId()).isEqualTo("2L");
        }
    }

    @Test
    void fillMetaData3() {
        //given
        val listener = new AuditingEntityListener();
        val entity = new TestEntity2();
        // when
        listener.fillMetaData(entity);
        //then
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getTenantId()).isNull();

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class TestEntity extends AuditableEntity<Long, String, Long> implements Tenantable<Long> {
        private Long id;
        private Long tenantId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class TestEntity2 extends AuditableEntity<Long, String, Long> implements Tenantable<String> {
        private Long id;
        private String tenantId;
    }
}

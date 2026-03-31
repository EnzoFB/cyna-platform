package com.cyna.modules.product.domain.model;

import com.cyna.modules.product.domain.event.CategoryCreated;
import com.cyna.modules.product.domain.event.CategoryDeleted;
import com.cyna.modules.product.domain.event.CategoryUpdated;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void should_create_category_with_active_status() {
        var category = Category.create("Antivirus", "Logiciels antivirus");

        assertThat(category.getId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Antivirus");
        assertThat(category.getDescription()).isEqualTo("Logiciels antivirus");
        assertThat(category.isActive()).isTrue();
        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_raise_category_created_event() {
        var category = Category.create("Firewall", "Solutions pare-feu");

        assertThat(category.getDomainEvents()).hasSize(1);
        assertThat(category.getDomainEvents().getFirst()).isInstanceOf(CategoryCreated.class);

        var event = (CategoryCreated) category.getDomainEvents().getFirst();
        assertThat(event.categoryId()).isEqualTo(category.getId());
        assertThat(event.name()).isEqualTo("Firewall");
    }

    @Test
    void should_reject_null_name() {
        assertThatThrownBy(() -> Category.create(null, "description"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_blank_name() {
        assertThatThrownBy(() -> Category.create("", "description"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_null_description() {
        assertThatThrownBy(() -> Category.create("Antivirus", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_update_name_and_description() {
        var category = Category.create("Old Name", "Old desc");
        var result = category.update("New Name", "New desc");

        assertThat(result.isSuccess()).isTrue();

        var updated = result.getValue();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getId()).isEqualTo(category.getId());
        assertThat(updated.getDomainEvents()).hasSize(1);
        assertThat(updated.getDomainEvents().getFirst()).isInstanceOf(CategoryUpdated.class);

        // original not mutated
        assertThat(category.getName()).isEqualTo("Old Name");
    }

    @Test
    void should_deactivate_category() {
        var category = Category.create("Antivirus", "desc");
        var result = category.deactivate();

        assertThat(result.isSuccess()).isTrue();

        var deactivated = result.getValue();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.getDomainEvents()).hasSize(1);
        assertThat(deactivated.getDomainEvents().getFirst()).isInstanceOf(CategoryDeleted.class);
    }

    @Test
    void should_fail_deactivate_when_already_inactive() {
        var category = Category.reconstitute(
                UUID.randomUUID(), "Antivirus", "desc", null, false,
                Instant.now(), Instant.now()
        );

        var result = category.deactivate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Category is already inactive");
    }

    @Test
    void should_activate_inactive_category() {
        var category = Category.reconstitute(
                UUID.randomUUID(), "Antivirus", "desc", null, false,
                Instant.now(), Instant.now()
        );

        var result = category.activate();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().isActive()).isTrue();
    }

    @Test
    void should_fail_activate_when_already_active() {
        var category = Category.create("Antivirus", "desc");

        var result = category.activate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Category is already active");
    }

    @Test
    void should_reconstitute_without_events() {
        var category = Category.reconstitute(
                UUID.randomUUID(), "Antivirus", "desc", null, true,
                Instant.now(), Instant.now()
        );

        assertThat(category.getName()).isEqualTo("Antivirus");
        assertThat(category.getDomainEvents()).isEmpty();
    }
}

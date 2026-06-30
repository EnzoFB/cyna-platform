package com.cyna.modules.product.domain.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSortTest {

    @Test
    void should_parse_valid_sort() {
        var result = ProductSort.parse("name,desc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().field()).isEqualTo(ProductSortField.NAME);
        assertThat(result.getValue().direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void should_accept_price_alias() {
        var result = ProductSort.parse("price,asc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().field()).isEqualTo(ProductSortField.PRICE);
        assertThat(result.getValue().direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_reject_unknown_field() {
        var result = ProductSort.parse("drop,asc");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void should_reject_unknown_direction() {
        var result = ProductSort.parse("name,down");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void should_default_when_blank() {
        var result = ProductSort.parse("  ");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().field()).isEqualTo(ProductSortField.PRIORITY);
        assertThat(result.getValue().direction()).isEqualTo(SortDirection.DESC);
    }
}

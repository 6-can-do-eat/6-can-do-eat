package com.team6.backend.store.domain.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.backend.store.domain.entity.Store;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.team6.backend.store.domain.entity.QStore.store;

public class StoreRepositoryCustomImpl implements StoreRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public StoreRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Store> searchStores(String keyword, UUID categoryId, UUID areaId, Pageable pageable) {
        List<Store> content = queryFactory
                .selectFrom(store)
                .leftJoin(store.category).fetchJoin()
                .leftJoin(store.area).fetchJoin()
                .where(
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        areaEq(areaId),
                        store.isHidden.isFalse()
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(store.count())
                .from(store)
                .where(
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        areaEq(areaId),
                        store.isHidden.isFalse()
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Store> searchStoresByOwnerId(UUID ownerId, String keyword, UUID categoryId, UUID areaId, Pageable pageable) {
        List<Store> content = queryFactory
                .selectFrom(store)
                .leftJoin(store.category).fetchJoin()
                .leftJoin(store.area).fetchJoin()
                .where(
                        ownerEq(ownerId),
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        areaEq(areaId)
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(store.count())
                .from(store)
                .where(
                        ownerEq(ownerId),
                        keywordContains(keyword),
                        categoryEq(categoryId),
                        areaEq(areaId)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
                PathBuilder<Store> pathBuilder = new PathBuilder<>(Store.class, "store");
                orders.add(new OrderSpecifier(direction, pathBuilder.get(order.getProperty())));
            }
        }
        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? store.name.containsIgnoreCase(keyword) : null;
    }
    private BooleanExpression categoryEq(UUID categoryId) {
        return categoryId != null ? store.category.categoryId.eq(categoryId) : null;
    }
    private BooleanExpression areaEq(UUID areaId) {
        return areaId != null ? store.area.areaId.eq(areaId) : null;
    }
    private BooleanExpression ownerEq(UUID ownerId) {
        return ownerId != null ? store.owner.id.eq(ownerId) : null;
    }
}

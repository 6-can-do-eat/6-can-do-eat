package com.team6.backend.menu.domain.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team6.backend.menu.domain.entity.Menu;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.team6.backend.menu.domain.entity.QMenu.menu;
import static com.team6.backend.store.domain.entity.QStore.store;

public class MenuRepositoryCustomImpl implements MenuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MenuRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Menu> searchMenus(UUID storeId, String keyword, Pageable pageable) {
        return getPagedResult(
                storeEq(storeId),
                keywordContains(keyword),
                menu.isHidden.isFalse(),
                pageable
        );
    }

    @Override
    public Page<Menu> findByStore_StoreId(UUID storeId, Pageable pageable) {
        return getPagedResult(
                storeEq(storeId),
                null,
                null,
                pageable
        );
    }

    @Override
    public Page<Menu> findByStore_StoreIdAndNameContainingIgnoreCase(UUID storeId, String name, Pageable pageable) {
        return getPagedResult(
                storeEq(storeId),
                keywordContains(name),
                null,
                pageable
        );
    }

    @Override
    public Optional<Menu> findByMenuIdAndStore_StoreId(UUID menuId, UUID storeId) {
        Menu fetchOne = queryFactory
                .selectFrom(menu)
                .join(menu.store, store)
                .where(
                        menu.menuId.eq(menuId),
                        store.storeId.eq(storeId)
                )
                .fetchOne();

        return Optional.ofNullable(fetchOne);
    }

    private Page<Menu> getPagedResult(BooleanExpression cond1, BooleanExpression cond2, BooleanExpression cond3, Pageable pageable) {
        List<Menu> content = queryFactory
                .selectFrom(menu)
                .innerJoin(menu.store, store).fetchJoin()
                .where(cond1, cond2, cond3)
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(menu.count())
                .from(menu)
                .innerJoin(menu.store, store)
                .where(cond1, cond2, cond3);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (pageable.getSort() != null && pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
                PathBuilder<Menu> pathBuilder = new PathBuilder<>(Menu.class, "menu");
                orders.add(new OrderSpecifier(direction, pathBuilder.get(order.getProperty())));
            }
        }
        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanExpression storeEq(UUID storeId) {
        return storeId != null ? menu.store.storeId.eq(storeId) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? menu.name.containsIgnoreCase(keyword) : null;
    }
}

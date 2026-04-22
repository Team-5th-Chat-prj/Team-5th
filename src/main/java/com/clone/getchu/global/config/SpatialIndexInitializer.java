package com.clone.getchu.global.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpatialIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Spatial Index는 JPA ddl-auto가 생성하지 못하므로 애플리케이션 시작 시 직접 생성.
     * - 이미 존재하는 경우(재시작) : DataAccessException catch → DEBUG 로그로 흡수
     * - H2 테스트 환경 미지원    : 동일하게 catch → 테스트에 영향 없음
     */
    @PostConstruct
    public void createSpatialIndexes() {
        createSpatialIndex("idx_member_location",  "members", "location");
        createSpatialIndex("idx_product_location", "PRODUCT", "location");
    }

    private void createSpatialIndex(String indexName, String tableName, String columnName) {
        try {
            jdbcTemplate.execute(
                    "CREATE SPATIAL INDEX " + indexName +
                    " ON " + tableName + "(" + columnName + ")"
            );
            log.info("Spatial index created: {}.{}", tableName, indexName);
        } catch (DataAccessException e) {
            log.debug("Spatial index skipped [{}.{}]: {}",
                    tableName, indexName, e.getMostSpecificCause().getMessage());
        }
    }
}

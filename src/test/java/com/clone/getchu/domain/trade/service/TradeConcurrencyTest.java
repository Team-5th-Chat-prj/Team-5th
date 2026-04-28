package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.category.repository.CategoryRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.clone.getchu.support.EmbeddedRedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
class TradeConcurrencyTest {

    @Autowired
    private TradeFacade tradeFacade;

    @Autowired
    private TradeService tradeService; // 비관적 락 단독 검증용

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        Category category = Category.builder()
                .name("전자기기")
                .build();
        categoryRepository.save(category);

        Member seller = Member.builder()
                .email("seller@test.com")
                .nickname("판매자")
                .password("test1234!")
                .build();
        memberRepository.save(seller);

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title("아이폰 17")
                .description("미개봉 새상품")
                .price(10000)
                .status(ProductEnum.SALE)
                .build();
        productRepository.save(product);
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        tradeRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    // -- 설계서 §3-B: 분산락 + 비관적 락 이중 방어 검증 --
    @Test
    @DisplayName("동시에 100명이 예약 요청할 경우 단 1명만 성공해야 한다 (분산락 + 비관적 락 이중 방어)")
    void reserveProduct_concurrency_withDistributedLock() throws InterruptedException {
        int threadCount = 100;
        List<Long> buyerIds = new ArrayList<>();

        // 100명의 구매자 생성
        for (int i = 0; i < threadCount; i++) {
            Member buyer = Member.builder()
                    .email("buyer" + i + "@test.com")
                    .nickname("구매자" + i)
                    .password("test1234!")
                    .build();
            memberRepository.save(buyer);
            buyerIds.add(buyer.getId());
        }

        // ExecutorService: 스레드 풀을 관리하며 비동기적으로 작업을 실행하는 인터페이스
        // fixedThreadPool(32): 최대 32개의 액티브 스레드를 유지하며 작업을 처리
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // CountDownLatch: 모든 작업이 종료될 때까지 메인 스레드를 블로킹
        CountDownLatch latch = new CountDownLatch(threadCount);
        // AtomicInteger: 멀티스레드 환경에서 synchronized 없이도 스레드 안전한 정수형 변수
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Long buyerId = buyerIds.get(i);
            executorService.submit(() -> {
                try {
                    // TradeFacade 호출 -> 분산락(1차) + 비관적 락(2차) 모두 동작
                    tradeFacade.reserveProduct(productId, buyerId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).as("시간 내에 모든 스레드가 작업을 완료해야 합니다.").isTrue();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        long tradeCount = tradeRepository.count();
        assertThat(tradeCount).isEqualTo(1);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductEnum.RESERVED);
    }

    // -- 설계서 §3-A & §3-C: 비관적 락 단독 검증 (Redis 장애 시 fallback 시뮬레이션) --
    @Test
    @DisplayName("비관적 락 단독 — 동시에 5명이 예약 시도 시 1명만 성공해야 한다 (Redis 장애 fallback 시나리오)")
    void reserveProduct_pessimisticLockOnly_redisFailureFallback() throws InterruptedException {
        // 설계서 §5 & §3-C:
        // TradeFacade를 우회하고 TradeService를 직접 호출하여
        // Redis(분산락) 없이 비관적 락(SELECT FOR UPDATE)만으로 정합성이 보장되는지 검증
        int threadCount = 5;
        List<Long> buyerIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Member buyer = Member.builder()
                    .email("pessimistic_buyer" + i + "@test.com")
                    .nickname("비관적락구매자" + i)
                    .password("test1234!")
                    .build();
            memberRepository.save(buyer);
            buyerIds.add(buyer.getId());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Long buyerId = buyerIds.get(i);
            executorService.submit(() -> {
                try {
                    // TradeFacade 대신 TradeService 직접 호출
                    // -> Lettuce 분산락 없이 비관적 락(SELECT FOR UPDATE)만 동작
                    tradeService.reserveProduct(productId, buyerId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).as("시간 내에 모든 스레드가 작업을 완료해야 합니다.").isTrue();

        // Redis 없이도 비관적 락만으로 1명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductEnum.RESERVED);

        long tradeCount = tradeRepository.count();
        assertThat(tradeCount).isEqualTo(1);
    }
}

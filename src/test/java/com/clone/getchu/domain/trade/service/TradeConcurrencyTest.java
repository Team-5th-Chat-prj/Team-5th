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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeConcurrencyTest {

    @Autowired
    private TradeFacade tradeFacade;
    //private TradeService tradeService;

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

        // 카테고리 생성
        Category category = Category.builder()
                .name("전자기기")
                .build();
        categoryRepository.save(category);

        // 판매자 생성
        Member seller = Member.builder()
                .email("seller@test.com")
                .nickname("판매자")
                .password("test1234!")
                .build();
        memberRepository.save(seller);

        // 상품 생성
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

    @Test
    @DisplayName("동시에 100명이 예약 요청할 경우 단 1명만 성공해야 한다 (분산락 적용)")
    void reserveProduct_concurrency() throws InterruptedException {
        int threadCount = 100;
        List<Long> buyerIds = new ArrayList<>();

        //1. 100명의 구매자 생성
        for (int i = 0; i < threadCount; i++) {
            Member buyer = Member.builder()
                    .email("buyer" + i + "@test.com")
                    .nickname("구매자" + i)
                    .password("test1234!")
                    .build();
            memberRepository.save(buyer);
            buyerIds.add(buyer.getId());
        }

        //2. 동시성 제어 객체 생성
        //ExecutorService: 스레드 풀을 관리하며 비동기적으로 작업을 실행하는 인터페이스
        // fixedThreadPool(32): 최대 32개의 액티브 스레드를 유지하며 작업을 처리
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // CountDownLatch: 다른 스레드에서 수행 중인 작업이 완료될 때까지 대기할 수 있게 하는 동기화 장치
        // 초기 카운트를 100으로 설정하여 모든 작업이 종료될 때까지 메인 스레드를 블로킹(Blocking)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // AtomicInteger: CAS(Compare And Swap) 알고리즘을 사용하여 멀티스레드 환경에서 가시성과 원자성을 보장하는 정수형 변수
        // 일반적인 int 변수와 달리 synchronized 없이도 스레드 안전(Thread-safe)한 연산이 가능
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 100개의 스레드에서 동시에 예약 요청
        for (int i = 0; i < threadCount; i++) {
            Long buyerId = buyerIds.get(i);
            //스레드 풀의 가용 스레드에 할당하여 실행
            executorService.submit(() -> {
                try {
                    tradeFacade.reserveProduct(productId, buyerId);
                    //tradeService.reserveProduct(productId, buyerId);
                    successCount.incrementAndGet(); // 원자적 증가 연산
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 예외 발생 시 원자적 증가 연산
                } finally {
                    // 각 작업 완료 시 카운트를 감소시켜 Latch의 상태를 갱신
                    latch.countDown();
                }
            });
        }

        // await(): Latch의 카운트가 0이 될 때까지 현재 메인 스레드의 실행을 일시 중단 (타임아웃 10초)
        boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(completed).as("시간 내에 모든 스레드가 작업을 완료해야 합니다.").isTrue();

        // 단 1개의 요청만 성공하고 99개는 예외(이미 예약됨) 발생
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // 실제 DB에 1개의 거래(Trade)만 생성되었는지, 상품 상태가 변경되었는지 확인
        long tradeCount = tradeRepository.count();
        assertThat(tradeCount).isEqualTo(1);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductEnum.RESERVED);
    }
}

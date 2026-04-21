package com.clone.getchu.domain.like.service;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.category.repository.CategoryRepository;
import com.clone.getchu.domain.like.repository.LikeRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest { 

    @Autowired
    private LikeFacade likeFacade;
    // private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        likeRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        // 카테고리 생성
        Category category = Category.builder()
                .name("의류")
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
                .title("테스트 상품")
                .description("테스트 설명")
                .price(10000)
                .status(ProductEnum.SALE)
                .build();
        productRepository.save(product);
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        likeRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("서로 다른 100명이 동시에 찜하기 요청 시 100개 모두 정상 반영되어야 한다 (분산락 적용)")
    void createLike_differentUsers_concurrency() throws InterruptedException {
        int threadCount = 100;
        List<Long> buyerIds = new ArrayList<>();

        // 1. 100명의 구매자 생성
        for (int i = 0; i < threadCount; i++) {
            Member buyer = Member.builder()
                    .email("buyer" + i + "@test.com")
                    .nickname("구매자" + i)
                    .password("test1234!")
                    .build();
            memberRepository.save(buyer);
            buyerIds.add(buyer.getId());
        }

        // 2. 동시성 제어 객체 생성
        ExecutorService executorService = Executors.newFixedThreadPool(128);
        // CyclicBarrier: 모든 스레드가 준비될 때까지 대기 후 동시에 실행 (await 호출 후 모든 스레드가 모이면 통과)
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 100개의 스레드에서 동시에 찜하기 요청
        for (int i = 0; i < threadCount; i++) {
            Long buyerId = buyerIds.get(i);
            executorService.submit(() -> {
                try {
                    barrier.await(); // 100개의 스레드가 모일 때까지 대기하다가 동시에 시작
                    
                    likeFacade.createLike(productId, buyerId);
                    // likeService.createLike(productId, buyerId); // 락 제외 테스트 시 활성화
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("Error creating like: " + e.getMessage());
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        //latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        latch.await();

        // 모두 성공해야 함
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(0);

        // DB 검증 (100개의 좋아요 생성)
        long likeCount = likeRepository.count();
        assertThat(likeCount).isEqualTo(100);

        // 상품의 좋아요 카운트가 100이어야 함
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getLikeCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("동일한 사용자가 동시에 100번 찜하기 요청 시 단 1번만 성공해야 한다 (분산락 적용)")
    void createLike_sameUser_concurrency() throws InterruptedException {
        int threadCount = 100;

        // 1명의 구매자 생성
        Member buyer = Member.builder()
                .email("buyer_same@test.com")
                .nickname("구매자_동일")
                .password("test1234!")
                .build();
        memberRepository.save(buyer);
        Long buyerId = buyer.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(128);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 100번을 동시에 찜하기 요청 (동일 사용자)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    barrier.await(); // 100개의 스레드가 모일 때까지 대기하다가 동시에 시작
                    
                    likeFacade.createLike(productId, buyerId);
                    // likeService.createLike(productId, buyerId); // 락 제외 테스트 시 활성화
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        //latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        latch.await();

        // 단 1건만 성공하고 99건은 실패해야 함 (이미 찜한 상품 예외)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // DB 검증 (1개의 좋아요 기록)
        long likeCount = likeRepository.count();
        assertThat(likeCount).isEqualTo(1);

        // 상품의 좋아요 카운트가 1이어야 함
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getLikeCount()).isEqualTo(1);
    }
}

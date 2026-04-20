package com.clone.getchu.domain.like.repository;

import com.clone.getchu.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
}

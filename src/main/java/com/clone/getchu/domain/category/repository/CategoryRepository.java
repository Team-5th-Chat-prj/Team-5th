package com.clone.getchu.domain.category.repository;

import com.clone.getchu.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

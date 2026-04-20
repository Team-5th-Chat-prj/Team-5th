package com.clone.getchu.domain.category.repository;

import com.clone.getchu.domain.category.dto.CategoryResponse;
import com.clone.getchu.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT new com.clone.getchu.domain.category.dto.CategoryResponse(c.id, c.name) " +
            "FROM Category c")
    List<CategoryResponse> findAllProjections();
}

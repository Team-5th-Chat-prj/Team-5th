package com.clone.getchu.domain.category.service;

import com.clone.getchu.domain.category.dto.CategoryResponse;
import com.clone.getchu.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    @Cacheable(cacheNames = "categories")
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllProjections();
    }
}
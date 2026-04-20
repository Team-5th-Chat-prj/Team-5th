package com.clone.getchu.domain.category.controller;

import com.clone.getchu.domain.category.dto.CategoryResponse;
import com.clone.getchu.domain.category.service.CategoryService;
import com.clone.getchu.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return  ResponseEntity.ok(ApiResponse.success(categoryService.getCategories()));
    }
}

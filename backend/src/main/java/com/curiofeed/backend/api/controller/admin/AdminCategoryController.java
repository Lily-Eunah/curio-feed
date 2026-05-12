package com.curiofeed.backend.api.controller.admin;

import com.curiofeed.backend.api.dto.admin.CategoryResponse;
import com.curiofeed.backend.api.dto.admin.CategorySaveRequest;
import com.curiofeed.backend.domain.entity.Category;
import com.curiofeed.backend.domain.repository.ArticleRepository;
import com.curiofeed.backend.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories(@RequestParam(name = "all", defaultValue = "false") boolean all) {
        List<Category> categories = all
                ? categoryRepository.findAllByOrderBySortOrderAsc()
                : categoryRepository.findByActiveTrueOrderBySortOrderAsc();

        List<CategoryResponse> responses = categories.stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getDisplayName(), // Map displayName to name
                        category.getName(),        // Map name to slug
                        category.getSortOrder(),
                        category.isActive()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategorySaveRequest request) {
        Category category = new Category(
                request.name(),
                request.displayName(),
                request.sortOrder(),
                request.active()
        );
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateCategory(@PathVariable("id") UUID id, @RequestBody CategorySaveRequest request) {
        return categoryRepository.findById(id).map(category -> {
            category.update(
                    request.name(),
                    request.displayName(),
                    request.sortOrder(),
                    request.active()
            );
            return ResponseEntity.ok(toResponse(category));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
        return categoryRepository.findById(id).map(category -> {
            if (articleRepository.existsByCategoryId(id)) {
                // If articles exist, do soft delete
                category.deactivate();
                return ResponseEntity.ok(Map.of("message", "Category deactivated because it has associated articles", "active", false));
            } else {
                // Safe to delete
                categoryRepository.delete(category);
                return ResponseEntity.noContent().build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getDisplayName(),
                category.getName(),
                category.getSortOrder(),
                category.isActive()
        );
    }
}

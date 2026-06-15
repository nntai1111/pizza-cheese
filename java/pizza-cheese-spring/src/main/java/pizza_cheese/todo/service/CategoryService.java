package pizza_cheese.todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pizza_cheese.todo.dao.CategoryDao;
import pizza_cheese.todo.domain.Category;
import pizza_cheese.todo.dto.request.CreateCategoryRequest;
import pizza_cheese.todo.dto.request.UpdateCategoryRequest;
import pizza_cheese.todo.dto.response.CategoryResponse;
import pizza_cheese.todo.exception.CategoryNotFoundException;
import pizza_cheese.todo.exception.SlugAlreadyExistsException;
import pizza_cheese.todo.util.SlugUtil;

@Service
public class CategoryService {

    private final CategoryDao categoryDao;
    private final CloudinaryService cloudinaryService;

    public CategoryService(CategoryDao categoryDao, CloudinaryService cloudinaryService) {
        this.categoryDao = categoryDao;
        this.cloudinaryService = cloudinaryService;
    }

    public List<CategoryResponse> findAll(boolean activeOnly) {
        return categoryDao.findAll(activeOnly).stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse findById(UUID id) {
        return categoryDao.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục"));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        return create(request, null);
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, MultipartFile imageFile) {
        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), null);
        if (categoryDao.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setImageUrl(resolveImageUrl(request.getImageUrl(), imageFile));
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setActive(request.getIsActive() == null || request.getIsActive());

        return CategoryResponse.from(categoryDao.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request, MultipartFile imageFile) {
        Category category = categoryDao.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục"));

        if (request.getName() != null) {
            category.setName(request.getName().trim());
        }

        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), category.getSlug());
        if (slug != null && categoryDao.existsBySlugExcludingId(slug, id)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }
        if (slug != null) {
            category.setSlug(slug);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImageUrl(cloudinaryService.uploadCategoryImage(imageFile));
        } else if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            category.setActive(request.getIsActive());
        }

        return CategoryResponse.from(categoryDao.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        if (categoryDao.findById(id).isEmpty()) {
            throw new CategoryNotFoundException("Không tìm thấy danh mục");
        }
        categoryDao.deactivate(id);
    }

    public void requireActiveCategory(UUID categoryId) {
        Category category = categoryDao.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục"));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Danh mục không còn hoạt động");
        }
    }

    private String resolveImageUrl(String imageUrl, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return cloudinaryService.uploadCategoryImage(imageFile);
        }
        return imageUrl;
    }
}

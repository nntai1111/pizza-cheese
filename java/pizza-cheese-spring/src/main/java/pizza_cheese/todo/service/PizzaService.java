package pizza_cheese.todo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pizza_cheese.todo.dao.PizzaDao;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaSize;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.dto.request.CreatePizzaRequest;
import pizza_cheese.todo.dto.request.PizzaImageRequest;
import pizza_cheese.todo.dto.request.PizzaVariantRequest;
import pizza_cheese.todo.dto.request.UpdatePizzaRequest;
import pizza_cheese.todo.dto.response.PizzaResponse;
import pizza_cheese.todo.exception.PizzaNotFoundException;
import pizza_cheese.todo.exception.SlugAlreadyExistsException;
import pizza_cheese.todo.util.SlugUtil;

@Service
public class PizzaService {

    private final PizzaDao pizzaDao;
    private final CategoryService categoryService;
    private final ToppingService toppingService;
    private final CloudinaryService cloudinaryService;

    public PizzaService(
            PizzaDao pizzaDao,
            CategoryService categoryService,
            ToppingService toppingService,
            CloudinaryService cloudinaryService) {
        this.pizzaDao = pizzaDao;
        this.categoryService = categoryService;
        this.toppingService = toppingService;
        this.cloudinaryService = cloudinaryService;
    }

    public List<PizzaResponse> findAll(boolean activeOnly, UUID categoryId) {
        return pizzaDao.findAll(activeOnly, categoryId).stream().map(PizzaResponse::from).toList();
    }

    public PizzaResponse findById(UUID id) {
        return pizzaDao.findById(id)
                .map(PizzaResponse::from)
                .orElseThrow(() -> new PizzaNotFoundException("Không tìm thấy pizza"));
    }

    @Transactional
    public PizzaResponse create(CreatePizzaRequest request) {
        return create(request, List.of());
    }

    @Transactional
    public PizzaResponse create(CreatePizzaRequest request, List<MultipartFile> imageFiles) {
        categoryService.requireActiveCategory(request.getCategoryId());
        validateUniqueVariantSizes(request.getVariants());
        toppingService.validateToppingIds(request.getToppingIds());

        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), null);
        if (pizzaDao.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }

        Pizza pizza = new Pizza();
        pizza.setCategoryId(request.getCategoryId());
        pizza.setName(request.getName().trim());
        pizza.setSlug(slug);
        pizza.setDescription(request.getDescription());
        pizza.setBasePrice(request.getBasePrice());
        pizza.setActive(request.getIsActive() == null || request.getIsActive());
        pizza.setVariants(toVariants(request.getVariants()));
        pizza.setToppingIds(request.getToppingIds() != null ? request.getToppingIds() : List.of());
        pizza.setImages(buildImages(request.getImages(), imageFiles));

        Pizza saved = pizzaDao.save(pizza);
        return PizzaResponse.from(pizzaDao.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public PizzaResponse update(UUID id, UpdatePizzaRequest request) {
        return update(id, request, List.of());
    }

    @Transactional
    public PizzaResponse update(UUID id, UpdatePizzaRequest request, List<MultipartFile> imageFiles) {
        Pizza pizza = pizzaDao.findById(id)
                .orElseThrow(() -> new PizzaNotFoundException("Không tìm thấy pizza"));

        if (request.getCategoryId() != null) {
            categoryService.requireActiveCategory(request.getCategoryId());
            pizza.setCategoryId(request.getCategoryId());
        }

        if (request.getName() != null) {
            pizza.setName(request.getName().trim());
        }

        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), pizza.getSlug());
        if (slug != null && pizzaDao.existsBySlugExcludingId(slug, id)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }
        if (slug != null) {
            pizza.setSlug(slug);
        }

        if (request.getDescription() != null) {
            pizza.setDescription(request.getDescription());
        }
        if (request.getBasePrice() != null) {
            pizza.setBasePrice(request.getBasePrice());
        }
        if (request.getIsActive() != null) {
            pizza.setActive(request.getIsActive());
        }

        if (request.getVariants() != null) {
            validateUniqueVariantSizes(request.getVariants());
            pizza.setVariants(toVariants(request.getVariants()));
        }

        if (request.getToppingIds() != null) {
            toppingService.validateToppingIds(request.getToppingIds());
            pizza.setToppingIds(request.getToppingIds());
        } else {
            pizza.setToppingIds(pizza.getToppings().stream().map(t -> t.getId()).toList());
        }

        if (request.getImages() != null || !imageFiles.isEmpty()) {
            pizza.setImages(buildImages(request.getImages(), imageFiles));
        } else {
            pizza.setImages(new ArrayList<>(pizza.getImages()));
        }

        Pizza saved = pizzaDao.save(pizza);
        return PizzaResponse.from(pizzaDao.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(UUID id) {
        if (pizzaDao.findById(id).isEmpty()) {
            throw new PizzaNotFoundException("Không tìm thấy pizza");
        }
        pizzaDao.deactivate(id);
    }

    private void validateUniqueVariantSizes(List<PizzaVariantRequest> variants) {
        Set<PizzaSize> sizes = new HashSet<>();
        for (PizzaVariantRequest variant : variants) {
            if (!sizes.add(variant.getSize())) {
                throw new IllegalArgumentException("Không được trùng size pizza: " + variant.getSize());
            }
        }
    }

    private List<PizzaVariant> toVariants(List<PizzaVariantRequest> requests) {
        return requests.stream().map(request -> {
            PizzaVariant variant = new PizzaVariant();
            variant.setSize(request.getSize());
            variant.setPrice(request.getPrice());
            return variant;
        }).toList();
    }

    private List<PizzaImage> buildImages(List<PizzaImageRequest> imageRequests, List<MultipartFile> imageFiles) {
        List<PizzaImage> images = new ArrayList<>();

        if (imageRequests != null) {
            int order = 0;
            for (PizzaImageRequest req : imageRequests) {
                PizzaImage image = new PizzaImage();
                image.setImageUrl(req.getImageUrl());
                image.setMain(Boolean.TRUE.equals(req.getIsMain()));
                image.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : order++);
                images.add(image);
            }
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            int startOrder = images.size();
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                PizzaImage image = new PizzaImage();
                image.setImageUrl(cloudinaryService.uploadPizzaImage(file));
                image.setMain(images.isEmpty() && i == 0);
                image.setSortOrder(startOrder + i);
                images.add(image);
            }
        }

        ensureSingleMainImage(images);
        return images;
    }

    private void ensureSingleMainImage(List<PizzaImage> images) {
        if (images.isEmpty()) {
            return;
        }
        boolean hasMain = images.stream().anyMatch(PizzaImage::isMain);
        if (!hasMain) {
            images.get(0).setMain(true);
        }
    }
}

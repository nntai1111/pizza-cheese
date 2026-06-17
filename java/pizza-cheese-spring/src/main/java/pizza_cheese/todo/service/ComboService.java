package pizza_cheese.todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pizza_cheese.todo.dao.ComboDao;
import pizza_cheese.todo.dao.PizzaDao;
import pizza_cheese.todo.domain.Combo;
import pizza_cheese.todo.domain.ComboItem;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.dto.request.ComboItemRequest;
import pizza_cheese.todo.dto.request.CreateComboRequest;
import pizza_cheese.todo.dto.request.UpdateComboRequest;
import pizza_cheese.todo.dto.response.ComboResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.exception.ComboNotFoundException;
import pizza_cheese.todo.exception.PizzaNotFoundException;
import pizza_cheese.todo.exception.SlugAlreadyExistsException;
import pizza_cheese.todo.util.SlugUtil;

@Service
public class ComboService {

    private final ComboDao comboDao;
    private final PizzaDao pizzaDao;
    private final CloudinaryService cloudinaryService;

    public ComboService(ComboDao comboDao, PizzaDao pizzaDao, CloudinaryService cloudinaryService) {
        this.comboDao = comboDao;
        this.pizzaDao = pizzaDao;
        this.cloudinaryService = cloudinaryService;
    }

    public PageResponse<ComboResponse> findPage(boolean activeOnly, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = comboDao.countAll(activeOnly);
        List<ComboResponse> content = comboDao.findPage(activeOnly, safePage, safeSize)
                .stream()
                .map(ComboResponse::from)
                .toList();
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public ComboResponse findById(UUID id) {
        return comboDao.findById(id)
                .map(ComboResponse::from)
                .orElseThrow(() -> new ComboNotFoundException("Không tìm thấy combo"));
    }

    @Transactional
    public ComboResponse create(CreateComboRequest request) {
        return create(request, null);
    }

    @Transactional
    public ComboResponse create(CreateComboRequest request, MultipartFile imageFile) {
        validateComboItems(request.getItems());

        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), null);
        if (comboDao.existsBySlug(slug)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }

        Combo combo = new Combo();
        combo.setName(request.getName().trim());
        combo.setSlug(slug);
        combo.setDescription(request.getDescription());
        combo.setPrice(request.getPrice());
        combo.setDiscountPercent(request.getDiscountPercent());
        combo.setImageUrl(resolveImageUrl(request.getImageUrl(), imageFile));
        combo.setActive(request.getIsActive() == null || request.getIsActive());
        combo.setItems(toComboItems(request.getItems()));

        Combo saved = comboDao.save(combo);
        return ComboResponse.from(comboDao.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public ComboResponse update(UUID id, UpdateComboRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public ComboResponse update(UUID id, UpdateComboRequest request, MultipartFile imageFile) {
        Combo combo = comboDao.findById(id)
                .orElseThrow(() -> new ComboNotFoundException("Không tìm thấy combo"));

        if (request.getName() != null) {
            combo.setName(request.getName().trim());
        }

        String slug = SlugUtil.resolve(request.getSlug(), request.getName(), combo.getSlug());
        if (slug != null && comboDao.existsBySlugExcludingId(slug, id)) {
            throw new SlugAlreadyExistsException("Slug đã tồn tại: " + slug);
        }
        if (slug != null) {
            combo.setSlug(slug);
        }

        if (request.getDescription() != null) {
            combo.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            combo.setPrice(request.getPrice());
        }
        if (request.getDiscountPercent() != null) {
            combo.setDiscountPercent(request.getDiscountPercent());
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            combo.setImageUrl(cloudinaryService.uploadComboImage(imageFile));
        } else if (request.getImageUrl() != null) {
            combo.setImageUrl(request.getImageUrl());
        }
        if (request.getIsActive() != null) {
            combo.setActive(request.getIsActive());
        }

        if (request.getItems() != null) {
            validateComboItems(request.getItems());
            combo.setItems(toComboItems(request.getItems()));
        }

        Combo saved = comboDao.save(combo);
        return ComboResponse.from(comboDao.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(UUID id) {
        if (comboDao.findById(id).isEmpty()) {
            throw new ComboNotFoundException("Không tìm thấy combo");
        }
        comboDao.deactivate(id);
    }

    private void validateComboItems(List<ComboItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Combo phải có ít nhất một pizza");
        }
        long uniqueCount = items.stream().map(ComboItemRequest::getPizzaVariantId).distinct().count();
        if (uniqueCount != items.size()) {
            throw new IllegalArgumentException("Không được trùng pizza + size trong combo");
        }
        for (ComboItemRequest item : items) {
            Pizza pizza = pizzaDao.findById(item.getPizzaId())
                    .orElseThrow(() -> new PizzaNotFoundException("Không tìm thấy pizza: " + item.getPizzaId()));
            if (!pizza.isActive()) {
                throw new IllegalArgumentException("Pizza không còn hoạt động: " + pizza.getName());
            }
            PizzaVariant variant = pizza.getVariants().stream()
                    .filter(v -> v.getId().equals(item.getPizzaVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Size không hợp lệ cho pizza: " + pizza.getName()));
        }
    }

    private List<ComboItem> toComboItems(List<ComboItemRequest> requests) {
        return requests.stream().map(request -> {
            ComboItem item = new ComboItem();
            item.setPizzaId(request.getPizzaId());
            item.setPizzaVariantId(request.getPizzaVariantId());
            item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
            return item;
        }).toList();
    }

    private String resolveImageUrl(String imageUrl, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return cloudinaryService.uploadComboImage(imageFile);
        }
        return imageUrl;
    }
}

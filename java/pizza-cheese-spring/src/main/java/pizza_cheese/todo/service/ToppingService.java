package pizza_cheese.todo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.config.MenuCacheNames;
import pizza_cheese.todo.dao.ToppingDao;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.dto.request.CreateToppingRequest;
import pizza_cheese.todo.dto.request.UpdateToppingRequest;
import pizza_cheese.todo.dto.response.ToppingResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class ToppingService {

    private final ToppingDao toppingDao;
    private final MenuCacheEvictor menuCacheEvictor;

    public ToppingService(ToppingDao toppingDao, MenuCacheEvictor menuCacheEvictor) {
        this.toppingDao = toppingDao;
        this.menuCacheEvictor = menuCacheEvictor;
    }

    @Cacheable(cacheNames = MenuCacheNames.TOPPINGS, key = "'all:' + #activeOnly")
    public List<ToppingResponse> findAll(boolean activeOnly) {
        return new ArrayList<>(toppingDao.findAll(activeOnly).stream().map(ToppingResponse::from).toList());
    }

    @Cacheable(cacheNames = MenuCacheNames.TOPPINGS, key = "'id:' + #id")
    public ToppingResponse findById(UUID id) {
        return toppingDao.findById(id)
                .map(ToppingResponse::from)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy topping"));
    }

    // những hàm có ghi dữ liệu xuống DB.
    @Transactional
    public ToppingResponse create(CreateToppingRequest request) {
        Topping topping = new Topping();
        topping.setName(request.getName().trim());
        topping.setPrice(request.getPrice());
        topping.setActive(request.getIsActive() == null || request.getIsActive());

        Topping saved = toppingDao.save(topping);
        menuCacheEvictor.evictToppingsAndPizzas();
        return ToppingResponse.from(saved);
    }

    @Transactional
    public ToppingResponse update(UUID id, UpdateToppingRequest request) {
        Topping topping = toppingDao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy topping"));

        if (request.getName() != null) {
            topping.setName(request.getName().trim());
        }
        if (request.getPrice() != null) {
            topping.setPrice(request.getPrice());
        }
        if (request.getIsActive() != null) {
            topping.setActive(request.getIsActive());
        }

        Topping saved = toppingDao.save(topping);
        menuCacheEvictor.evictToppingsAndPizzas();
        return ToppingResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (toppingDao.findById(id).isEmpty()) {
            throw ApiException.notFound("Không tìm thấy topping");
        }
        toppingDao.deactivate(id);
        menuCacheEvictor.evictToppingsAndPizzas();
    }

    public void validateToppingIds(List<UUID> toppingIds) {
        if (toppingIds == null || toppingIds.isEmpty()) {
            return;
        }
        long uniqueCount = toppingIds.stream().distinct().count();
        if (uniqueCount != toppingIds.size()) {
            throw new IllegalArgumentException("Không được trùng topping");
        }
        for (UUID toppingId : toppingIds) {
            Topping topping = toppingDao.findById(toppingId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy topping: " + toppingId));
            if (!topping.isActive()) {
                throw new IllegalArgumentException("Topping không còn hoạt động: " + topping.getName());
            }
        }
    }
}

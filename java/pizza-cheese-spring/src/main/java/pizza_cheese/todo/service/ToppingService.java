package pizza_cheese.todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.ToppingDao;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.dto.request.CreateToppingRequest;
import pizza_cheese.todo.dto.request.UpdateToppingRequest;
import pizza_cheese.todo.dto.response.ToppingResponse;
import pizza_cheese.todo.exception.ToppingNotFoundException;

@Service
public class ToppingService {

    private final ToppingDao toppingDao;

    public ToppingService(ToppingDao toppingDao) {
        this.toppingDao = toppingDao;
    }

    public List<ToppingResponse> findAll(boolean activeOnly) {
        return toppingDao.findAll(activeOnly).stream().map(ToppingResponse::from).toList();
    }

    public ToppingResponse findById(UUID id) {
        return toppingDao.findById(id)
                .map(ToppingResponse::from)
                .orElseThrow(() -> new ToppingNotFoundException("Không tìm thấy topping"));
    }

    // những hàm có ghi dữ liệu xuống DB.
    @Transactional
    public ToppingResponse create(CreateToppingRequest request) {
        Topping topping = new Topping();
        topping.setName(request.getName().trim());
        topping.setPrice(request.getPrice());
        topping.setActive(request.getIsActive() == null || request.getIsActive());

        return ToppingResponse.from(toppingDao.save(topping));
    }

    @Transactional
    public ToppingResponse update(UUID id, UpdateToppingRequest request) {
        Topping topping = toppingDao.findById(id)
                .orElseThrow(() -> new ToppingNotFoundException("Không tìm thấy topping"));

        if (request.getName() != null) {
            topping.setName(request.getName().trim());
        }
        if (request.getPrice() != null) {
            topping.setPrice(request.getPrice());
        }
        if (request.getIsActive() != null) {
            topping.setActive(request.getIsActive());
        }

        return ToppingResponse.from(toppingDao.save(topping));
    }

    @Transactional
    public void delete(UUID id) {
        if (toppingDao.findById(id).isEmpty()) {
            throw new ToppingNotFoundException("Không tìm thấy topping");
        }
        toppingDao.deactivate(id);
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
                    .orElseThrow(() -> new ToppingNotFoundException("Không tìm thấy topping: " + toppingId));
            if (!topping.isActive()) {
                throw new IllegalArgumentException("Topping không còn hoạt động: " + topping.getName());
            }
        }
    }
}

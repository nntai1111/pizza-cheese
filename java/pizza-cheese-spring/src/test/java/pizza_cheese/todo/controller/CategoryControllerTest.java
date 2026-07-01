package pizza_cheese.todo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import pizza_cheese.todo.dto.request.CreateCategoryRequest;
import pizza_cheese.todo.dto.response.CategoryResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.CategoryService;

class CategoryControllerTest {

    private CategoryService categoryService;
    private CategoryController controller;
    private UUID deletedId;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(null, null) {
            @Override
            public List<CategoryResponse> findAll(boolean activeOnly) {
                CategoryResponse category = new CategoryResponse();
                category.setId(UUID.randomUUID());
                category.setName("Pizza cổ điển");
                return List.of(category);
            }

            @Override
            public CategoryResponse findById(UUID id) {
                CategoryResponse category = new CategoryResponse();
                category.setId(id);
                category.setName("Pizza hải sản");
                return category;
            }

            @Override
            public CategoryResponse create(CreateCategoryRequest request) {
                CategoryResponse created = new CategoryResponse();
                created.setName(request.getName());
                return created;
            }

            @Override
            public void delete(UUID id) {
                deletedId = id;
            }
        };
        controller = new CategoryController(categoryService);
    }

    @Test
    void list_returnsCategoriesFromService() {
        ResponseEntity<RestResponse<List<CategoryResponse>>> response = controller.list(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getStatusCode());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Pizza cổ điển", response.getBody().getData().get(0).getName());
    }

    @Test
    void getById_returnsCategoryFromService() {
        UUID id = UUID.randomUUID();

        ResponseEntity<RestResponse<CategoryResponse>> response = controller.getById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getData().getId());
        assertEquals("Pizza hải sản", response.getBody().getData().getName());
    }

    @Test
    void createJson_delegatesToService() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Pizza mới");

        ResponseEntity<RestResponse<CategoryResponse>> response = controller.createJson(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Pizza mới", response.getBody().getData().getName());
    }

    @Test
    void delete_delegatesToServiceAndReturnsEmptyData() {
        UUID id = UUID.randomUUID();

        ResponseEntity<RestResponse<Void>> response = controller.delete(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getStatusCode());
        assertNull(response.getBody().getData());
        assertEquals(id, deletedId);
    }
}

package pizza_cheese.todo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import pizza_cheese.todo.dto.request.CreateComboRequest;
import pizza_cheese.todo.dto.response.ComboResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.ComboService;

class ComboControllerTest {

    private ComboService comboService;
    private ComboController controller;
    private UUID deletedId;

    @BeforeEach
    void setUp() {
        comboService = new ComboService(null, null, null) {
            @Override
            public PageResponse<ComboResponse> findPage(boolean activeOnly, int page, int size) {
                ComboResponse combo = new ComboResponse();
                combo.setId(UUID.randomUUID());
                combo.setName("Combo gia đình");
                return PageResponse.of(List.of(combo), page, size, 1);
            }

            @Override
            public ComboResponse findById(UUID id) {
                ComboResponse combo = new ComboResponse();
                combo.setId(id);
                combo.setName("Combo đôi");
                return combo;
            }

            @Override
            public ComboResponse create(CreateComboRequest request) {
                ComboResponse created = new ComboResponse();
                created.setName(request.getName());
                return created;
            }

            @Override
            public void delete(UUID id) {
                deletedId = id;
            }
        };
        controller = new ComboController(); // lúc này controller.service == null; -> chưa dùng được
        ReflectionTestUtils.setField(controller, "comboService", comboService);// phụ thuộc spring boot test, setField
                                                                               // để inject service vào controller
    }

    @Test
    void list_returnsPagedCombosFromService() {
        ResponseEntity<RestResponse<PageResponse<ComboResponse>>> response = controller.list(true, 0, 12);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().getContent().size());
        assertEquals("Combo gia đình", response.getBody().getData().getContent().get(0).getName());
    }

    @Test
    void getById_returnsComboFromService() {
        UUID id = UUID.randomUUID();

        ResponseEntity<RestResponse<ComboResponse>> response = controller.getById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getData().getId());
        assertEquals("Combo đôi", response.getBody().getData().getName());
    }

    @Test
    void createJson_delegatesToService() {
        CreateComboRequest request = new CreateComboRequest();
        request.setName("Combo mới");
        request.setPrice(new BigDecimal("199000"));

        ResponseEntity<RestResponse<ComboResponse>> response = controller.createJson(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Combo mới", response.getBody().getData().getName());
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

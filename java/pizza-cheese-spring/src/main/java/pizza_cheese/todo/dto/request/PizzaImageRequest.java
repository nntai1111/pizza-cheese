package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PizzaImageRequest {

    @NotBlank(message = "URL ảnh không được để trống")
    private String imageUrl;

    private Boolean isMain = false;
    private Integer sortOrder = 0;
}

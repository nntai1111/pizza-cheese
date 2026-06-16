package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 100, message = "Slug không được vượt quá 100 ký tự")
    private String slug;

    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean isActive;
}

package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PizzaImageRequest {

    @NotBlank(message = "URL ảnh không được để trống")
    private String imageUrl;

    private Boolean isMain = false;
    private Integer sortOrder = 0;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}

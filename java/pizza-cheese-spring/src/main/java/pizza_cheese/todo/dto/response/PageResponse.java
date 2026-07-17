package pizza_cheese.todo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    /** Server time to send back as updatedSince on the next incremental poll. */
    private LocalDateTime syncedAt;
    /** true = content is only orders changed since updatedSince (not a full page). */
    private boolean incremental;

    public PageResponse() {
    }

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        PageResponse<T> response = new PageResponse<>(content, page, size, totalElements);
        response.setSyncedAt(LocalDateTime.now());
        response.setIncremental(false);
        return response;
    }

    public static <T> PageResponse<T> incremental(List<T> changes, long totalElements) {
        int size = Math.max(changes.size(), 1);
        PageResponse<T> response = new PageResponse<>(changes, 0, size, totalElements);
        response.setSyncedAt(LocalDateTime.now());
        response.setIncremental(true);
        return response;
    }
}

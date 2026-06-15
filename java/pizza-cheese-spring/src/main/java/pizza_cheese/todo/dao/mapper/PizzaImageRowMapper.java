package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.PizzaImage;

public class PizzaImageRowMapper implements RowMapper<PizzaImage> {

    @Override
    public PizzaImage mapRow(ResultSet rs, int rowNum) throws SQLException {
        PizzaImage image = new PizzaImage();
        image.setId(rs.getObject("id", UUID.class));
        image.setPizzaId(rs.getObject("pizza_id", UUID.class));
        image.setImageUrl(rs.getString("image_url"));
        image.setMain(rs.getBoolean("is_main"));
        image.setSortOrder(rs.getInt("sort_order"));
        return image;
    }
}

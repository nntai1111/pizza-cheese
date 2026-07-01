package pizza_cheese.todo.dao.mapper;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.BeanPropertyRowMapper;

import pizza_cheese.todo.domain.CodedEnum;
import pizza_cheese.todo.util.CodedEnums;

// chuyển số (code) trong database thành Enum.
public class CodedEnumBeanPropertyRowMapper<T> extends BeanPropertyRowMapper<T> {

    public CodedEnumBeanPropertyRowMapper(Class<T> mappedClass) {
        super(mappedClass);
    }

    public static <T> CodedEnumBeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
        return new CodedEnumBeanPropertyRowMapper<>(mappedClass);
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        Class<?> propertyType = pd.getPropertyType();

        if (propertyType != null && CodedEnum.class.isAssignableFrom(propertyType)) {
            int code = rs.getInt(index);
            if (rs.wasNull()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends CodedEnum> enumType = (Class<? extends CodedEnum>) propertyType;
            return CodedEnums.fromCode(enumType, code);
        }

        return super.getColumnValue(rs, index, pd);
    }
}

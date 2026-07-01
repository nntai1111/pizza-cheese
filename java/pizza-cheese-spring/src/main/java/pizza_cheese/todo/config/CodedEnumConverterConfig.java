package pizza_cheese.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import pizza_cheese.todo.domain.CodedEnum;
import pizza_cheese.todo.util.CodedEnums;

// Class này giúp Spring tự động chuyển chuỗi (String) thành Enum (CodedEnum)
@Configuration
public class CodedEnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CodedEnumConverterFactory());
    }

    private static final class CodedEnumConverterFactory implements ConverterFactory<String, CodedEnum> {

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T extends CodedEnum> Converter<String, T> getConverter(Class<T> targetType) {
            Class<? extends CodedEnum> enumType = targetType;
            return source -> (T) CodedEnums.parse((Class) enumType, source);
        }
    }
}

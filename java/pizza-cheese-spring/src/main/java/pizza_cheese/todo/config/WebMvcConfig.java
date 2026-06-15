package pizza_cheese.todo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring chỉ cho phép Jackson parse JSON
// nhưng khi client gửi request với content-type là application/octet-stream thì sẽ bị lỗi 415 Unsupported Media Type
// nên cần cấu hình để Jackson có thể parse JSON từ content-type application/octet-stream
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
				List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
				if (!supportedMediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
					supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
					jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
				}
			}
		}
	}
}

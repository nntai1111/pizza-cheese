package pizza_cheese.todo.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

	private final Cloudinary cloudinary;

	public CloudinaryService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	public Map<String, Object> upload(MultipartFile file) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> result = cloudinary.uploader().upload(
				file.getBytes(),
				ObjectUtils.asMap("resource_type", "auto"));
		return result;
	}

	public Map<String, Object> uploadFromUrl(String imageUrl) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> result = cloudinary.uploader().upload(imageUrl, ObjectUtils.emptyMap());
		return result;
	}

	public String getOptimizedUrl(String publicId) {
		return cloudinary.url()
				.transformation(new Transformation()
						.fetchFormat("auto")
						.quality("auto"))
				.generate(publicId);
	}
}

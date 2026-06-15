package pizza_cheese.todo.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import pizza_cheese.todo.config.CloudinaryProperties;
import pizza_cheese.todo.exception.FileUploadException;

@Service
public class CloudinaryService {

	private final Cloudinary cloudinary;
	private final CloudinaryProperties properties;

	public CloudinaryService(Cloudinary cloudinary, CloudinaryProperties properties) {
		this.cloudinary = cloudinary;
		this.properties = properties;
	}

	public String uploadAvatar(MultipartFile file) {
		return upload(file, properties.getFolderAvatar());
	}

	public String uploadPizzaImage(MultipartFile file) {
		return upload(file, properties.getFolderPizza());
	}

	public String uploadCategoryImage(MultipartFile file) {
		return upload(file, properties.getFolderCategory());
	}

	public String upload(MultipartFile file, String folder) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> result = cloudinary.uploader().upload(
					file.getBytes(),
					ObjectUtils.asMap(
							"resource_type", "auto",
							"folder", folder));
			return (String) result.get("secure_url");
		} catch (Exception ex) {
			String detail = ex.getMessage();
			if (detail != null && detail.contains("missing permissions")) {
				throw new FileUploadException(
						"API key Cloudinary chưa có quyền upload. Vào Cloudinary Console → Settings → API Keys → chọn key → Assign Roles → gán role Master Admin (hoặc role có quyền create/upload).",
						ex);
			}
			throw new FileUploadException("Không thể tải ảnh lên Cloudinary", ex);
		}
	}

	public Map<String, Object> uploadFromUrl(String imageUrl, String folder) throws IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> result = cloudinary.uploader().upload(
				imageUrl,
				ObjectUtils.asMap("folder", folder));
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

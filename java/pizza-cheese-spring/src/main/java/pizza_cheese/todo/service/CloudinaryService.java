package pizza_cheese.todo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import jakarta.annotation.PreDestroy;
import pizza_cheese.todo.config.CloudinaryProperties;
import pizza_cheese.todo.exception.ApiException;

@Service
public class CloudinaryService {

	private final Cloudinary cloudinary;
	private final CloudinaryProperties properties;
	private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(4);

	public CloudinaryService(Cloudinary cloudinary, CloudinaryProperties properties) {
		this.cloudinary = cloudinary;
		this.properties = properties;
	}

	@PreDestroy
	void shutdown() {
		uploadExecutor.shutdown();
	}

	public String uploadAvatar(MultipartFile file) {
		return upload(file, properties.getFolderAvatar());
	}

	public String uploadPizzaImage(MultipartFile file) {
		return upload(file, properties.getFolderPizza());
	}

	/** Upload nhiều ảnh pizza song song để giảm tổng thời gian chờ Cloudinary. */
	public List<String> uploadPizzaImages(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return List.of();
		}
		if (files.size() == 1) {
			return List.of(uploadPizzaImage(files.get(0)));
		}

		List<CompletableFuture<String>> futures = new ArrayList<>(files.size());
		for (MultipartFile file : files) {
			futures.add(CompletableFuture.supplyAsync(
					() -> uploadPizzaImage(file),
					uploadExecutor));
		}

		try {
			List<String> urls = new ArrayList<>(futures.size());
			for (CompletableFuture<String> future : futures) {
				urls.add(future.join());
			}
			return urls;
		} catch (CompletionException ex) {
			Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
			if (cause instanceof ApiException apiException) {
				throw apiException;
			}
			throw ApiException.uploadFailed("Không thể tải ảnh lên Cloudinary", cause);
		}
	}

	public String uploadCategoryImage(MultipartFile file) {
		return upload(file, properties.getFolderCategory());
	}

	public String uploadComboImage(MultipartFile file) {
		return upload(file, properties.getFolderCombo());
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
				throw ApiException.uploadFailed(
						"API key Cloudinary chưa có quyền upload. Vào Cloudinary Console → Settings → API Keys → chọn key → Assign Roles → gán role Master Admin (hoặc role có quyền create/upload).",
						ex);
			}
			throw ApiException.uploadFailed("Không thể tải ảnh lên Cloudinary", ex);
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

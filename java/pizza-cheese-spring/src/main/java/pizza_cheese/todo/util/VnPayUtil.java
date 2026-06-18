package pizza_cheese.todo.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Theo mẫu JSP/Servlet chính thức của VNPay:
 * - hashData: fieldName=encode(value) với space → '+'
 * - query URL: encode(fieldName)=encode(value)
 * - HMAC-SHA512 tính trên hashData (không phải query URL)
 */
public final class VnPayUtil {

    private VnPayUtil() {
    }

    public static String buildPaymentUrl(String baseUrl, Map<String, String> params, String hashSecret) {
        List<String> fieldNames = sortedNonEmptyKeys(params);

        List<String> hashParts = new ArrayList<>();
        List<String> queryParts = new ArrayList<>();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            hashParts.add(fieldName + "=" + encodeHashValue(fieldValue));
            queryParts.add(urlEncode(fieldName) + "=" + urlEncode(fieldValue));
        }

        String hashData = String.join("&", hashParts);
        String query = String.join("&", queryParts);
        String secureHash = hmacSha512(hashSecret, hashData);

        return baseUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public static boolean verifySignature(Map<String, String> params, String hashSecret) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> signParams = new java.util.TreeMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        String expectedHash = hmacSha512(hashSecret, buildHashData(signParams));
        return expectedHash.equalsIgnoreCase(receivedHash);
    }

    static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = sortedNonEmptyKeys(params);
        List<String> hashParts = new ArrayList<>();
        for (String fieldName : fieldNames) {
            hashParts.add(fieldName + "=" + encodeHashValue(params.get(fieldName)));
        }
        return String.join("&", hashParts);
    }

    private static List<String> sortedNonEmptyKeys(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                fieldNames.add(entry.getKey());
            }
        }
        Collections.sort(fieldNames);
        return fieldNames;
    }

    /** Giá trị trong chuỗi hash: US-ASCII, space thành '+'. */
    private static String encodeHashValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII).replace("%20", "+");
    }

    /** Encode cho query URL. */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    public static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                hash.append(String.format("%02x", b & 0xff));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay", ex);
        }
    }
}

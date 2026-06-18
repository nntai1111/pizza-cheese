package pizza_cheese.todo.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VnPayUtilTest {

    @Test
    void buildPaymentUrl_containsSecureHashAndRequiredParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "FCR0JH8M");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "PC2506181234");
        params.put("vnp_OrderInfo", "Thanh toan don PC2506181234");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", "http://localhost:8080/api/v1/payments/vnpay/return");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20250618103800");
        params.put("vnp_ExpireDate", "20250618105300");

        String url = VnPayUtil.buildPaymentUrl(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                params,
                "17W6NZM6YJ35QKQ1O42636HAPRLGB7FX");

        assertTrue(url.contains("vnp_SecureHash="));
        assertTrue(url.contains("vnp_TmnCode=FCR0JH8M"));
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
    }

    @Test
    void verifySignature_acceptsOwnPaymentUrlParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "FCR0JH8M");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "PC2506181234");
        params.put("vnp_OrderInfo", "Thanh toan don PC2506181234");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", "http://localhost:8080/api/v1/payments/vnpay/return");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20250618103800");
        params.put("vnp_ExpireDate", "20250618105300");

        String secret = "17W6NZM6YJ35QKQ1O42636HAPRLGB7FX";
        String url = VnPayUtil.buildPaymentUrl(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", params, secret);

        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                parsed.put(pair.substring(0, idx), java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.US_ASCII));
            }
        }

        assertTrue(VnPayUtil.verifySignature(parsed, secret));
    }

    @Test
    void verifySignature_rejectsTamperedAmount() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "PC2506181234");
        params.put("vnp_SecureHash", "invalid");

        assertFalse(VnPayUtil.verifySignature(params, "17W6NZM6YJ35QKQ1O42636HAPRLGB7FX"));
    }
}

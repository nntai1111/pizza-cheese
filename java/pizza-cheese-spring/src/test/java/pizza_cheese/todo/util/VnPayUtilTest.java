package pizza_cheese.todo.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VnPayUtilTest {

    private static final String TEST_HASH_SECRET = "TEST_VNPAY_HASH_SECRET_FOR_UNIT_TESTS";
    private static final String TEST_TMN_CODE = "TESTTMNCODE";

    @Test
    void buildPaymentUrl_containsSecureHashAndRequiredParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", TEST_TMN_CODE);
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
                TEST_HASH_SECRET);

        assertTrue(url.contains("vnp_SecureHash="));
        assertTrue(url.contains("vnp_TmnCode=" + TEST_TMN_CODE));
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
    }

    @Test
    void verifySignature_acceptsOwnPaymentUrlParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", TEST_TMN_CODE);
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
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", params, TEST_HASH_SECRET);

        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                parsed.put(pair.substring(0, idx), java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.US_ASCII));
            }
        }

        assertTrue(VnPayUtil.verifySignature(parsed, TEST_HASH_SECRET));
    }

    @Test
    void verifySignature_rejectsTamperedAmount() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "PC2506181234");
        params.put("vnp_SecureHash", "invalid");

        assertFalse(VnPayUtil.verifySignature(params, TEST_HASH_SECRET));
    }
}

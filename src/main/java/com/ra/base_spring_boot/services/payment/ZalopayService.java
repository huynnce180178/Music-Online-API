package com.ra.base_spring_boot.services.payment;

import com.ra.base_spring_boot.Configuration.ZalopayConfig;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.model.Payment;
import com.ra.base_spring_boot.model.constants.PaymentStatus;
import com.ra.base_spring_boot.repository.PaymentRepository;
import com.ra.base_spring_boot.services.payment.crypto.HMACUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ZalopayService {

    @Autowired
    private PaymentRepository paymentRepository;

    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            throw new HttpBadRequest("Payment not found");
        }
        payment.setPaymentStatus(status);
        paymentRepository.save(payment);
    }

    private static String getCurrentTimeString(String format) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setCalendar(cal);
        return fmt.format(cal.getTimeInMillis());
    }

    public String createOrder(Long userId,Double amount) {
        Random rand = new Random();
        int randomId = rand.nextInt(1000000);

        Long  total = Math.round(amount);
        if (amount == null) {
            return "{\"error\": \"Amount is required\"}";
        }

        Map<String, Object> order = new HashMap<>();
        order.put("app_id", ZalopayConfig.config.get("app_id"));
        order.put("app_trans_id", getCurrentTimeString("yyMMdd") + "_" + randomId);
        order.put("app_time", System.currentTimeMillis());
        order.put("app_user", "user123");
        order.put("amount", total);
        order.put("description", "SN Mobile - Payment for the order #" + randomId);
        order.put("bank_code", "");
        order.put("item", "[{}]");
        order.put("embed_data", "{}");
        order.put("callback_url", "https://ef5e1e22d1fc.ngrok-free.app/api/v1/user/payment/zalopay/order-status/"+getCurrentTimeString("yyMMdd") + "_" + randomId);

        String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|"
                + order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|"
                + order.get("item");
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, ZalopayConfig.config.get("key1"), data);
        order.put("mac", mac);

        System.out.println("Generated MAC: " + mac);
        System.out.println(getCurrentTimeString("yyMMdd") + "_" + randomId);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(ZalopayConfig.config.get("endpoint"));

            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }

            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }

                System.out.println("Zalopay Response: " + resultJsonStr.toString());
                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();

            return "{\"error\": \"Failed to create order: " + e.getMessage() + "\"}";
        }
    }

    public String getOrderStatus(Long userId,String appTransId) {
        String data = ZalopayConfig.config.get("app_id") + "|" + appTransId + "|" + ZalopayConfig.config.get("key1");
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, ZalopayConfig.config.get("key1"), data);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(ZalopayConfig.config.get("orderstatus"));

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("app_id", ZalopayConfig.config.get("app_id")));
            params.add(new BasicNameValuePair("app_trans_id", appTransId));
            params.add(new BasicNameValuePair("mac", mac));

            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder resultJsonStr = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultJsonStr.append(line);
                }
                updatePaymentStatus(userId, PaymentStatus.COMPLETED);
                return resultJsonStr.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            updatePaymentStatus(userId, PaymentStatus.FAILED);
            return "{\"error\": \"Failed to get order status: " + e.getMessage() + "\"}";
        }
    }

}
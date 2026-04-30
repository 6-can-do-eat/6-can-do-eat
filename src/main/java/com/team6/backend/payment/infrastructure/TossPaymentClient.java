package com.team6.backend.payment.infrastructure;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.payment.infrastructure.dto.TossPaymentConfirmResponse;
import com.team6.backend.payment.infrastructure.dto.TossPaymentRequest;
import com.team6.backend.payment.infrastructure.dto.TossPaymentResponse;
import com.team6.backend.payment.presentation.dto.PaymentConfirmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentClient {

    private final RestClient restClient;
    private final String secretKey;

    public TossPaymentClient(
            @Value("${toss.payments.base-url}") String baseUrl,
            @Value("${toss.payments.secret-key}") String secretKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.secretKey = secretKey;
    }

    public TossPaymentResponse createPayment(TossPaymentRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/payments")
                    .headers(this::setAuthHeaders)
                    .body(request)
                    .retrieve()
                    .body(TossPaymentResponse.class);
        } catch (RestClientResponseException e) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public TossPaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(this::setAuthHeaders)
                    .body(request)
                    .retrieve()
                    .body(TossPaymentConfirmResponse.class);
        } catch (RestClientResponseException e) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void setAuthHeaders(HttpHeaders headers) {
        String encodedSecretKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
}

package com.rak.studentmanagement.service.impl;

import com.rak.studentmanagement.config.FeeCollectionProperties;
import com.rak.studentmanagement.exception.BusinessException;
import com.rak.studentmanagement.model.PaymentRequestDto;
import com.rak.studentmanagement.model.ReceiptResponseDto;
import com.rak.studentmanagement.service.FeeServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.rak.studentmanagement.constant.FeeClientConstants.*;
import static java.util.Collections.emptyList;

@Service
public class FeeServiceClientImpl implements FeeServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(FeeServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final FeeCollectionProperties feeCollectionProperties;

    private static final String CIRCUIT_BEAKER_NAME = "fetchReceiptDetails";
    private static final String FALLBACK_METHOD = "fetchReceiptDetailsFallback";


    public FeeServiceClientImpl(RestTemplate restTemplate, FeeCollectionProperties feeCollectionProperties) {
        this.restTemplate = restTemplate;
        this.feeCollectionProperties = feeCollectionProperties;
    }

    @Override
//    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
//    @CircuitBreaker(name = CIRCUIT_BEAKER_NAME, fallbackMethod = FALLBACK_METHOD)
    public ReceiptResponseDto performFeePayment(final PaymentRequestDto paymentRequest) {
        logger.info("Perform fee payment of Student ID :: [{}] ", paymentRequest.getStudentDto().getStudentId());
        try {
            ResponseEntity<ReceiptResponseDto> responseEntity = restTemplate.postForEntity(
                    feeCollectionProperties.getServices().get(HOST_URL) +
                            feeCollectionProperties.getServices().get(RECEIPT_BASE_URL) +
                            feeCollectionProperties.getServices().get(FEE_PAYMENT),
                    paymentRequest,
                    ReceiptResponseDto.class
            );
            return responseEntity.getBody();
        } catch (Exception ex) {
            logger.error("Error creating tuition fee: {}", ex.getMessage());
            throw new BusinessException(String.format("Failed to make tuition fee payment for Student ID [{}]", paymentRequest.getStudentDto().getStudentId()));
        }
    }

//    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
//    @CircuitBreaker(name = CIRCUIT_BEAKER_NAME, fallbackMethod = FALLBACK_METHOD)
    @Override
    public ReceiptResponseDto fetchReceiptDetails(final Long studentId) {
        logger.info("Fetch receipt details of Student ID :: [{}] ", studentId);
        ResponseEntity<ReceiptResponseDto> responseEntity = restTemplate.getForEntity(
                feeCollectionProperties.getServices().get(HOST_URL) +
                        feeCollectionProperties.getServices().get(RECEIPT_BASE_URL) +
                        feeCollectionProperties.getServices().get(RECEIPT_DETAILS) +
                        studentId,
                ReceiptResponseDto.class
        );
        return responseEntity.getBody();

    }

    private ReceiptResponseDto fetchReceiptDetailsFallback(Throwable throwable) {
        ReceiptResponseDto receiptResponse = new ReceiptResponseDto();
        receiptResponse.setReceiptDataList(emptyList());
        receiptResponse.setStatusCode(SERVICE_UNAVAILABLE_CODE);
        receiptResponse.setStatusMessage(SERVICE_UNAVAILABLE);
        return receiptResponse;
    }
}

package com.team6.backend.ai.application;

import com.team6.backend.ai.domain.entity.AiRequestLog;
import com.team6.backend.ai.domain.entity.AiRequestType;
import com.team6.backend.ai.domain.AiErrorCode;
import com.team6.backend.ai.domain.repository.AiRequestLogRepository;
import com.team6.backend.ai.infrastructure.GeminiClient;
import com.team6.backend.ai.presentation.dto.ProductDescriptionResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final String SHORT_ANSWER_SUFFIX = "답변을 최대한 간결하게 50자 이하로";

    private final GeminiClient geminiClient;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final SecurityUtils securityUtils;
    private final UserInfoRepository userInfoRepository;

    @Transactional
    public ProductDescriptionResponse generateProductDescription(String prompt) {
        validatePrompt(prompt);

        User user = userInfoRepository.findByIdAndDeletedAtIsNull(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        String result = geminiClient.generate(prompt + "\n" + SHORT_ANSWER_SUFFIX);

        aiRequestLogRepository.save(
                new AiRequestLog(user, prompt, result, AiRequestType.PRODUCT_DESCRIPTION)
        );
        return new ProductDescriptionResponse(prompt, result);
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new ApplicationException(AiErrorCode.AI_PROMPT_REQUIRED);
        }
        if (prompt.length() > 100) {
            throw new ApplicationException(AiErrorCode.AI_PROMPT_TOO_LONG);
        }
    }
}

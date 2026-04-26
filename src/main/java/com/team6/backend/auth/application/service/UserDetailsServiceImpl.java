package com.team6.backend.auth.application.service;


import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    // 1.  @AuthenticationPrincipal addressController에서 넘겨줌.

    private final userInfoRepository userInfoRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        return new UserDetailsImpl(user);
    }
}

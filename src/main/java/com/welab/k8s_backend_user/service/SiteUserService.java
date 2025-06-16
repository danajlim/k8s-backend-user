package com.welab.k8s_backend_user.service;

import com.welab.k8s_backend_user.common.exception.BadParameter;
import com.welab.k8s_backend_user.common.exception.NotFound;
import com.welab.k8s_backend_user.domain.SiteUser;
import com.welab.k8s_backend_user.domain.dto.SiteUserLoginDto;
import com.welab.k8s_backend_user.domain.dto.SiteUserRefreshDto;
import com.welab.k8s_backend_user.domain.dto.SiteUserRegisterDto;
import com.welab.k8s_backend_user.domain.event.SiteUserInfoEvent;
import com.welab.k8s_backend_user.domain.repository.SiteUserRepository;
import com.welab.k8s_backend_user.event.producer.KafkaMessageProducer;
import com.welab.k8s_backend_user.secret.hash.SecureHashUtils;
import com.welab.k8s_backend_user.secret.jwt.TokenGenerator;
import com.welab.k8s_backend_user.secret.jwt.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteUserService {

    private final SiteUserRepository siteUserRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final TokenGenerator tokenGenerator;

    @Transactional
    public void registerUser(SiteUserRegisterDto registerDto) {
        SiteUser siteUser = registerDto.toEntity();

        siteUserRepository.save(siteUser);

        SiteUserInfoEvent event = SiteUserInfoEvent.fromEntity("Create", siteUser);
        kafkaMessageProducer.send(SiteUserInfoEvent.Topic, event);
    }

    //로그인: Access+Refresh Token 발급
    @Transactional(readOnly = true)
    public TokenDto.AccessRefreshToken login(SiteUserLoginDto loginDto) {
        SiteUser user = siteUserRepository.findByUserId(loginDto.getUserId());
        if (user == null) {
            throw new NotFound("사용자를 찾을 수 없습니다.");
        }
        if (!SecureHashUtils.matches(loginDto.getPassword(), user.getPassword())) {
            throw new BadParameter("비밀번호가 맞지 않습니다.");
        }
        return tokenGenerator.generateAccessRefreshToken(loginDto.getUserId(), "WEB");
    }

    //Refresh Token -> Access Token 재발급
    @Transactional(readOnly = true)
    public TokenDto.AccessToken refresh(SiteUserRefreshDto refreshDto) {
        String userId = tokenGenerator.validateJwtToken(refreshDto.getToken());
        if (userId == null) {
            throw new BadParameter("토큰이 유효하지 않습니다");
        }

        SiteUser user = siteUserRepository.findByUserId(userId);
        if (user == null) {
            throw new NotFound("사용자를 찾을 수 없습니다");
        }
        return tokenGenerator.generateAccessToken(userId, "WEB");
    }

}

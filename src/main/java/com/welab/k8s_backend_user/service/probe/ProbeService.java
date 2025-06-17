package com.welab.k8s_backend_user.service.probe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProbeService {

    public void validateLiveness() {
        // TODO: 서비스가 죽은 상태로 판단되면 예외 발생시켜서 재시작 유도
    }

    public void validateReadiness() {
        // TODO: 준비되지 않은 상태라면 예외 발생시켜 트래픽 차단
    }
}

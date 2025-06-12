package com.welab.k8s_backend_user.api.open;

import com.welab.k8s_backend_user.common.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/user/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {
    @GetMapping(value = "/hello")
    public ApiResponseDto<String> hello() {
        return ApiResponseDto.createOk("웰컴 투 백엔드 유저");
    }
}
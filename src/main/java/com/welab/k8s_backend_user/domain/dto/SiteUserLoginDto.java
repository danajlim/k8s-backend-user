package com.welab.k8s_backend_user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

//로그인 요청 body DTO
@Getter @Setter
public class SiteUserLoginDto {

    @NotBlank(message = "아이디를 입력하세요")
    private String userId;

    @NotBlank(message = "비밀번호를 입력하세요")
    private String password;

}
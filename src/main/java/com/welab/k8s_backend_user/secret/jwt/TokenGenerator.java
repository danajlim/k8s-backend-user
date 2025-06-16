package com.welab.k8s_backend_user.secret.jwt;

import com.welab.k8s_backend_user.secret.jwt.dto.TokenDto;
import com.welab.k8s_backend_user.secret.jwt.props.JwtConfigProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

//JWT 생성, Refresh 검증, secretKey 관리
@Component
@RequiredArgsConstructor
public class TokenGenerator {

    private final JwtConfigProperties configProperties;

    //JWT 서명을 위한 비밀 키
    private volatile SecretKey secretKey;

    //비밀키를 처음 한번만 생성하고 재사용
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(configProperties.getSecretKey()));
                }
            }
        }
        return secretKey;
    }

    //기기 타입, 토큰 종류에 따른 만료 시간을 정하는 로직
    private int tokenExpiresIn(boolean refreshToken, String deviceType) {
        int expiresIn = 60*15;
        if (refreshToken) {
            if (deviceType != null) {
                if (deviceType.equals("WEB")) {
                    expiresIn = configProperties.getExpiresIn();
                } else if (deviceType.equals("MOBILE")) {
                    expiresIn = configProperties.getMobileExpiresIn();
                } else {
                    expiresIn = configProperties.getExpiresIn();
                }
            }
        }
        return expiresIn;
    }

    //JWT 토큰을 실제로 생성하는 메서드
    public TokenDto.JwtToken generateJwtToken(String userId, String deviceType, boolean refreshToken) {
        int tokenExpiresIn = tokenExpiresIn(refreshToken, deviceType);
        String tokenType = refreshToken ? "refresh" : "access";

        //JWT Token 생성
        String token = Jwts.builder()
                .issuer("welab")
                .subject(userId)
                .claim("usrId", userId)
                .claim("deviceType",deviceType)
                .claim("tokenType", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiresIn *1000L))
                .signWith(getSecretKey())
                .header().add("typ","JWT")
                .and()
                .compact();

        return new TokenDto.JwtToken(token, tokenExpiresIn);
    }

    //엑세스 토큰만 발급하는 메서드
    public TokenDto.AccessToken generateAccessToken(String userId, String deviceType) {
        TokenDto.JwtToken jwtToken = this.generateJwtToken(userId, deviceType, false);
        return new TokenDto.AccessToken(jwtToken);
    }

    //엑세스, 리프레시 토큰 발급하는 메서드
    public TokenDto.AccessRefreshToken generateAccessRefreshToken(String userId, String deviceType) {
        TokenDto.JwtToken accessJwtToken = this.generateJwtToken(userId, deviceType, false);
        TokenDto.JwtToken refreshJwtToken = this.generateJwtToken(userId, deviceType, true);
        return new TokenDto.AccessRefreshToken(accessJwtToken, refreshJwtToken);
    }

    //refresh token 유효성 검증 메서드
    public String validateJwtToken(String refreshToken) {
        String userId = null;

        final Claims claims = this.verifyAndGetClaims(refreshToken);

        if (claims == null) {
            return null;
        }

        Date expirationDate = claims.getExpiration();
        if (expirationDate == null || expirationDate.before(new Date())) {
            return null;
        }

        userId = claims.get("userId", String.class);

        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            return null;
        }

        return userId;

    }

    //토큰에서 claims를 꺼내는 내부 메서드
    private Claims verifyAndGetClaims(String token) {
        Claims claims;

        try {
            claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }
}

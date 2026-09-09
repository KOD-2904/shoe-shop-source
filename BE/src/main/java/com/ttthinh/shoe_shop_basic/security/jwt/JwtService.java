package com.ttthinh.shoe_shop_basic.security.jwt;


import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.security.user.UserDetailServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class JwtService {
      private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }
    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }
    // Tạo access token từ Authentication (lúc login)
    // Tạo access token từ Authentication (lúc login)
    public String generateAccessToken(Authentication authentication, String deviceId) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return getAccess(userDetails, deviceId);
    }
    // Tạo access token MỚI từ refresh token cũ (khi gọi refresh endpoint)
    // ⚠️ Cần truyền userDetails mới nhất để lấy roles hiện tại
    public String generateAccessTokenFromUserDetails(CustomUserDetails userDetails, String deviceId) {
        return getAccess(userDetails, deviceId);
    }
    private String getAccess(CustomUserDetails userDetails, String deviceId) {
        var roles = listRoles(userDetails);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .claim("tokenType", "ACCESS")
                .claim("deviceId", deviceId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    // Tạo refresh token từ Authentication (lúc login)
    public String generateRefreshToken(Authentication authentication, String deviceId) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return getRefresh(userDetails, deviceId);
    }
    public String generateRefreshTokenFromUserDetails(CustomUserDetails userDetails, String deviceId) {
        return getRefresh(userDetails, deviceId);
    }
    private String getRefresh(CustomUserDetails userDetails,String deviceId) {
        var roles = listRoles(userDetails);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .claim("tokenType", "REFRESH")
                .claim("deviceId", deviceId)  // ✅ Thêm deviceId vào JWT
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private List<String> listRoles(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

//    public String generateRefreshToken(Jws<Claims> claimsJwt){
//        var roles = claimsJwt.getBody().get("roles");
//        String userId = claimsJwt.getBody().get("userId", String.class);
//        String userName = claimsJwt.getBody().getSubject();
//        return Jwts.builder()
//                .setId(UUID.randomUUID().toString())
//                .setSubject(userName)
//                .claim("userId", userId)
//                .claim("roles", roles)
//                .claim("tokenType", "REFRESH")
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
//                .compact();
//    }
//    public String generateAccessToken(Jws<Claims> claimsJwt){
//        var roles = claimsJwt.getBody().get("roles");
//        String userId = claimsJwt.getBody().get("userId", String.class);
//        String userName = claimsJwt.getBody().getSubject();
//        return Jwts.builder()
//                .setId(UUID.randomUUID().toString())
//                .setSubject(userName)
//                .claim("userId", userId)
//                .claim("roles", roles)
//                .claim("tokenType", "ACCESS")
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
//                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
//                .compact();
//    }



//    public String getTokenType(Jws<Claims> claims) {
//        try {
//            return claims.getBody().get("tokenType", String.class);
//        } catch (JwtException e) {
//            throw new AppException(ErrorCode.UNAUTHENTICATED);
//        }
//    }
//    public String getTokenType(String token) {
//        try {
//            Jws<Claims> claimsJws = parseToken(token);
//            return claimsJws.getBody().get("tokenType", String.class);
//        } catch (JwtException e) {
//            throw new AppException(ErrorCode.UNAUTHENTICATED);
//        }
//    }
}

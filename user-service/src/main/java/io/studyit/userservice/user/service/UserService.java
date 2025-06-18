package io.studyit.userservice.user.service;

import io.studyit.jwt.JwtPayload;
import io.studyit.jwt.JwtTokenProvider;
import io.studyit.userservice.user.dto.LoginRequest;
import io.studyit.userservice.user.dto.TokenResponse;
import io.studyit.userservice.user.entity.RefreshToken;
import io.studyit.userservice.user.entity.User;
import io.studyit.userservice.user.repository.RefreshTokenRepository;
import io.studyit.userservice.user.repository.UserRepository;
import io.studyit.userservice.validation.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    public TokenResponse login(LoginRequest request) {

        UserValidator.validateLoginInput(request);

        Optional<User> userOptional = userRepository.findByUserId(request.getUserId());

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("존재하지 않는 사용자입니다.");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("아이디 혹은 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiryDate(new Date(System.currentTimeMillis() + jwtTokenProvider.getRefreshExpiration()))
                .build();

        refreshTokenRepository.save(tokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public TokenResponse refreshToken(String providedRefreshToken) {
        JwtPayload payload = jwtTokenProvider.getPayloadFromToken(providedRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findById(payload.userId())
                .orElseThrow(() -> new BadCredentialsException("해당 유저로 조회되는 리프레시 토큰 없음"));

        if (!storedToken.getToken().equals(providedRefreshToken)) {
            throw new BadCredentialsException("리프레시 토큰 일치하지 않음");
        }

        if (storedToken.getExpiryDate().before(new Date())) {
            throw new BadCredentialsException("리프레시 토큰 유효시간 만료");
        }

        User user = userRepository.findByUserId(payload.userId())
                .orElseThrow(() -> new BadCredentialsException("해당 리프레시 토큰을 위한 유저 없음"));

        String accessToken = jwtTokenProvider.createToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiryDate(
                        new Date(System.currentTimeMillis()
                                + jwtTokenProvider.getRefreshExpiration())
                )
                .build();

        refreshTokenRepository.save(tokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String refreshToken) {

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("리프레시 토큰은 필수 입력값입니다.");
        }

        JwtPayload payload;
        try {
            payload = jwtTokenProvider.getPayloadFromToken(refreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.");
        }

        RefreshToken storedToken = refreshTokenRepository.findById(payload.userId())
                .orElseThrow(() -> new BadCredentialsException("해당 유저로 저장된 리프레시 토큰이 없습니다."));

        if (!storedToken.getToken().equals(refreshToken)) {
            throw new BadCredentialsException("저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        if (storedToken.getExpiryDate().before(new Date())) {
            throw new BadCredentialsException("리프레시 토큰이 만료되었습니다.");
        }

        refreshTokenRepository.deleteById(payload.userId());
    }


}

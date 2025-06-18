package com.ohgiraffers.studyservice.studyjoin.command.controller;

import com.ohgiraffers.studyservice.common.ApiResponse;
import com.ohgiraffers.studyservice.studyjoin.command.dto.StudyJoinRequestDTO;
import com.ohgiraffers.studyservice.studyjoin.command.service.StudyJoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyJoinCommandController {

    private final StudyJoinService studyJoinService;

    // 스터디 참가 신청
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<String>> joinStudy(
            @AuthenticationPrincipal String userId,
            @RequestBody StudyJoinRequestDTO requestDTO) {

        try {
            studyJoinService.joinStudy(requestDTO, userId);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("스터디 참여 신청이 완료되었습니다."));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("STUDY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.failure("DUPLICATE_STUDY", e.getMessage()));
        }
    }

    // 스터치 참가 신청 취소
    // 스터디 참가 신청 취소
    @DeleteMapping("/cancel/{studyRoomId}")
    public ResponseEntity<ApiResponse<String>> cancelStudyJoin(@PathVariable Long studyRoomId,
                                                               @AuthenticationPrincipal String userId) {
        try {
            String resultMessage = studyJoinService.cancelJoinStudy(studyRoomId, userId);
            return ResponseEntity.ok(ApiResponse.success(resultMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("STUDY_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.failure("INVALID_STATUS", e.getMessage()));
        }
    }

}


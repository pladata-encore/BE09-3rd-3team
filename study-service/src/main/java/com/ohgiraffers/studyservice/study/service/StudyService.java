package com.ohgiraffers.studyservice.study.service;

import com.ohgiraffers.studyservice.study.dto.StudyCreateRequest;
import com.ohgiraffers.studyservice.study.dto.StudyResponse;
import com.ohgiraffers.studyservice.study.dto.StudyUpdateRequest;
import com.ohgiraffers.studyservice.study.entity.Study;
import com.ohgiraffers.studyservice.study.entity.StudyStatus;
import com.ohgiraffers.studyservice.study.exception.StudyInvalidRequestException;
import com.ohgiraffers.studyservice.study.exception.StudyStatusNotFoundException;
import com.ohgiraffers.studyservice.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;

    // 스터디 개설
    public StudyResponse createStudy(StudyCreateRequest request) {
        validateCreateRequest(request);

        Study study = Study.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .organizer(request.getOrganizer())
                .category(request.getCategory())
                .maxMembers(request.getMaxMembers())
                .status(StudyStatus.OPEN)
                .build();

        Study saved = studyRepository.save(study);
        log.info("스터디가 생성되었습니다. [postId={}, title={}]", saved.getStudyRoomId(), saved.getTitle());
        return StudyResponse.from(saved);
    }

    // 전체 스터디 목록 조회
    public List<StudyResponse> getAllStudies() {
        return studyRepository.findAll().stream()
                .map(StudyResponse::from)
                .collect(Collectors.toList());
    }

    // studyRoomID로 스터디 상세 조회
    public StudyResponse getStudyById(Long studyRoomId) {
        Study study = studyRepository.findById(studyRoomId)
                .orElseThrow(() -> new StudyStatusNotFoundException(studyRoomId));
        return StudyResponse.from(study);
    }

    // 스터디 마감
    public void closeStudy(Long studyRoomId) {
        Study study = studyRepository.findById(studyRoomId)
                .orElseThrow(() -> new StudyStatusNotFoundException(studyRoomId));

        study.setStatus(StudyStatus.CLOSED);
        study.setClosedAt(LocalDateTime.now());
        studyRepository.save(study);

        log.info("스터디가 마감되었습니다. [postId={}, title={}, closedAt={}]",
                study.getStudyRoomId(), study.getTitle(), study.getClosedAt());
    }

    // 스터디 내용 수정
    public StudyResponse updateStudy(Long studyRoomId, StudyUpdateRequest request) {
        validateUpdateRequest(request);

        Study study = studyRepository.findById(studyRoomId)
                .orElseThrow(() -> new StudyStatusNotFoundException(studyRoomId));

        study.setTitle(request.getTitle());
        study.setDescription(request.getDescription());
        study.setCategory(request.getCategory());
        study.setMaxMembers(request.getMaxMembers());

        Study updated = studyRepository.save(study);
        log.info("스터디를 수정하였습니다. [postId={}, title={}]", updated.getStudyRoomId(), updated.getTitle());
        return StudyResponse.from(updated);
    }

    // 스터디 삭제
    public void deleteStudy(Long studyRoomId) {
        Study study = studyRepository.findById(studyRoomId)
                .orElseThrow(() -> new StudyStatusNotFoundException(studyRoomId));

        studyRepository.delete(study);
        log.info("스터디가 삭제되었습니다. [postId={}, title={}]", study.getStudyRoomId(), study.getTitle());
    }

    // 제목, 카테고리, 주최자 키워드로 검색
    public List<StudyResponse> searchStudiesByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("검색어가 입력되지 않았습니다.");
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        String lowerKeyword = keyword.toLowerCase();

        List<StudyResponse> result = studyRepository.findAll().stream()
                .filter(study ->
                        study.getTitle().toLowerCase().contains(lowerKeyword) ||
                                study.getCategory().toLowerCase().contains(lowerKeyword) ||
                                study.getOrganizer().toLowerCase().contains(lowerKeyword)
                )
                .map(StudyResponse::from)
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            log.warn("'{}' 키워드로 검색된 결과가 없습니다.", keyword);
            throw new IllegalArgumentException("해당 검색어로 일치하는 스터디가 없습니다.");
        }

        log.info("'{}' 키워드로 검색된 스터디 {}건", keyword, result.size());
        return result;
    }

    // 스터디 생성 유효성 검사
    private void validateCreateRequest(StudyCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new StudyInvalidRequestException("스터디 제목은 필수입니다.");
        }
        if (request.getOrganizer() == null || request.getOrganizer().trim().isEmpty()) {
            throw new StudyInvalidRequestException("스터디 주최자는 필수입니다.");
        }
        if (request.getMaxMembers() <= 0) {
            throw new StudyInvalidRequestException("최대 인원은 1명 이상이어야 합니다.");
        }
    }

    // 스터디 수정 유효성 검사
    private void validateUpdateRequest(StudyUpdateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new StudyInvalidRequestException("수정할 제목은 비어 있을 수 없습니다.");
        }
        if (request.getMaxMembers() <= 0) {
            throw new StudyInvalidRequestException("최대 인원은 1명 이상이어야 합니다.");
        }
    }
}

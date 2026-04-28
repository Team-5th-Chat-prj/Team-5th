package com.clone.getchu.domain.location.service;

import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 위치 정보 DB 저장 전담 컴포넌트
 * LocationService의 외부 API 호출과 분리하여 트랜잭션이 DB 작업 구간만 점유하도록 보장
 */
@Component
@RequiredArgsConstructor
public class LocationUpdater {

    private final MemberRepository memberRepository;

    @Transactional
    public LocationVerifyResponse update(Long memberId, Point location, String locationName) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateLocation(location, locationName);
        return LocationVerifyResponse.from(member);
    }
}

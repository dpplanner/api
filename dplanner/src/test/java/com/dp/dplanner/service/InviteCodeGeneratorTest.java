package com.dp.dplanner.service;


import com.dp.dplanner.domain.InviteCode;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.adapter.dto.InviteDto;
import com.dp.dplanner.repository.InviteCodeRepository;
import com.dp.dplanner.util.InviteCodeGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class InviteCodeGeneratorTest {

    @Mock
    InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    InviteCodeGenerator inviteCodeGenerator;

    @Test
    @DisplayName("생성된지 7일 이내인 초대코드는 유효하다")
    public void testInviteCodeWithin7Days() {

        Long clubId = 1L;
        Club club = Club.builder().clubName("clubName")
                .build();
        ReflectionTestUtils.setField(club, "id", clubId);

        String code = "valid_code";
        InviteCode inviteCode = InviteCode.builder()
                .code(code)
                .club(club)
                .build();

        ReflectionTestUtils.setField(inviteCode,"createdDate", LocalDateTime.now().minusDays(5)); //5일 지난 코드

        given(inviteCodeRepository.findInviteCodeByCode(code)).willReturn(Optional.of(inviteCode));

        InviteDto inviteDto = inviteCodeGenerator.verify(code);

        assertThat(inviteDto.getVerify()).isTrue();
    }

    @Test
    @DisplayName("생성된지 7일 밖인 초대코드는 유효하지 않다")
    public void testInviteCodeNotWithin7Days() {

        Long clubId = 1L;
        Club club = Club.builder().clubName("clubName")
                .build();
        ReflectionTestUtils.setField(club, "id", clubId);

        String code = "valid_code";
        InviteCode inviteCode = InviteCode.builder()
                .code(code)
                .club(club)
                .build();

        ReflectionTestUtils.setField(inviteCode,"createdDate", LocalDateTime.now().minusDays(14)); //14일 지난 코드

        given(inviteCodeRepository.findInviteCodeByCode(code)).willReturn(Optional.of(inviteCode));

        InviteDto inviteDto = inviteCodeGenerator.verify(code);

        assertThat(inviteDto.getVerify()).isFalse();

    }

    @Test
    @DisplayName("코드를 찾지 못하면 해당 코드는 유요하지 않다")
    public void testInviteCodeNotFound() {

        Long clubId = 1L;
        Club club = Club.builder().clubName("clubName")
                .build();
        ReflectionTestUtils.setField(club, "id", clubId);

        String invalidCode = "invalid";
        given(inviteCodeRepository.findInviteCodeByCode(invalidCode)).willReturn(Optional.empty());

        InviteDto inviteDto = inviteCodeGenerator.verify(invalidCode);

        assertThat(inviteDto.getVerify()).isFalse();
    }


}

package com.dp.dplanner.util;

import com.dp.dplanner.domain.InviteCode;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.repository.InviteCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteCodeGenerator {

    private final InviteCodeRepository inviteCodeRepository;

    public String generateInviteCode(Club club) {

        String inviteCode = UUID.randomUUID().toString();

        inviteCodeRepository.save(
                InviteCode.builder()
                        .club(club)
                        .code(inviteCode)
                        .build()
        );

        return inviteCode;
    }

    public InviteDto verify(String code) {

        Optional<InviteCode> inviteCodeOptional = inviteCodeRepository.findInviteCodeByCode(code);

        if (inviteCodeOptional.isPresent()) {
            InviteCode inviteCode = inviteCodeOptional.get();
            if (inviteCode.getCreatedDate().plusDays(7).isAfter(LocalDateTime.now())) {
                return InviteDto.builder()
                        .verify(true)
                        .inviteCode(code)
                        .clubId(inviteCode.getClub().getId())
                        .build();
            }
        }

        return  InviteDto.builder()
                .verify(false)
                .inviteCode(code)
                .build();
    }

}

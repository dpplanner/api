package com.dp.dplanner.dto;

import com.dp.dplanner.domain.club.Club;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ClubDTO {
    private Long id;
    private String clubName;
    private String info;

    public static ClubDTO of(Club club) {
        return new ClubDTO(
                club.getId(),
                club.getClubName(),
                club.getInfo()
        );
    }
}

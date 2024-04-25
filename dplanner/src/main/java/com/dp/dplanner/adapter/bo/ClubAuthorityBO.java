package com.dp.dplanner.adapter.bo;


import com.dp.dplanner.domain.club.ClubAuthorityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubAuthorityBO {
    private Long id;
    private ClubBO club;
    private String name;
    private String description;
    private List<ClubAuthorityType> clubAuthorityTypes;
}

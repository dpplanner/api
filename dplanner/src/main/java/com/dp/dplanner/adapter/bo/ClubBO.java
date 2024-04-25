package com.dp.dplanner.adapter.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubBO {
    private Long id;
    private String clubName;
    private String info;
    private String url;
    private Long memberCount;
    private Boolean isConfirmed;
}

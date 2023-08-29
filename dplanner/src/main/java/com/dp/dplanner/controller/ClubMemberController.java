package com.dp.dplanner.controller;

import com.dp.dplanner.service.ClubMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/club-members")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;
}

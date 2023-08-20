package com.dp.dplanner.security;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Member member = memberRepository.findByUserName(username).orElseThrow(()->new RuntimeException("사용자를 찾을 수 없습니다."));

        return new PrincipalDetails(member);
    }
}

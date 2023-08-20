package com.dp.dplanner.security;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.dp.dplanner.exception.ErrorResult.MEMBER_NOT_FOUND;


@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Member member = memberRepository.findByUserName(username).orElseThrow(()->new MemberException(MEMBER_NOT_FOUND));

        return new PrincipalDetails(member);
    }
}

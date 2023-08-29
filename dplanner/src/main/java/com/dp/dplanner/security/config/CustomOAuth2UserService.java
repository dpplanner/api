package com.dp.dplanner.security.config;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest,OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(oAuth2UserRequest);

        return process(oAuth2UserRequest, oAuth2User);
    }

    private PrincipalDetails process(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {

        String registrationId = oAuth2UserRequest
                .getClientRegistration()
                .getRegistrationId();

        String userNameAttributeName = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,oAuth2User.getAttributes());

        if (attributes.getEmail().isEmpty() || attributes.getName().isEmpty()) {
            throw new RuntimeException();
//            throw new OAuthProcessingException("Email not found from OAuth2 Provider");
        }

        Optional<Member> optionalMember = memberRepository.findByEmail(attributes.getEmail());
        Member member;
        member = optionalMember.orElseGet(() -> createMember(attributes));

        return PrincipalDetails.create(member, attributes.getAttributes());
    }

    private Member createMember(OAuthAttributes attributes) {

        return memberRepository.save(Member.builder()
                .name(attributes.getName())
                .email(attributes.getEmail())
                .build()
        );
    }


}

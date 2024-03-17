package com.dp.dplanner.config.security;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {
    private Long id;
    private Long clubId;
    private Long clubMemberId;
    private String email;

    private Map<String, Object> attributes;

    public PrincipalDetails(Long id, Long club_id, Long club_member_id, String email, Map<String, Object> attributes) {
        this.id = id;
        this.clubId = club_id;
        this.clubMemberId = club_member_id;
        this.email = email;
        this.attributes = attributes;
    }

    public static PrincipalDetails create(Member member, Club club, ClubMember clubMember, Map<String,Object> attributes) {

        if (club != null && clubMember != null) {
            return new PrincipalDetails(member.getId(), club.getId(), clubMember.getId(), member.getEmail(), attributes);
        }

        return new PrincipalDetails(member.getId(), null, null, member.getEmail(), attributes);
    }


    /**
     * UserDetails
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return null ;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * OAuth2User
     */
    @Override
    public String getName() {
        return String.valueOf(id);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

}

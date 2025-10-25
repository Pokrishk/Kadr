package com.example.Kadr.config;

import com.example.Kadr.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

public class AppUserDetails implements UserDetails {
    private final User user;
    public AppUserDetails(User user) { this.user = user; }

    @Override
    public Set<? extends GrantedAuthority> getAuthorities() {
        String roleName = user.getRole().getTitle();
        String normalized = "ROLE_" + roleName.toUpperCase();
        return Set.of(new SimpleGrantedAuthority(normalized));
    }

    @Override public String getPassword()  { return user.getPasswordHash(); }
    @Override public String getUsername()  { return user.getUsername(); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
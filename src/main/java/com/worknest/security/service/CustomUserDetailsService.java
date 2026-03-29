package com.worknest.security.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO Load the user from persistence (DB/identity provider) and map roles properly.
        throw new UsernameNotFoundException("User lookup not implemented for: " + username);
    }

    public UserDetails placeholderUser(String username) {
        return User.withUsername(username)
                .password("{noop}placeholder")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }
}

package ru.sis.cosplay.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.sis.cosplay.repository.UserRepository;

@Service
@AllArgsConstructor
public class MongoAuthUserDetailService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
    return userRepository
        .findUserByUsername(userName)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}

package com.mention.service;

import com.mention.config.EmailService;
import com.mention.dto.ApiRs;
import com.mention.dto.ChangePasswordRq;
import com.mention.dto.ForgotPasswordRq;
import com.mention.dto.JwtAuthenticationRs;
import com.mention.dto.LoginRq;
import com.mention.dto.UserRq;
import com.mention.model.Profile;
import com.mention.model.User;
import com.mention.model.UserToken;
import com.mention.repository.ProfileRepository;
import com.mention.repository.UserRepository;
import com.mention.repository.UserTokenRepository;
import com.mention.security.JwtTokenProvider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {

  private AuthenticationManager authenticationManager;

  private JwtTokenProvider tokenProvider;

  private UserRepository userRepository;

  private ModelMapper modelMapper;

  private UserTokenRepository tokenRepository;

  private EmailService emailService;

  private ProfileRepository profileRepository;

  @Autowired
  PasswordEncoder passwordEncoder;

  public LoginServiceImpl(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserRepository userRepository,
                          EmailService emailService,
                          UserTokenRepository tokenRepository,
                          ProfileRepository profileRepository) {
    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
    this.userRepository = userRepository;
    this.modelMapper = new ModelMapper();
    this.emailService = emailService;
    this.tokenRepository = tokenRepository;
    this.profileRepository = profileRepository;
  }

  @Override
  public ResponseEntity<?> authenticateUser(LoginRq loginRequest) {
    Optional<User> currentUser = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail(),
        loginRequest.getUsernameOrEmail());
    if (currentUser.isPresent() && !currentUser.get().isActive()) {
      return new ResponseEntity(new ApiRs(false, "Email confirmation required"),
          HttpStatus.BAD_REQUEST);
    }
    if (!currentUser.isPresent()) {
      return new ResponseEntity(new ApiRs(false, "Username or Email not found"),
          HttpStatus.BAD_REQUEST);
    }

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsernameOrEmail(),
            loginRequest.getPassword()
        )
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = tokenProvider.generateToken(authentication, loginRequest.isRemembered());
    return ResponseEntity.ok(new JwtAuthenticationRs(jwt));
  }

  @Override
  public ResponseEntity<?> registerUser(UserRq userDtoRq) {
    Optional<User> usernameCheck = userRepository.findByUsername(userDtoRq.getUsername());
    if (usernameCheck.isPresent()) {
      return new ResponseEntity(new ApiRs(false, "Username is already taken!"),
          HttpStatus.BAD_REQUEST);
    }
    Optional<User> userEmailCheck = userRepository.findByEmail(userDtoRq.getEmail());
    if (userEmailCheck.isPresent()) {
      return new ResponseEntity(new ApiRs(false, "Email Address already in use!"),
          HttpStatus.BAD_REQUEST);
    }

    User newUser = modelMapper.map(userDtoRq, User.class);
    newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
    newUser.setActive(false);
    userRepository.save(newUser);

    String token = UUID.randomUUID().toString();
    UserToken userToken = new UserToken(token, newUser);
    tokenRepository.save(userToken);

    Profile profile = new Profile(newUser);
    profile.setAvatarUrl("https://www.jetphotos.com/assets/img/user.png");
    profileRepository.save(profile);

    String message = "Thank you for registering in Mention! To finish your "
        + "registration, please follow the link: "
        + "http://localhost:3000/registration/" + token;
    String to = newUser.getEmail();
    String subject = "Confirm your email";
    emailService.sendSimpleMessage(to, subject, message);

    return ResponseEntity.ok(new ApiRs(true,
        "User registered successfully! Check your inbox for confirmation!"));
  }

  @Override
  public ResponseEntity<?> confirmRegistration(String token) {
    Optional<UserToken> userToken = tokenRepository.findByToken(token);
    if (userToken.isPresent()) {
      User user = userRepository.findById(userToken.get().getUser().getId()).get();
      user.setActive(true);
      userRepository.save(user);
      tokenRepository.delete(userToken.get());
      return ResponseEntity.ok(new ApiRs(true,
          "User email confirmed successfully"));
    }
    return new  ResponseEntity(new ApiRs(false, "Token not found"),
        HttpStatus.BAD_REQUEST);
  }

  @Override
  public ResponseEntity<?> recoverPassword(ForgotPasswordRq forgotPasswordRq) {
    Optional<User> userEmailCheck = userRepository.findByEmail(forgotPasswordRq.getEmail());
    if (!userEmailCheck.isPresent()) {
      return new ResponseEntity(new ApiRs(false,
          "User with provided email not found"), HttpStatus.NOT_FOUND);
    }
    User user = userEmailCheck.get();
    String token = UUID.randomUUID().toString();
    UserToken userToken = new UserToken(token, user);
    tokenRepository.save(userToken);

    String message = "Hi, " + user.getUsername()
        + ". You recently requested to reset your password for Mention. "
        + "Follow the link to reset it: "
        + "http://localhost:3000/registration/recover/" + token
        + "\n\nIf you received this email by mistake, please, ignore it."
        + "\n\nSincerely yours,\nMention team";
    String to = user.getEmail();
    String subject = "Forgotten password recovery";
    emailService.sendSimpleMessage(to, subject, message);

    return ResponseEntity.ok(new ApiRs(true, "An email with instructions was sent. "
        + "Please, check your inbox for confirmation!"
    ));
  }

  @Override
  public ResponseEntity<?> changePassword(String token, ChangePasswordRq changePasswordRq) {
    Optional<UserToken> userToken = tokenRepository.findByToken(token);
    if (!userToken.isPresent()) {
      return new  ResponseEntity(new ApiRs(false, "Token not found"),
          HttpStatus.BAD_REQUEST);
    }
    User user = userRepository.findById(userToken.get().getUser().getId()).get();
    user.setPassword(passwordEncoder.encode(changePasswordRq.getPassword()));
    userRepository.save(user);
    tokenRepository.delete(userToken.get());
    return ResponseEntity.ok(new ApiRs(true,
        "Password changed successfully"));
  }
}

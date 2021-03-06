package com.mention.controller;

import com.mention.dto.CurrentUserRs;
import com.mention.dto.ShortUserDetailsRs;
import com.mention.dto.UserIdRq;
import com.mention.dto.UserRq;
import com.mention.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private UserServiceImpl userService;

  @Autowired
  public UserController(UserServiceImpl userService) {
    this.userService = userService;
  }

  @GetMapping("/{username}")
  public ShortUserDetailsRs getUser(@PathVariable String username) {
    return userService.getUser(username);
  }

  @GetMapping("/search/{username}")
  public List<ShortUserDetailsRs> getUsersByUsername(@PathVariable String username) {
    return userService.getUsersByUsername(username.replace("%20", " "));
  }

  @GetMapping("/current")
  public CurrentUserRs getCurrentUser() {
    return userService.getCurrentUser();
  }

  @PostMapping("/add")
  public void createUser(@Valid @RequestBody UserRq userDtoNewUser) {
    userService.createNewUser(userDtoNewUser);
  }

  @DeleteMapping("/delete")
  public ResponseEntity<?> deleteUser(@RequestBody UserIdRq userIdRq) {
    return userService.deleteUser(userIdRq);
  }
}

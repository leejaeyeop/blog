package com.springboot.blog.controller.rest;

import com.springboot.blog.entity.User;
import com.springboot.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping(value = "/users", produces = MediaTypes.HAL_JSON_VALUE)
@RestController
public class UserRestController {

    private final UserService userService;

    @Autowired
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<String> join(@RequestBody User newUser) {
        userService.join(formValidation(newUser));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<String> modify(@PathVariable Long id, @RequestBody User newUser, @RequestPart MultipartFile newPhoto) {
        userService.modify(id, formValidation(newUser), newPhoto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @DeleteMapping(value = "/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 입력 값 유효성 검사
     *
     * @param newUser
     * @return
     */
    private User formValidation(User newUser) {
        if (newUser.getUsername().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be <strong>null</strong>.");
        }

        if (newUser.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be <strong>null</strong>.");
        }

//        if (newUser.getEmail().isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be <strong>null</strong>.");
//        }

        if (newUser.getName().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be <strong>null</strong>.");
        }

        return newUser;
    }

}

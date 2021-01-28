package com.springboot.blog.service;

import com.springboot.blog.entity.Role;
import com.springboot.blog.entity.User;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.util.AmazonService;
import com.springboot.blog.util.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService implements OAuth2UserService {

    public static final String LOCAL = "Local";
    private final String USER_NOT_FOUND_MESSAGE = "Not found user.";
    private final UserRepository userRepository;
    private final AmazonService amazonService;
    private final String defaultPicture;

    private final MailService mailService;

    @Autowired
    public UserService(UserRepository userRepository, AmazonService amazonService, @Value("${cloud.aws.s3.default-picture}") String defaultPicture, MailService mailService) {
        this.userRepository = userRepository;
        this.amazonService = amazonService;
        this.defaultPicture = defaultPicture;
        this.mailService = mailService;
    }

    /**
     * OAuth2
     * 회원 가입 & 회원 로그인
     *
     * @param userRequest
     * @return
     * @throws OAuth2AuthenticationException
     */
    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        mailService.sendMail("kj9772@naver.com", "text", "sub");

        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String authId = oAuth2User.getName();
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        return userRepository.findByAuthIdAndRegistrationId(authId, registrationId).orElseGet(() -> {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = null;

            switch (registrationId) {
                case "google":
                    picture = oAuth2User.getAttribute("picture");
                    break;
                case "github":
                    picture = oAuth2User.getAttribute("avatar_url");
                    break;
                case "facebook":
                    picture = String.format("https://graph.facebook.com/%s/picture?type=normal", authId);
                    break;
            }

            return userRepository.save(User.builder()
                    .authId(authId)
                    .registrationId(registrationId)
                    .role(Role.ROLE_USER)
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .build());
        });
    }


//    @Transactional
//    public void sendEmail(String email) {
//        return userRepository.findByEmail(email).orElseGet(() ->
//                userRepository.save(User.builder()
//                        .authId(String.valueOf(UUID.randomUUID()))
//                        .registrationId(LOCAL)
//                        .role(Role.ROLE_USER)
//                        .email(email)
//                        .name(email.split("@")[0])
//                        .picture(defaultPicture)
//                        .build()));
//    }

//    @Transactional
//    public User userAuthentication(String authId) {
//
//    }


    /**
     * 회원 수정
     *
     * @param id
     * @param newUser
     * @param newPicture
     * @return
     */
    @Transactional
    public User modify(Long id, User newUser, MultipartFile newPicture) {
        return userRepository.findById(id)
                .map(findUser -> {
                    findUser.setName(newUser.getName());
                    findUser.setIntroduce(newUser.getIntroduce());

                    if (newPicture != null) {
                        if (findUser.getPicture() != null) {
                            amazonService.deleteImage(findUser.getPicture());
                        }
                        findUser.setPicture(amazonService.putImage(newPicture));
                    }

                    return findUser;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }

    /**
     * 회원 삭제
     *
     * @param id
     * @return
     */
    @Transactional
    public void delete(Long id) {
        userRepository.findById(id)
                .map(findUser -> {
                    if (findUser.getPicture() != null) {
                        amazonService.deleteImage(findUser.getPicture());
                    }

                    findUser.getBoards()
                            .stream()
                            .filter(board -> board.getImage() != null)
                            .forEach(board -> amazonService.deleteImage(board.getImage()));

                    userRepository.deleteById(findUser.getId());

                    return findUser;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND_MESSAGE));
    }

}



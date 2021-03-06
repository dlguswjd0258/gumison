package com.ssafy.gumison.api.service;

import com.ssafy.gumison.redis.RankProvider;
import com.ssafy.gumison.security.UserPrincipal;
import com.ssafy.gumison.common.exception.ResourceNotFoundException;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ssafy.gumison.api.response.UserSearchRes;
import com.ssafy.gumison.common.dto.UserOauthDto;
import com.ssafy.gumison.common.dto.UserSearchDto;
import com.ssafy.gumison.db.entity.CommonCode;
import com.ssafy.gumison.db.entity.Solution;
import com.ssafy.gumison.db.entity.User;
import com.ssafy.gumison.db.repository.CommonCodeRepository;
import com.ssafy.gumison.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service("UserService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final CommonCodeRepository commonCodeRepository;
  private final HttpSession httpSession;
  private final RankProvider rankProvider;

  @Override
  public UserSearchRes getUserList(String nickname, int pageNumber) {
    PageRequest page = PageRequest.of(pageNumber, 10, Sort.by(Sort.Direction.ASC, "nickname"));
    List<User> userList = userRepository.findByNicknameContaining(nickname, page);

    UserSearchRes userSearchRes = new UserSearchRes();
    for (User user : userList) {
      userSearchRes.getUsers().add(getUserSearchDtoByUser(user));
    }

    log.info("Set user search response: {}", userSearchRes);

    return userSearchRes;
  }

  @Override
  public UserOauthDto getOauthUserByOauthId(String oauthId) {
    User user = userRepository.findByOauthId(oauthId)
        .orElseThrow(
            () -> new UsernameNotFoundException("User not found with oauthId : " + oauthId));
    UserOauthDto userOauthDto = UserOauthDto.builder()
        .nickname(user.getNickname())
        .description(user.getDescription())
        .profile(user.getProfile())
        .oauthId(user.getOauthId())
        .build();
    log.info("getOauthUserByOauthId: {}", user);
    return userOauthDto;
  }


  @Override
  public UserDetails loadUserByOauthId(String oauthId) {
    User user = userRepository.findByOauthId(oauthId)
        .orElseThrow(
            () -> new UsernameNotFoundException("User not found with oauthId : " + oauthId));

    log.info("loadUserByOauthId: {}", user);

    return UserPrincipal.create(user);
  }

  /**
   * ?????? ??????????????? UserSearchDto ??????.
   *
   * @param nickname ????????? ?????????
   * @return ?????? ?????? ??? ?????????, ?????????, ????????????, ?????? ?????? ?????? ??????
   */
  @Override
  public UserSearchDto getUserSearchDtoByNickname(String nickname) {
    User user = userRepository.findByNickname(nickname)
        .orElseThrow(() -> new ResourceNotFoundException("User", nickname, "nickname"));
    return getUserSearchDtoByUser(user);
  }

  /**
   * ???????????? ????????? ????????? ?????? ?????? ??????.
   *
   * @param keyword ?????? ?????????
   * @return ?????? ???????????? ????????? ????????? ??? ?????????
   */
  @Override
  public Long getUserCountByKeyword(String keyword) {
    return userRepository.countByNicknameContaining(keyword);
  }


  /**
   * ?????? ????????? UserSearchDto ??????.
   *
   * @param user ?????? ??????
   * @return ?????? ?????? ??? ?????????, ?????????, ????????????, ?????? ?????? ?????? ??????
   * @throws RuntimeException ?????? ????????? ??????????????? ????????? ?????? CommonCode??? ?????? ??????
   */
  private UserSearchDto getUserSearchDtoByUser(User user) {
    CommonCode code = commonCodeRepository.findById(user.getTierCode())
        .orElseThrow(RuntimeException::new);

    long solvedCount = 0;

    for (Solution solution : user.getSolutionList()) {
      solvedCount += solution.getCount();
    }

    return UserSearchDto.builder()
        .nickname(user.getNickname())
        .profile(user.getProfile())
        .tier(code.getName().toLowerCase())
        .solCnt(solvedCount)
        .build();
  }


  @Override
  public UserOauthDto updateUserByOauthId(String oauthId, UserOauthDto userOauthDto) {

    User user = userRepository.findByOauthId(oauthId)
        .orElseThrow(() -> new ResourceNotFoundException("User", oauthId, "oauthId"));
    try {
      user.setNickname(userOauthDto.getNickname());
      user.setDescription(userOauthDto.getDescription());
      user.setProfile(userOauthDto.getProfile());

      userRepository.save(user);

    } catch (Exception e) {
      log.error("[updateUserByOAuthId] ", e);
    }
    rankProvider.loadAllUserExpIntoRankZSet();

    return UserOauthDto.builder()
        .nickname(user.getNickname())
        .description(user.getDescription())
        .profile(user.getProfile())
        .oauthId(user.getOauthId())
        .build();


  }

  @Override
  public void deleteUserByOauthId(String oauthId) {
    User user = userRepository.findByOauthId(oauthId)
        .orElseThrow(() -> new ResourceNotFoundException("User", oauthId, "oauthId"));
    userRepository.delete(user);
  }

  @Override
  public String updateProfileByOauthId(String oauthId, MultipartFile file) {

    userRepository.updateProfileByOauthId(oauthId, "test");
    User user = userRepository.findByOauthId(oauthId)
        .orElseThrow(() -> new ResourceNotFoundException("User", oauthId, "oauthId"));
    return user.getProfile();
  }

}





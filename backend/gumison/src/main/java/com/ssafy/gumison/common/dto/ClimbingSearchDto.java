package com.ssafy.gumison.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@ToString
public class ClimbingSearchDto {

  private String climbingName;

  private String address;

  private String phoneNumber;

  private String imgUrl;

}

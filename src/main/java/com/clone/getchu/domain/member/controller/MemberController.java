package com.clone.getchu.domain.member.controller;

import com.clone.getchu.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members/me")
public class MemberController {
    private final MemberService memberService;
}

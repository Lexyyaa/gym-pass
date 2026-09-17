package com.gym.pass.application.member;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberCommand {

    public record Register(String name, String phone) {}
}

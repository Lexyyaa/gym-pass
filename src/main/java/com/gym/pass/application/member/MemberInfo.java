package com.gym.pass.application.member;

import com.gym.pass.domain.member.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberInfo {

    public record Registered(Long memberId, String name, String phone) {

        public static Registered from(Member member) {
            return new Registered(
                    member.getId(), member.getName(), member.getPhone().getValue());
        }
    }
}

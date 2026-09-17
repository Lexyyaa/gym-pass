package com.gym.pass.presentation.member;

import com.gym.pass.application.member.MemberInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberResponse {

    public record Registered(Long memberId, String name, String phone) {

        public static Registered from(MemberInfo.Registered info) {
            return new Registered(info.memberId(), info.name(), info.phone());
        }
    }
}

package com.gym.pass.presentation.member;

import com.gym.pass.application.member.MemberCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberRequest {

    public record Register(
            @NotBlank @Size(max = 50) String name,

            @NotBlank @Pattern(regexp = "^01\\d-\\d{3,4}-\\d{4}$")
            String phone) {

        public MemberCommand.Register toCommand() {
            return new MemberCommand.Register(name, phone);
        }
    }
}

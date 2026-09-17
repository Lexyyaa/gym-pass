package com.gym.pass.domain.branch;

import com.gym.pass.domain.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 지점. seed로만 생성되고 지점 격리 판정의 기준이 된다 (03 §3.1). */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}

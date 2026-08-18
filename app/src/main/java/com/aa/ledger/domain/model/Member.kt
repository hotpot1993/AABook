package com.aa.ledger.domain.model

import com.aa.ledger.data.local.entity.MemberEntity

data class Member(
    val id: Long = 0,
    val ledgerId: Long,
    val name: String,
    val nickname: String = "",
    val avatarUri: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
)

fun MemberEntity.toMember() = Member(
    id = id,
    ledgerId = ledgerId,
    name = name,
    nickname = nickname,
    avatarUri = avatarUri,
    joinedAt = joinedAt
)

fun Member.toEntity() = MemberEntity(
    id = id,
    ledgerId = ledgerId,
    name = name,
    nickname = nickname,
    avatarUri = avatarUri,
    joinedAt = joinedAt
)

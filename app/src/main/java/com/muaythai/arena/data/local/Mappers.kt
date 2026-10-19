package com.muaythai.arena.data.local

import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.ChallengeRecord
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.Fighter
import com.muaythai.arena.domain.model.OpponentRecord
import com.muaythai.arena.domain.model.PersonalRecord
import com.muaythai.arena.domain.model.RecordKey
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.Strike
import com.muaythai.arena.domain.model.TrainingSession
import com.muaythai.arena.domain.model.encodeGains
import com.muaythai.arena.domain.model.parseGains

private const val LIST_SEPARATOR = ","
private const val LINE_SEPARATOR = "\n"

private fun String.splitList(separator: String = LIST_SEPARATOR): List<String> =
    if (isEmpty()) emptyList() else split(separator)

// ---- fighter

fun FighterEntity.toDomain(): Fighter = Fighter(
    name = name,
    style = Archetype.entries.firstOrNull { it.name == style } ?: Archetype.TECHNICAL,
    xp = xp, coins = coins, rankPoints = rankPoints, badges = badges, wins = wins, losses = losses, draws = draws,
    winStreak = winStreak, bestWinStreak = bestWinStreak, campsCompleted = campsCompleted, createdAt = createdAt
)

fun Fighter.toEntity(): FighterEntity = FighterEntity(
    id = 1, name = name, style = style.name, xp = xp, coins = coins, rankPoints = rankPoints, badges = badges,
    wins = wins, losses = losses, draws = draws, winStreak = winStreak, bestWinStreak = bestWinStreak,
    campsCompleted = campsCompleted, createdAt = createdAt
)

// ---- sessions

fun TrainingSessionEntity.toDomain(): TrainingSession = TrainingSession(
    id = id, timestamp = timestamp, epochDay = epochDay,
    type = SessionType.entries.firstOrNull { it.name == type } ?: SessionType.TECHNIQUE,
    title = title, durationSec = durationSec, xp = xp, performance = performance, reps = reps, correct = correct,
    total = total, tag = tag, success = success, valueMs = valueMs, attributeGains = attributeGains, note = note
)

fun TrainingSession.toEntity(): TrainingSessionEntity = TrainingSessionEntity(
    id = id, timestamp = timestamp, epochDay = epochDay, type = type.name, title = title, durationSec = durationSec,
    xp = xp, performance = performance, reps = reps, correct = correct, total = total, tag = tag, success = success,
    valueMs = valueMs, attributeGains = attributeGains, note = note
)

// ---- combinations

fun CombinationEntity.toDomain(): Combination = Combination(
    id = id, name = name,
    strikes = strikes.splitList().mapNotNull { raw -> Strike.entries.firstOrNull { it.name == raw } },
    favorite = favorite, reps = reps, custom = custom, milestonesClaimed = milestonesClaimed, createdAt = createdAt
)

fun Combination.toEntity(): CombinationEntity = CombinationEntity(
    id = id, name = name, strikes = strikes.joinToString(LIST_SEPARATOR) { it.name }, favorite = favorite, reps = reps,
    custom = custom, milestonesClaimed = milestonesClaimed, createdAt = createdAt
)

// ---- fights

fun FightResultEntity.toDomain(): FightResult = FightResult(
    id = id, timestamp = timestamp, opponentId = opponentId, opponentName = opponentName, opponentTier = opponentTier,
    won = won, draw = draw, method = method, endRound = endRound,
    scorecards = scorecards.splitList("|"), roundResults = roundResults.splitList(),
    striking = striking, defense = defense, kicking = kicking, clinch = clinch, fightIq = fightIq, stamina = stamina,
    tactical = tactical, overall = overall, xp = xp,
    mainStrategy = Strategy.entries.firstOrNull { it.name == mainStrategy } ?: Strategy.BALANCED,
    campFight = campFight
)

fun FightResult.toEntity(): FightResultEntity = FightResultEntity(
    id = id, timestamp = timestamp, opponentId = opponentId, opponentName = opponentName, opponentTier = opponentTier,
    won = won, draw = draw, method = method, endRound = endRound,
    scorecards = scorecards.joinToString("|"), roundResults = roundResults.joinToString(LIST_SEPARATOR),
    striking = striking, defense = defense, kicking = kicking, clinch = clinch, fightIq = fightIq, stamina = stamina,
    tactical = tactical, overall = overall, xp = xp, mainStrategy = mainStrategy.name, campFight = campFight
)

// ---- opponents

fun OpponentEntity.toDomain(): OpponentRecord = OpponentRecord(opponentId, wins, losses, lastFought)

fun OpponentRecord.toEntity(): OpponentEntity {
    val opponent = Opponents.get(opponentId)
    return OpponentEntity(opponentId, opponent?.name ?: "Unknown", opponent?.tier ?: 1, wins, losses, lastFought)
}

// ---- challenges and records

fun ChallengeEntity.toDomain(): ChallengeRecord = ChallengeRecord(challengeKey, templateId, weekly, periodKey, completedAt)

fun ChallengeRecord.toEntity(): ChallengeEntity = ChallengeEntity(key, templateId, weekly, periodKey, completedAt)

fun PersonalRecordEntity.toDomain(): PersonalRecord? {
    val key = RecordKey.entries.firstOrNull { it.name == recordKey } ?: return null
    return PersonalRecord(key, value, detail, achievedAt)
}

fun PersonalRecord.toEntity(): PersonalRecordEntity = PersonalRecordEntity(key.name, value, detail, achievedAt)

// ---- camp

fun FightCampEntity.toDomain(): FightCamp = FightCamp(
    active = active, day = day, energy = energy, freshness = freshness, opponentId = opponentId,
    bonuses = parseGains(bonuses), log = campLog.splitList(LINE_SEPARATOR), boughtEnergyToday = boughtEnergyToday,
    trainedToday = trainedToday.splitList().mapNotNull { raw -> CampTraining.entries.firstOrNull { it.name == raw } }
)

fun FightCamp.toEntity(): FightCampEntity = FightCampEntity(
    id = 1, active = active, day = day, energy = energy, freshness = freshness, opponentId = opponentId,
    bonuses = encodeGains(bonuses), campLog = log.joinToString(LINE_SEPARATOR), boughtEnergyToday = boughtEnergyToday,
    trainedToday = trainedToday.joinToString(LIST_SEPARATOR) { it.name }
)

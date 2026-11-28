package com.piecer.app.util.quickentry

data class VoiceTranscriptSelection(
    val transcript: String,
    val parseResult: QuickEntryParseResult
)

/**
 * Memilih kandidat transkrip suara terbaik (maksimal 3) berdasarkan hasil parse G1:
 * memprioritaskan kandidat yang memiliki nominal valid dan confidence tertinggi.
 */
object VoiceTranscriptSelector {

    fun selectBest(
        candidates: List<String>,
        context: QuickEntryContext = QuickEntryContext(),
        maxCandidates: Int = 3
    ): VoiceTranscriptSelection? {
        val cleaned = candidates
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(maxCandidates)
            .toList()

        if (cleaned.isEmpty()) return null

        var bestSelection: VoiceTranscriptSelection? = null
        var bestScore: CandidateScore? = null

        cleaned.forEachIndexed { index, text ->
            val result = QuickEntryPipeline.parse(text, context)
            val score = scoreResult(result, index)
            if (bestScore == null || score.isBetterThan(bestScore!!)) {
                bestScore = score
                bestSelection = VoiceTranscriptSelection(
                    transcript = text,
                    parseResult = result
                )
            }
        }

        return bestSelection
    }

    private fun scoreResult(result: QuickEntryParseResult, index: Int): CandidateScore {
        val entriesWithAmount = result.entries.count { (it.amount ?: 0L) > 0L }
        val hasNominal = entriesWithAmount > 0
        val maxConfidence = result.entries.maxOfOrNull { it.confidence } ?: 0f
        val avgConfidence = if (result.entries.isEmpty()) {
            0f
        } else {
            result.entries.map { it.confidence }.average().toFloat()
        }
        return CandidateScore(
            hasNominal = hasNominal,
            maxConfidence = maxConfidence,
            avgConfidence = avgConfidence,
            entriesWithAmount = entriesWithAmount,
            index = index
        )
    }

    private data class CandidateScore(
        val hasNominal: Boolean,
        val maxConfidence: Float,
        val avgConfidence: Float,
        val entriesWithAmount: Int,
        val index: Int
    ) {
        fun isBetterThan(other: CandidateScore): Boolean {
            if (hasNominal != other.hasNominal) return hasNominal
            if (maxConfidence != other.maxConfidence) return maxConfidence > other.maxConfidence
            if (avgConfidence != other.avgConfidence) return avgConfidence > other.avgConfidence
            if (entriesWithAmount != other.entriesWithAmount) return entriesWithAmount > other.entriesWithAmount
            return index < other.index
        }
    }
}

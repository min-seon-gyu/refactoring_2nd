package chapter1.calculator

import chapter1.Performance
import chapter1.Play
import kotlin.math.floor
import kotlin.math.max

open class PerformanceCalculator(
    var performance: Performance,
    var play: Play
) {

    companion object {
        fun createPerformanceCalculator(perf: Performance, play: Play): PerformanceCalculator {
            return when (play.type) {
                "tragedy" -> TragedyCalculator(perf, play)
                "comedy"-> ComedyCalculator(perf, play)
                else -> throw IllegalArgumentException("알 수 없는 장르: ${play.type}")
            }
        }
    }

    open fun amount(): Int {
        var result = 0

        when (play.type) {
            // 비극
            "tragedy" -> throw IllegalArgumentException("오류 발생")

            // 희극
            "comedy" -> {
                result = 30000
                if (performance.audience > 20) {
                    result += 10000 + 500 * (performance.audience - 20)
                }
                result += 300 * performance.audience
            }

            else -> throw IllegalArgumentException("알 수 없는 장르: ${play.type}")
        }

        return result
    }

    open fun volumeCredits(): Int {
        var result = 0

        result += max(performance.audience - 30, 0)

        if ("comedy" == play.type) {
            result += floor(performance.audience.toDouble() / 5).toInt()
        }

        return result
    }
}

class TragedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play) {

    override fun amount(): Int {
        var result = 40000

        if (performance.audience > 30) {
            result += 1000 * (performance.audience - 30)
        }

        return result
    }
}

class ComedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play)

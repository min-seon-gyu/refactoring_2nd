package chapter1

import kotlin.math.floor
import kotlin.math.max

class StatementData(
    var invoice: Invoice,
    var plays: Plays
) {

    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }

    fun amountFor(perf: Performance): Int {
        return PerformanceCalculator(perf, playFor(perf)).amount()
    }

    fun volumeCreditsFor(perf: Performance): Int {
        var result = 0

        result += max(perf.audience - 30, 0)

        if ("comedy" == playFor(perf).type) {
            result += floor(perf.audience.toDouble() / 5).toInt()
        }

        return result
    }

    fun totalVolumeCredits(): Int {
        return invoice.performances.map { volumeCreditsFor(it) }.reduce {
            total, volumeCredits -> total + volumeCredits
        }
    }

    fun totalAmount(): Int {
        return invoice.performances.map { amountFor(it) }.reduce {
                total, amount -> total + amount
        }
    }
}

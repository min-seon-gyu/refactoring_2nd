package chapter1

import chapter1.calculator.PerformanceCalculator.Companion.createPerformanceCalculator

class StatementData(
    var invoice: Invoice,
    var plays: Plays
) {

    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }

    fun amountFor(perf: Performance): Int {
        return createPerformanceCalculator(perf, playFor(perf)).amount()
    }

    fun volumeCreditsFor(perf: Performance): Int {
        return createPerformanceCalculator(perf, playFor(perf)).volumeCredits()
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

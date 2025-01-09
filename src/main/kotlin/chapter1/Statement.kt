package chapter1

import java.text.NumberFormat
import java.util.*;
import kotlin.math.floor
import kotlin.math.max

fun statement(invoice: Invoice, plays: Plays): String {

    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }

    fun amountFor(perf: Performance): Int {
        var result = 0

        when (playFor(perf).type) {
            "tragedy" -> {
                result = 40000
                if (perf.audience > 30) {
                    result += 1000 * (perf.audience - 30)
                }
            }

            "comedy" -> {
                result = 30000
                if (perf.audience > 20) {
                    result += 10000 + 500 * (perf.audience - 20)
                }
                result += 300 * perf.audience
            }

            else -> throw IllegalArgumentException("알 수 없는 장르: ${playFor(perf).type}")
        }

        return result
    }

    fun volumeCreditsFor(perf: Performance): Int {
        var result = 0

        result += max(perf.audience - 30, 0)

        if ("comedy" == playFor(perf).type) {
            result += floor(perf.audience.toDouble() / 5).toInt()
        }

        return result
    }

    fun usd(number: Int): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(number / 100.0)
    }

    var totalAmount = 0
    var volumeCredits = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"

    for (perf in invoice.performances) {
        result += "  ${playFor(perf).name}: $${usd(amountFor(perf))} (${perf.audience}석)\n"
        totalAmount += amountFor(perf)
    }

    for (perf in invoice.performances) {
        volumeCredits += volumeCreditsFor(perf)
    }

    result += "총액: $${usd(totalAmount)}\n"
    result += "적립 포인트: $volumeCredits 점\n"

    return result
}

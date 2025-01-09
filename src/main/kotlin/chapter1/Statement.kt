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

    var totalAmount = 0
    var volumeCredits = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"
    val format = NumberFormat.getCurrencyInstance(Locale.US)

    for(perf in invoice.performances) {
        volumeCredits += max(perf.audience - 30, 0)
        if("comedy" == playFor(perf).type) volumeCredits += floor(perf.audience.toDouble() / 5).toInt()

        result += "  ${playFor(perf).name}: $${format.format(amountFor(perf) / 100.0)} (${perf.audience}석)\n"
        totalAmount += amountFor(perf)
    }

    result += "총액: $${format.format(totalAmount / 100.0)}\n"
    result += "적립 포인트: $volumeCredits 점\n"

    return result
}

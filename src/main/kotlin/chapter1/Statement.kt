package chapter1

import java.text.NumberFormat
import java.util.*;
import kotlin.math.floor
import kotlin.math.max

fun statement(invoice: Invoice, plays: Plays): String {
    var totalAmount = 0
    var volumeCredits = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"
    val format = NumberFormat.getCurrencyInstance(Locale.US)

    for(perf in invoice.performances) {
        val play = plays.play[perf.playId]!!
        val thisAmount = amountFor(play, perf)

        volumeCredits += max(perf.audience - 30, 0)
        if("comedy" == play.type) volumeCredits += floor(perf.audience.toDouble() / 5).toInt()

        result += "  ${play.name}: $${format.format(thisAmount / 100.0)} (${perf.audience}석)\n"
        totalAmount += thisAmount
    }

    result += "총액: $${format.format(totalAmount / 100.0)}\n"
    result += "적립 포인트: $volumeCredits 점\n"
    return result;
}

private fun amountFor(play: Play, perf: Performance): Int {
    var result = 0

    when (play.type) {
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

        else -> throw IllegalArgumentException("알 수 없는 장르: ${play.type}")
    }
    return result
}

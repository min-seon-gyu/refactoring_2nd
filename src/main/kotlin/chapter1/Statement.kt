package chapter1

import java.text.NumberFormat
import java.util.*

fun statement(invoice: Invoice, plays: Plays): String {
    val statementData = StatementData(invoice, plays)
    return renderPlainText(statementData)
}

fun renderPlainText(statementData: StatementData): String {
    fun usd(number: Int): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(number / 100.0)
    }

    var result = "청구 내역 (고객명: ${statementData.invoice.customer})\n"

    for (perf in statementData.invoice.performances) {
        result += "  ${statementData.playFor(perf).name}: $${usd(statementData.amountFor(perf))} (${perf.audience}석)\n"
    }

    result += "총액: $${usd(statementData.totalAmount())}\n"
    result += "적립 포인트: ${statementData.totalVolumeCredits()} 점\n"

    return result
}

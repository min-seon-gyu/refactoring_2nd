package chapter1

import java.text.NumberFormat
import java.util.*

fun statement(invoice: Invoice, plays: Plays): String {
    return renderPlainText(StatementData(invoice, plays))
}

fun renderPlainText(statementData: StatementData): String {
    var result = "청구 내역 (고객명: ${statementData.invoice.customer})\n"

    for (perf in statementData.invoice.performances) {
        result += "  ${statementData.playFor(perf).name}: $${usd(statementData.amountFor(perf))} (${perf.audience}석)\n"
    }

    result += "총액: $${usd(statementData.totalAmount())}\n"
    result += "적립 포인트: ${statementData.totalVolumeCredits()} 점\n"

    return result
}

fun htmlStatement(invoice: Invoice, plays: Plays): String {
    return renderHtml(StatementData(invoice, plays))
}

fun renderHtml(statementData: StatementData): String {
    var result = "<h1>청구 내역 (고객명: ${statementData.invoice.customer})</h1>\n"

    result += "<table>\n"
    result += "<tr><th>연극</th><th>좌석 수</th><th>금액</th></tr>\n"

    for (perf in statementData.invoice.performances) {
        result += "  <tr><td>${statementData.playFor(perf).name}</td><td>(${perf.audience}석)</td>"
        result += "<td>$${usd(statementData.amountFor(perf))}</td></tr>\n"
    }

    result += "</table>\n"

    result += "<p>총액: <em>$${usd(statementData.totalAmount())}</em></p>\n"
    result += "<p>적립 포인트: <em>${statementData.totalVolumeCredits()}</em>점</p>\n"

    return result
}

fun usd(number: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(number / 100.0)
}

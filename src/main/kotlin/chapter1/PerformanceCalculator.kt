package chapter1

class PerformanceCalculator(
    var performance: Performance,
    var play: Play
) {

    fun amount(): Int {
        var result = 0

        when (play.type) {
            // 비극
            "tragedy" -> {
                result = 40000
                if (performance.audience > 30) {
                    result += 1000 * (performance.audience - 30)
                }
            }

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
}

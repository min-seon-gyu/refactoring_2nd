package chapter4

import kotlinx.serialization.Serializable

@Serializable
data class Province(
    val name: String,
    val producers: MutableList<Producer>,
    var demand: Int,
    var price: Int
) {

    // 생산 부족분
    val shortFall: Int
        get() = this.demand - this.totalProduction

    // 총수익 계산 로직
    val profit: Int
        get() = this.demandValue - this.demandCost

    // 수요에 대한 가치
    val demandValue: Int
        get() = this.satisfiedDemand * this.price

    // 수요를 충족시키는 생산량
    val satisfiedDemand: Int
        get() = this.demand.coerceAtMost(totalProduction)

    // 수요에 대한 비용
    val demandCost: Int
        get() {
            var remainingDemand = this.demand
            var result = 0
            producers.sortBy { it.cost }
            producers.forEach { producer ->
                val contribution = remainingDemand.coerceAtMost(producer.production)
                remainingDemand -= contribution
                result += contribution * producer.cost
            }
            return result
        }

    // 총 생산량
    val totalProduction: Int
        get() = producers.sumOf { it.production }
}
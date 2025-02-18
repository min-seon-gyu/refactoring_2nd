package chapter4

import kotlinx.serialization.Serializable

@Serializable
data class Producer(
    val name: String,
    var cost: Int,
    var production: Int
)
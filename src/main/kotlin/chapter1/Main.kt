package chapter1

fun main() {
    println(
        statement(
            Invoice(
                customer = "BigCo",
                performances = listOf(
                    Performance("hamlet", 55),
                    Performance("as-like", 35),
                    Performance("othello", 40)
                )
            ), Plays(
                play = mapOf(
                    "hamlet" to Play("Hamlet", "tragedy"),
                    "as-like" to Play("As You Like It", "comedy"),
                    "othello" to Play("Othello", "tragedy")
                )
            )
        )
    )
}

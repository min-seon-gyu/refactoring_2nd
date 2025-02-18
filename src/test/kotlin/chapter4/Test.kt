package chapter4


import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Test {

    lateinit var asia: Province
    val json = Json { ignoreUnknownKeys = true }
    val provinceJson = """
        {
          "name": "Asia",
          "producers": [
            {"name": "Byzantium", "cost": 10, "production": 9},
            {"name": "Attalia", "cost": 12, "production": 10},
            {"name": "Sinope", "cost": 10, "production": 6}
          ],
          "demand": 30,
          "price": 20
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        asia = json.decodeFromString(provinceJson)
    }

    @Test
    fun `생산 부족분`() {
        Assertions.assertThat(asia.shortFall).isEqualTo(5)
    }

    @Test
    fun `총수익 계산 로직`() {
        Assertions.assertThat(asia.profit).isEqualTo(230)
    }

    @Test
    fun `생산량 변경`() {
        asia.producers[0].production = 20

        Assertions.assertThat(asia.shortFall).isEqualTo(-6)
        Assertions.assertThat(asia.profit).isEqualTo(292)
    }

    @Test
    fun `생산량 없는 경우`() {
        asia.producers.clear()

        Assertions.assertThat(asia.shortFall).isEqualTo(30)
        Assertions.assertThat(asia.profit).isEqualTo(0)
    }

    @Test
    fun `수요가 0일 때`() {
        asia.demand = 0

        Assertions.assertThat(asia.demand).isEqualTo(0)
        Assertions.assertThat(asia.shortFall).isEqualTo(-25)
        Assertions.assertThat(asia.profit).isEqualTo(0)
    }

    @Test
    fun `수요가 음수 일 때`() {
        asia.demand = -1

        Assertions.assertThat(asia.demand).isEqualTo(-1)
        Assertions.assertThat(asia.shortFall).isEqualTo(-26)
        Assertions.assertThat(asia.profit).isEqualTo(-10)
    }

    @Test
    fun `수요가 비어있을 때`() {
        asia.demand = 0

        Assertions.assertThat(asia.demand).isEqualTo(0)
        Assertions.assertThat(asia.shortFall).isEqualTo(25)
        Assertions.assertThat(asia.profit).isEqualTo(0)
    }

    @Test
    fun `생산자 목록 필드에 문자열 대입`() {
        val dumy = """
            {
              "name": "Asia",
              "producers": "String",
              "demand": 30,
              "price": 20
            }
        """.trimIndent()

        Assertions.assertThatThrownBy { asia = json.decodeFromString(dumy) }
            .isInstanceOf(SerializationException::class.java)
    }
}
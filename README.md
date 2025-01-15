# Refactoring 2nd (Martin Fowler)
- **기간 : 2025.01 ~**
- **목표 : 리팩터링 2판을 읽고 정리한다.**

# CHAPTER 01 리팩터링: 첫 번째 예시
* * *

> 프로그램이 잘 작동하는 상황에서 그저 코드가 '지저분하다'는 이유로 불평하는 것은 프로그램의 구조를 너무 미적인 기준으로만 판단하는 건 아닐까? 컴파일러는
> 코드가 깔끔하든 지저분하든 개의치 않으니 말이다. 하지만 그 코드를 수정하려면 사람이 개입되고, 사람은 코드의 미적 상태에 민감하다. 설계가 나쁜 시스템은
> 수정하기 어렵다. 원하는 동작을 수행하기 위해 수정해야할 부분을 찾고, 기존 코드와 잘 맞물려 작동하게 할 방법을 강구하기가 어렵기 때문이다. 무엇을 수정할지
> 찾기 어렵다면 실수를 저질러서 버그가 생길 가능성도 높아진다. (p.26-27)

> 프로그램이 새로운 기능을 추가하기에 편한 구조가 아니라면, 먼저 기능을 추가하기 쉬운 형태로 리팩터링하고 나서 원하는 기능을 추가한다. (p.27)

> 리팩터링하기 전에 제대로 된 테스트부터 마련한다. 테스트는 반드시 자가진단하도록 만든다. (p.28)

> 리팩터링은 프로그램 수정을 작은 단계로 나눠 진행한다. 그래서 중간에 실수하더라도 버그를 쉽게 찾을 수 있다. (p.32)

## 리팩터링 기법 예시

### 함수 추출하기
- 코드 조각을 찾아 무슨 일을 하는지 파악한 다음, 독립된 함수로 추출하고 목적에 맞는 이름을 붙인다.
- 함수 반환 값에 변수명을 의미있게 붙인다.

※ 주의할 점
- 별도 함수로 빼냈을 때 유효범위를 벗어나는 변수, 즉 새 함수에서는 곧바로 사용할 수 없는 변수가 있는지 확인한다.

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    var totalAmount = 0
    var volumeCredits = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    
    for(perf in invoice.performances) {
        val play = plays.play[perf.playId]!!
        var thisAmount = 0

        when (play.type) {
            "tragedy" -> {
                thisAmount = 40000
                if (perf.audience > 30) {
                    thisAmount += 1000 * (perf.audience - 30)
                }
            }
            "comedy" -> {
                thisAmount = 30000
                if (perf.audience > 20) {
                    thisAmount += 10000 + 500 * (perf.audience - 20)
                }
                thisAmount += 300 * perf.audience
            }
            else -> throw IllegalArgumentException("알 수 없는 장르: ${play.type}")
        }
        
        volumeCredits += max(perf.audience - 30, 0)
        
        if("comedy" == play.type) volumeCredits += floor(perf.audience.toDouble() / 5).toInt()
        
        result += "  ${play.name}: $${format.format(thisAmount / 100.0)} (${perf.audience}석)\n"
        totalAmount += thisAmount
    }
    result += "총액: $${format.format(totalAmount / 100.0)}\n"
    result += "적립 포인트: $volumeCredits 점\n"
    
    return result
}
```

**'함수 추출하기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    //추출 함수
    fun amountFor(play: Play, perf: Performance): Int {
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
    
    var totalAmount = 0
    var volumeCredits = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    
    for(perf in invoice.performances) {
        val play = plays.play[perf.playId]!!
        //변경 코드
        val thisAmount = amountFor(play, perf)
        
        volumeCredits += max(perf.audience - 30, 0)
        
        if("comedy" == play.type) volumeCredits += floor(perf.audience.toDouble() / 5).toInt()
        
        result += "  ${play.name}: $${format.format(thisAmount / 100.0)} (${perf.audience}석)\n"
        totalAmount += thisAmount
    }
    result += "총액: $${format.format(totalAmount / 100.0)}\n"
    result += "적립 포인트: $volumeCredits 점\n"
    
    return result
}
```

**statement()에서 thisAmount 값을 채울 때 사용하는 코드를 함수로 추출하여 amountFor() 함수로 만들었다.**

### 임시 변수를 질의 함수로 바꾸기
- 변수가 단순한 연산 결과라면 그 연산을 수행하는 함수로 만들어 사용한다.(매개변수의 개수를 줄일 수 있다.)

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    ...
    for(perf in invoice.performances) {
        val play = plays.play[perf.playId]!!
        ...
    }
    ...
}
```

**'임시 변수를 질의 함수로 바꾸기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    //추출 함수
    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }
    
    ...
    for(perf in invoice.performances) {
        //변경 코드
        val play = playFor(perf)
        ...
    }
    ...
}
```

**statement()에서 임시 변수인 play 값을 채울 때 사용하는 코드를 playFor() 함수로 만들었다.**

### 변수 인라인하기
- 코드의 라인 수를 줄일 수 있다.
- 변수가 단순한 역할을 하고 있어서 변수 자체가 코드의 의미를 더 분명하게 전달하지 못할 때 사용한다.

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    fun amountFor(play: Play, perf: Performance): Int {
        ...
    }
    
    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }
    
    ...
    for(perf in invoice.performances) {
        val play = playFor(perf)
        val thisAmount = amountFor(play, perf)
        ...
    }
    ...
}
```

**'변수 인라인하기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    fun amountFor(play: Play, perf: Performance): Int {
        ...
    }
    
    fun playFor(perf: Performance): Play {
        return plays.play[perf.playId]!!
    }
    
    ...
    for(perf in invoice.performances) {
        //변경 코드
        val thisAmount = amountFor(playFor(perf), perf)
        ...
    }
    ...
}
```

**statement()에서 play 변수를 인라인하여 amountFor() 함수의 매개변수로 넘겨줬다.**

### 함수 선언 바꾸기
- 함수의 이름을 바꾸거나, 매개변수를 추가하거나, 제거하거나, 순서를 바꾸거나, 반환값을 바꾸는 등의 작업을 한다.

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    fun amountFor(play: Play, perf: Performance): Int {
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
    
    fun playFor(perf: Performance): Play {
        ...
    }
    
    ...
    for(perf in invoice.performances) {
        val thisAmount = amountFor(playFor(perf), perf)
        ...
    }
    ...
}
```

**'함수 선언 바꾸기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    //변경 코드
    fun amountFor(perf: Performance): Int {
        var result = 0

        //변경 코드
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
            //변경 코드
            else -> throw IllegalArgumentException("알 수 없는 장르: ${playFor(perf).type}")
        }

        return result
    }
    
    fun playFor(perf: Performance): Play {
        ...
    }
    
    ...
    for(perf in invoice.performances) {
        //변경 코드
        val thisAmount = amountFor(perf)
        ...
    }
    ...
}
```

**amountFor() 함수의 매개변수 play를 제거하고, playFor()를 amountFor() 함수 내부에서 사용하도록 변경했다.**

### 반복문 쪼개기
- 반복문이 너무 길거나, 한 가지 일을 너무 많이 하고 있을 때 사용한다.

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    ...
    for (perf in invoice.performances) {
        volumeCredits = volumeCreditsFor(perf)
        result += "  ${playFor(perf).name}: $${usd(amountFor(perf))} (${perf.audience}석)\n"
        totalAmount += amountFor(perf)
    }
    ...
}
```

**'반복문 쪼개기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    ...
    for (perf in invoice.performances) {
        //변경 코드
        result += "  ${playFor(perf).name}: $${usd(amountFor(perf))} (${perf.audience}석)\n"
        totalAmount += amountFor(perf)
    }

    //변경 코드
    for (perf in invoice.performances) {
        volumeCredits += volumeCreditsFor(perf)
    }
    ...
}
```

**statement() 함수의 반복문을 두 개로 나누어, 첫 번째 반복문에서는 result와 totalAmount를 계산하고, 두 번째 반복문에서는 volumeCredits를
계산하도록 변경했다.**

### 문장 슬라이드하기
- 변수 초기화 문장을 변수 값 대입 직후로 옮긴다.

**기존코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    ...
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
    ...
}
```

**'문장 슬라이드하기' 기법을 적용한 코드**

```kotlin
fun statement(invoice: Invoice, plays: Plays): String {
    ...
    var totalAmount = 0
    var result = "청구 내역 (고객명: ${invoice.customer})\n"
    for (perf in invoice.performances) {
        result += "  ${playFor(perf).name}: $${usd(amountFor(perf))} (${perf.audience}석)\n"
        totalAmount += amountFor(perf)
    }

    //변경 코드
    var volumeCredits = 0
    for (perf in invoice.performances) {
        volumeCredits += volumeCreditsFor(perf)
    }
    ...
}
```

**statement() 함수의 totalAmount와 volumeCredits 변수 초기화 문장을 반복문 직전으로 옮겼다.**

### 단계 쪼개기
- 한 함수에서 두 가지 일을 하고 있을 때 사용한다.

### 반복문을 파이프라인으로 바꾸기
- 반복문을 파이프라인으로 바꾸면 코드가 깔끔해지고, 각 단계를 수정하거나 재배치하기 쉬워진다.

**기존코드**

```kotlin
fun totalVolumeCredits(): Int {
    var result = 0
    
    for (perf in invoice.performances) {
        result += volumeCreditsFor(perf)
    }
    
    return result
}

fun totalAmount(): Int {
    var result = 0
    
    for (perf in invoice.performances) {
        result += amountFor(perf)
    }
    
    return result
}
```

**'반복문을 파이프라인으로 바꾸기' 기법을 적용한 코드**

```kotlin
fun totalVolumeCredits(): Int {
    //변경 코드
    return invoice.performances.map { volumeCreditsFor(it) }
        .reduce { total, volumeCredits -> total + volumeCredits }
}

fun totalAmount(): Int {
    //변경 코드
    return invoice.performances.map { amountFor(it) }
        .reduce { total, amount -> total + amount }
}
```

**totalVolumeCredits()와 totalAmount() 함수의 반복문을 파이프라인으로 변경했다.**

### 조건부 로직을 다형성으로 바꾸기
- 조건부 로직을 다형성으로 바꾸면 새로운 타입을 추가하거나 기존 타입을 수정할 때 코드를 수정하기 쉬워진다.

**기존코드**

```kotlin
class StatementData(
    var invoice: Invoice,
    var plays: Plays
) {
    ...
    fun amountFor(perf: Performance): Int {
        return PerformanceCalculator(perf, playFor(perf)).amount()
    }

    fun volumeCreditsFor(perf: Performance): Int {
        return PerformanceCalculator(perf, playFor(perf)).volumeCredits()
    }
    ...
}
```

**'조건부 로직을 다형성으로 바꾸기' 기법을 적용한 코드**

```kotlin
class StatementData(
    var invoice: Invoice,
    var plays: Plays
) {
    ...
    fun amountFor(perf: Performance): Int {
        return createPerformanceCalculator(perf, playFor(perf)).amount()
    }

    fun volumeCreditsFor(perf: Performance): Int {
        return createPerformanceCalculator(perf, playFor(perf)).volumeCredits()
    }
    ...
}

open class PerformanceCalculator(
    var performance: Performance,
    var play: Play
) {
    ...
    open fun amount(): Int {
        throw RuntimeException("서브클래스에서 처리하도록 설계되었습니다.")
    }

    open fun volumeCredits(): Int {
        return max(performance.audience - 30, 0)
    }
}

class TragedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play) {

    override fun amount(): Int {
        var result = 40000

        if (performance.audience > 30) {
            result += 1000 * (performance.audience - 30)
        }

        return result
    }
}

class ComedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play) {

    override fun amount(): Int {
        var result = 30000

        if (performance.audience > 20) {
            result += 10000 + 500 * (performance.audience - 20)
        }

        result += 300 * performance.audience

        return result
    }
    
    override fun volumeCredits(): Int {
        return super.volumeCredits() + floor(performance.audience / 5.0).toInt()
    }
}
```

**TragedyCalculator와 ComedyCalculator 클래스를 만들어서 PerformanceCalculator 클래스를 상속받게 하고, amount()와 volumeCredits()
함수를 오버라이딩하여 다형성 처리한다.**

### 함수 옮기기
- 함수가 자신이 속한 모듈보다 다른 모듈의 기능을 더 많이 이용할 때 사용한다.

### 타입 코드를 서브클래스로 바꾸기
- 타입 코드를 서브클래스로 바꾸면 타입 코드에 따라 동작이 달라지는 조건부 로직을 없앨 수 있다.

### 생성자를 팩터리 함수로 바꾸기
- 생성자를 팩터리 함수로 바꾸면 객체를 생성하는 방식을 변경하기 쉬워진다.

**기존코드**

```kotlin
class PerformanceCalculator(
    var performance: Performance,
    var play: Play
)
```

**'타입 코드를 서브클래스로 바꾸기', '생성자를 팩터리 함수로 바꾸기' 기법을 적용한 코드**

```kotlin
//변경 코드
open class PerformanceCalculator(
    var performance: Performance,
    var play: Play
) {

    //변경 코드
    companion object {
        fun createPerformanceCalculator(perf: Performance, play: Play): PerformanceCalculator {
            return when (play.type) {
                "tragedy" -> TragedyCalculator(perf, play)
                "comedy"-> ComedyCalculator(perf, play)
                else -> throw IllegalArgumentException("알 수 없는 장르: ${play.type}")
            }
        }
    }
    ...
}

//변경 코드
class TragedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play)
//변경 코드
class ComedyCalculator(
    perf: Performance, play: Play
): PerformanceCalculator(perf, play)
```

### 함수 매개변수화하기
- 두 함수의 로직이 아주 비슷하고 단지 리터럴 값만 다르다면, 그 값만 매개변수로 받아 처리하는 함수 하나로 합쳐서 중복을 없앨 수 있다.

**기존코드**

```kotlin
fun tenPercentRaise(aPerson: Person) {
   aPerson.salary = aPerson.salary.multiply(1.1)
}

fun fivePercentRaise(aPerson: Person) {
   aPerson.salary = aPerson.salary.multiply(1.05)
}
```

**'함수 매개변수화하기' 기법을 적용한 코드**

```kotlin
fun raise(aPerson: Person, factor: Double) {
   aPerson.salary = aPerson.salary.multiply(1 + factor)
}
```

**tenPercentRaise()와 fivePercentRaise() 함수를 raise() 함수로 합쳤다.**

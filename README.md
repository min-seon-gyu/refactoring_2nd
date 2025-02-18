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

# CHAPTER 02 리팩터링 원칙
* * *

> 누군가 "리팩터링하다가 코드가 깨져서 며칠이나 고생했다"라고 한다면, 십중팔구 리팩터링한 것이 아니다. (p.80).

> 리팩터링의 궁극적인 목적은 개발 속도를 높여서, 더 적은 노력으로 더 많은 가치를 창출하는 것이다. (p.92).

## 2.1 리팩터링 정의
리팩터링[명사] : 소프트웨어의 겉보기 동작은 그대로 유지한 채, 코드를 이해하고 수정하기 쉽도록 내부 구조를 변경하는 기법

리팩터링(하다)[동사] : 소프트웨어의 겉보기 동작은 그대로 유지한 채, 여러 가지 리팩터링 기법을 적용해서 소프트웨어를 재구성하다.

**리팩터링의 목적은 코드를 이해하고 수정하기 쉽게 만드는 것이다.(성능 X)**

## 2.2 두 개의 모자
기능을 추가할 때는 기존 코드는 절대 건드리지 않고 새 기능을 추가하기만 하고, 리팩터링할 때는 기능 추가는 절대 하지 않기로 다짐한 뒤 오로지 코드 재구성에만
전념한다.

## 2.3 리팩터링하는 이유

### 리팩터링하면 소프트웨어 설계가 좋아진다.
아키텍처를 충분히 이해하지 못한 채 단기 목표만을 위해 코드를 수정하다 보면 기반 구조가 무너지기 쉽다. 그러면 코드만 봐서는 설계를 파악하기 어려워진다. 반면
규칙적인 리팩토링은 코드의 구조를 지탱해줄 것이다.

### 리팩터링하면 소프트웨어를 이해하기 쉬워진다.
프로그램을 동작시키는 데만 신경 쓰다 보면 나중에 그 코드를 다룰 개발자를 배려하지 못한다는 데 있다. 리팩터링은 코드가 더 잘 읽히게 도와준다. 코드의 목적이
더 잘 드러나게, 다시 말해 내 의도를 더 명확하게 전달하도록 개선할 수 있다.

단지, 다른 사람뿐만 아니라 내 자신에게도 적용된다.

### 리팩터링하면 버그를 쉽게 찾을 수 있다.
리팩터링하면 코드가 하는 일을 깊이 파악하게 되면서 새로 꺠달은 것을 곧바로 코드에 반영하게 된다. 프로그램의 구조를 명확하게 다듬으면 그냥 '이럴 것이다'라고
가정하던 점들이 분명히 드러나는데, 버그를 지나치려야 지나칠 수 없을 정도까지 명확해진다.

### 리팩터링하면 프로그래밍 속도를 높일 수 있다.
지금까지 제시한 장점을 한 마디로 정리하면 다음과 같다. 리팩터링하면 코드 개발 속도를 높일 수 있다. 내부 설계가 잘 된 소프트웨어는 새로운 기능을 추가할
지점과 어떻게 고칠지를 쉽게 찾을 수 있다. 모듈화가 잘 되어 있으면 전체 코드베이스 중 작은 일부만 이해하면 된다. 코드가 명확하면 버그를 만들 가능성도 줄고,
버그를 만들더라도 디버깅하기가 쉽다.

## 2.4 언제 리팩터링해야 할까?

### 3의 법칙
> 1. 처음에는 그냥한다
> 2. 비슷한 일을 두 번째로 하게 되면(중복이 생격다는 사실에 당황스럽겠지만), 일단 계속 진행한다.
> 3. 비슷한 일을 세 번째 하게 되면 리팩터링한다.

### 준비를 위한 리팩터링: 기능을 쉽게 추가하게 만들기
리팩터링하기 가장 좋은 시점은 코드베이스에 기능을 새로 추가하기 직전이다. 이 시점에 현재 코드를 살펴보면서, 구조를 살짝 바꾸면 다른 작업을 하기가 훨씬 쉬워질
만한 부분을 찾는다.

### 이해를 위한 리팩터링: 코드를 이해하기 쉽게 만들기
코드를 수정하려면 먼저 그 코드가 하는 일을 파악해야 한다. 코드를 파악할 때마다 그 코드의 의도가 더 명확하게 드러나도록 리팩터링할 여지는 없는지 찾아본다.

### 쓰레기 줍기 리팩터링
간단히 수정할 수 있는 것은 즉시 고치고, 시간이 좀 걸리는 일은 짧은 메모만 남긴 다음, 하던 일을 끝내고 나서 처리한다. 물론 수정하려면 몇 시간이나 걸리고
당장은 더 급한 일이 있을 수 있다. 그렇더라도 조금이나마 개선해두는 것이 좋다.

### 코드 리뷰에 리팩터링 활용하기
리팩터링은 코드 리뷰의 결과를 더 구체적으로 도출하는 데에도 도움된다. 개선안들을 제시하는 데서 그치지 않고, 그중 상당수를 즉시 구현해볼 수 있기 때문이다.
코드 리뷰를 이런식으로 진행하면 훨씬 큰 성취감을 맛볼 수 있다.

### 리팩터링하지 말아야 할 때
- 지저분한 코드를 발견해도 굳이 수정할 필요가 없을 때
- 리팩터링하는 것보다 처음부터 새로 작성하는 게 쉬울 때

## 2.5 리팩터링 시 고려할 문제

### 새 기능 개발 속도 저하
**리팩터링의 궁극적인 목적은 개발 속도를 높여서, 더 적은 노력으로 더 많은 가치를 창출하는 것이다.**

### 브랜치
독립 브랜치로 작업하는 기간이 길어질수록 작업 결과를 마스터로 통합하기가 어려워진다. 그러므로 하루에 최소 한 번은 마스터와 통합하는 과정을 거쳐야한다.

### 테스팅
리팩터링하기 위해서는 자가 테스트 코드를 마련해야 한다. 자가 테스트 코드는 리팩터링을 할 수 있게 해줄 뿐만 아니라, 새 기능 추가도 훨씬 안전하게 진행할 수
있도록 도와준다. 핵심은 테스트가 실패한다면 가장 최근에 통과한 버전에서 무엇이 달라졌는지 살펴볼 수 있다는 데 있다. 테스트 주기가 짧다면 단 몇 줄만 비교하면
되며, 문제를 일으킨 부분이 그 몇 줄 안에 있기 때문에 버그를 훨씬 쉽게 찾을 수 있다.

### 데이터베이스
> 1. 새로운 데이터베이스 필드를 추가하고 사용은 하지 않음
> 2. 기존 필드와 새 필드를 동시에 업데이트
> 3. 데이터베이스를 읽는 클라이언트들을 새 필드를 사용하는 버전으로 조금씩 교체

## 2.6 리팩터링, 아키텍처, 애그니(YAGNI)
애그니는 '지금 필요한 것만 하라'는 원칙이다.

## 2.7 리팩터링과 소프트웨어 개발 프로세스
테스트 주도 개발 = 자가 테스트 코드 + 리팩토링

## 2.8 리팩터링과 성능
직관적인 설계 vs 성능(소프트웨어를 이해가기 쉽게 만들기 위해 속도가 느려지는 방향으로 수정하는 경우가 많다.)

빠른 소프트웨어를 작성하는 방법
- 시간 예산 분배 방식(가장 엄격한 방법) : 하드 리얼타임 시스템에서 많이 사용, 컴포넌트마다 자원(시간, 공간)을 할당한다. 할당된 자원 예산을 초과할 수
없으며, 주어진 자원을 서로 주고받는 메커니즘을 제공한다.
- 끊임없이 관심 기울이기 : 직관적이어서 흔히 사용하는 방식이지만 실제 효과는 미미하다.

## 2.9 리팩터링의 유래

## 2.10 리팩터링 자동화

## 2.11 더 알고 싶다면

# CHAPTER 03 코드에서 나는 악취
* * *

> 리팩터링을 언제 시작하고 언제 그만할지를 판단하는 일은 리팩터링의 작동 원리를 아는 것 못지않게 중요하다.

## 3.1 기이한 이름
함수, 모듈, 변수, 클래스 등은 그 이름만 보고도 각각이 무슨 일을 하고 어떻게 사용해야 하는지 명확히 알 수 있도록 엄청나게 신경 써서 이름을
지어야 한다.

우리가 가장 많이 사용하는 리팩터링도 **함수 선언 바꾸기**`6.5절`, **변수 이름 바꾸기**`6.7절`, **필드 이름 바꾸기**`9.2절`처럼 이름을
바꾸는 리팩터링들이다.

## 3.2 중복 코드
똑같은 코드 구조가 여러 곳에서 반복된다면 하나로 통합하여 더 나은 프로그램을 만들 수 있다. 코드가 중복되면 각각을 볼 때마다 서로 차이점은 없는지
주의 깊게 살펴봐야 하는 부담이 생긴다. 그중 하나를 변경할 때는 다른 비슷한 코드들도 모두 살펴보고 적절히 수정해야 한다.

중복 코드를 제거하기 위한 리팩터링은 **함수 추출하기**`6.1절`, **문장 슬라이드하기**`8.6절`, **메서드 올리기**`12.1절` 기법이 있다.

## 3.3 긴 코드
함수를 짧게 구성할 때 코드를 이해하고, 공유하고, 선택하기 쉬워진다는 장점을 가질 수 있다.

짧은 함수로 구성된 코드를 이해하기 쉽게 만드는 가장 확실한 방법은 좋은 이름이다. 함수 이름을 잘 지어두면 본문 코드를 볼 이유가 사라진다. 그러기
위해서는 훨씬 적극적으로 함수를 쪼개야 한다.

주석을 달아야 할 만한 부분은 무조건 함수로 만든다. 그 함수 본문에는 원래 주석으로 설명하려던 코드가 담기고, 함수 이름은 동작 방식이 아닌 의도가
드러나게 짓는다. 이러한 과정이 기존 코드보다 길어지더라도 함수로 뽑는 것이 좋다. 단, 함수 이름에 코드의 목적을 드러내야 한다. 여기서 핵심은
함수의 길이가 아닌, 함수의 목적과 구현 코드의 괴리가 얼마나 큰가다. 즉 '무엇을 하는지'를 코드가 잘 설명해주지 못할수록 함수로 만드는 게
유리하다.

함수를 짧게 만드는 작업의 99%는 **함수 추출하기**`6.1절`이다. 매개변수와 임시 변수를 많이 사용한다면 **임시 변수를 질의 함수로 바꾸기**`7.4절`
, **매개변수 객체 만들기**`6.8절`, **객체 통째로 넘기기**`11.4절` 기법을 사용해야 한다. 이 리팩터링들을 적용해도 여전히 임시 변수와
매개변수가 너무 많다면 더 큰 수술이라 할 수 있는 **함수를 명령으로 바꾸기**`11.9절`를 고려해보자.

그 밖에도 **조건문 분해하기**`10.1절`, **조건부 로직을 다형성으로 바꾸기**`10.4절`, **반복문 쪼개기**`8.7절`를 적용하여 조건문이나
반복문에서도 분리할 수 있다.

## 3.4 긴 매개변수 목록
매개변수 목록이 길어지면 그 자체로 이해하기 어려울 때가 많다. 종종 다른 매개변수에서 값을 얻어올 수 있는 매개변수가 있을 수 있는데, 이런
매개변수는 **매개변수를 질의 함수로 바꾸기**`11.5절`로 제거할 수 있다.

사용 중인 데이터 구조에서 값들을 뽑아 각각을 별개의 매개변수로 전달하는 코드라면 **객체 통째로 넘기기**`11.4절`를 적용해서 원본 데이터를 그대로
전달한다. 항상 함께 전달되는 매개변수들은 **매개변수 객체 만들기**`6.8절`로 묶어주면 된다. 함수의 동작 방식을 정하는 플래그 역할의
매개변수는 **플래그 인수 제거하기**`11.3절`로 제거한다.

클래스는 매개변수 목록을 줄이는 데 효과적인 수단이기도 하다. 특히 여러 개의 함수가 특정 매개변수들의 값을 공통으로 사용할 때 유용하다. 이럴 때는
**여러 함수를 클래스로 묶기**`6.9절`를 이용핳여 공통 값들을 클래스의 필드로 정의한다.

## 3.5 전역 데이터
전역 데이터는 코드베이스 어디에서든 건드릴 수 있고 값을 누가 바꿨는지 찾아낼 메커니즘이 없다는 게 문제다. 이를 방지하기 위해 우리가 사용하는
대표적인 리팩터링은 **변수 캡슐화하기**`6.6절`다. 다른 코드에서 오염시킬 가능성이 있는 데이터를 발견할 때마다 이 기법을 가장 먼저 적용한다.
이런 데이터를 함수로 감싸는 것만으로도 데이터를 수정하는 부분을 쉬벡 찾을 수 있고 접근을 통제할 수 있게 된다.

## 3.6 가변 데이터
데이터를 변경했더니 예상치 못한 결과나 골치 아픈 버그로 이어지는 경우가 종종 있다. 특히 이 문제가 아주 드문 조건에서만 발생한다면
원인을 알아내기가 매우 어렵다.

**변수 캡슐화하기**`6.6절`를 적용하여 정해놓은 함수를 거쳐야만 값을 수정할 수 있도록 하면 값이 어떻게 수정되는지 감시하거나 코드를 개선하기 쉽다.
하나의 변수에 용도가 다른 값들을 저장하느라 값을 갱신하는 경우라면 **변수 쪼개기**`9.1절`를 이용하여 용도별로 독립 변수에 저장하게 하여 값 갱신이
문제를 일으킬 여지를 없앤다. 그리고 **문장 슬라이드하기**`8.6절`, **함수 추출하기**`6.1절`를 이용해서 무언가를 갱신하는 코드를 분리하는 것이 좋다.
API를 만들 때는 **질의 함수와 변경 함수 분리하기**`11.1절`를 활용해서 꼭 필요한 경우가 아니라면 부작용이 있는 코드를 호출할 수 없게 한다. 우리는
가능한 한 **세터 제거하기**`11.7절`도 적용한다.

## 3.7 뒤엉킨 변경
우리는 소프트웨어의 구조를 변경하기 쉬운 형태로 조직한다. 뒤엉킨 변경은 단일 책임 원칙이 제대로 지켜지지 않을 때 나타난다. 즉, 하나의 모듈이
서로 다른 이유들로 인해 여러 가지 방식으로 변경되는 일이 많을 때 발생한다.

## 3.8 산탄총 수술
산탄총 수술은 뒤엉킨 변경과 비슷하면서도 정반대다. 이는 코드를 변경할 때마다 자잘하게 수정해야 하는 클래스가 많을 때 발생한다. 변경할 부분이
코드 전반에 퍼져 있다면 찾기도 어렵고 꼭 수정해야 할 곳을 지나치기 쉽다.

이럴 때는 함께 변경되는 대상들을 **함수 옮기기**`8.1절`와 **필드 옮기기**`8.2절`로 모두 한 모듈에 묶어두면 좋다. 비슷한 데이터를 다루는
함수가 많다면 **여러 함수를 클래스로 묶기**`6.9절`를 적용한다. 데이터 구조를 변환하거나 보강하는 함수들에는 **여러 함수를 변환 함수로 묶기**
`6.10절`를 적용한다.

어설프게 분리된 로직을 **함수 인라인하기**`6.2절`나 **클래스 인라인하기**`7.6절`같은 인라인 리팩터링으로 하나로 합치는 것도 산탄총 수술에
대처하는 좋은 방법이다.

## 3.9 기능 편애
프로그램을 모듈화할 때는 코드를 여러 영억으로 나눈 뒤 영역 안에서 이뤄지는 상호작용은 최대한 늘리고 영역 사이에서 이뤄지는 상호작용은
최소로 줄이는 데 주력한다.

기능 편애는 흔히 어떤 함수가 자기가 속한 모듈의 함수나 데이터보다 다른 모듈의 함수나 데이터와 상호작용 할 일이 더 많을 때 발생한다.
이런 경우에는 **함수 옮기기**`8.1절`, **함수 추출하기**`6.1절`로 해결할 수 있다.

## 3.10 데이터 뭉치
데이터 항목의 서너 개가 여러곳에서 항상 뭉쳐 다니는 모습을 목격하면 이들을 위한 보금자리를 마련해줘야 한다. 가장 먼저 **클래스 추출하기**`7.5절`
로 하나의 객체로 묶는다. 다음은 메서드 시그니처에 있는 데이터 뭉치는 **매개변수 객체 만들기**`6.8절`나 **객체 통째로 넘기기**`11.4절`를 적용해서
매개변수 수를 줄여본다.

## 3.11 기본형 집착
대부분의 프로그래밍 언어는 다양한 기본형을 제공한다. 기본형을 객체로 바꾸기`7.3절`를 적용하는 것이 좋다.

## 3.12 반복되는 switch문
중복된 switch문이 문제가 되는 이유는 새로운 타입을 추가하거나 기존 타입을 수정할 때 switch문을 찾아 모든 case문을 수정해야하기 때문이다.
이런 경우 **조건부 로직을 다형성으로 바꾸기**`10.4절`로 해결한다.

## 3.13 반복문
**반복문을 파이프라인으로 바꾸기**`8.8절`을 적절히 사용하여 코드에서 각 원소들이 어떻게 처리되는지 명확하게 드러나게 한다.

## 3.14 성의 없는 요소
우리는 코드의 구조를 잡을 때 프로그램 요소를 이용하는 걸 좋아한다. 그래야 그 구조를 변형하거나 재활용할 기회가 생기고, 혹은 단순히 더 의미
있는 이름을 가졌기 때문이다. 그렇지만 그 구조가 필요 없을 때도 있다. 본문 코드를 그대로 쓰는 것과 진배없는 함수도 있고, 실질적으로 메서드가
하나뿐인 클래스도 있다. 이런 구조는 나중에 더 추가하려고 했지만 어떠한 이유로 그러지 못한 결과일 수 있다. 

해당 요소는 제거하는 편이 좋다. **함수 인라인하기**`6.2절`, **클래스 인라인하기**`7.6절`로 처리를 하고 상속을 사용했다면 
**계층 합치기**`12.9절`를 적용한다.

## 3.15 추측성 일반화
추측성 일반화는 '나중에 필요할 거야'라는 생각으로 당장은 필요 없는 모든 종류의 후킹 포인트와 특이 케이스 처리 로직을 작성해 두는 것이다.
미래에 실제로 사용한다면 다행이지만, 그렇지 않는다면 쓸데없는 낭비일 뿐이다.

하는 일이 거의 없는 추상 클래스는 **계층 합치기**`12.9절`로 제거한다. 쓸데없이 위임하는 코드는 **함수 인라인하기**`6.2절`나 
**클라스 인라인하기**`7.6절`로 삭제한다. 본문에서 사용되지 않는 매개변수는 함수 선언 바꾸기로 없앤다.

추측성 일반화는 테스트 코드 말고는 사용하는 곳이 없는 함수나 클래스에서 흔히 볼 수 있다. 이런 코드를 발견하면 테스트 케이스부터 삭제한 뒤에
**죽은 코드 제거하기**`8.9절`로 제거하자.

## 3.16 임시 필드
간혹 특정 상황에서만 값이 설정되는 필드를 가진 클래스도 있다. 하지만 객체를 가져올 때는 당연히 모든 필드가 채워져 있으리라 기대하는게 보통이라,
이렇게 임시 필드를 갖도록 작성하면 코드를 이해하기 어렵다. 그래서 사용자는 쓰이지 않는 것처럼 보이는 필드가 존재하는 이유를 파악하기 어렵다.

이렇게 덩그러니 떨어져 있는 필드들을 발견하면 **클래스 추출하기**`7.5절`로 제 위치를 찾아준다. 그런 다음 **함수 옮기기**`8.1절`로 임시 필드들과 관련된
코드를 모조리 새 클래스에 몰아 넣는다. 또한, 임시 필드들이 유효한지 확인한 후 동작하는 조건부 로직이 있을 수 있는데,
**특이 케이스 추가하기**`10.5절`로 필드들이 유효하지 않을 때를 위한 대안 클래스를 만들어서 제거할 수 있다.

## 3.17 메시지 체인
메시지 체인은 클라이언트가 한 객체를 통해 다른 객체를 얻은 뒤 방금 얻은 객체에 또 다른 객체를 요청하는 식으로, 다른 객체를 요청하는 작업이
연쇄적으로 이어지는 코드를 말한다. 이는 클라이언트가 객체 네비게이션 구조에 종속됐음을 의미한다. 그래서 내비게이션 중간 단계를 수정하면
클라이언트 코드도 수정해야 한다.

이 문제는 **위임 숨기기**`7.7절`로 해결한다. 이 리팩터링은 메시지 체인의 다양한 연결점에 적용할 수 있다. 원칙적으로 체인을 구성하는 모든 객체에
적용할 수 있지만, 그러다 보면 중간 객체들이 모두 중개자가 돼버리기 쉽다. 그러니 최종 결과 객체가 어떻게 쓰이는지부터 살펴보는게 좋다.
**함수 추출하기**`6.1절`로 결과 객체를 사용하는 코드 일부를 따로 빼낸 다음 **함수 옮기기**`8.1절`로 체인을 숨길 수 있는지 살펴보자.

## 3.18 중개자
객체의 대표적인 기능 하나로, 외부로부터 세부사항을 숨겨주는 캡슐화가 있다. 캡슐화하는 과정에서는 위임이 자주 활용된다. 하지만 지나치면 문제가
된다. 클래스가 제공하는 메서드 중 절반이 다른 클래스에 구현을 위임하고 있다면 어떤가? 이럴 때는 **중개자 제거하기**`7.8절`를 활용하여 실제로 일을
하는 객체와 직접 소통하게 하자.

## 3.19 내부자 거래
커피 자판기 옆에서 은밀히 데이터를 주고받는 모듈이 있다면 **함수 옮기기**`8.1절`와 **필드 옮기기**`8.2절` 기법으로 떼어놓아서 사적으로 처리하는 부분을 줄인다.
여러 모듈이 같은 관심사를 공유한다면 공통 부분을 정식으로 처리하는 제3의 모듈을 새로 만들거나 **위임 숨기기**`7.7절`을 이용하여
다른 모듈이 중간자 역할을 하게 만든다.

상속 구조에서는 부모 자식 사이에 결탁이 생길 때가 있다. 자식 클래스는 항상 부모 클래스가 공개하고 싶은 것 이상으로 부모에 대해 알려고 한다.
그러다가 부모 품을 떠나야 할 때가 온다면 **서브클래스를 위임으로 바꾸기**`12.10절`나 **슈퍼클래스를 위임으로 바꾸기**`12.11절`를 활용하자.

## 3.20 거대한 클래스
한 클래스가 너무 많은 일을 하려다 보면 필드 수가 상당히 늘어난다. 그리고 클래스에 필드가 너무 많으면 중복 코드가 생기기 쉽다. 이럴 때는
**클래스 추출하기**`7.5절`로 필드를 일부를 따로 묶는다. 같은 컴포넌트에 모아두는 것이 합당해 보이는 필드들을 선택하면 된다. 더 일반적으로는,
한 클래스 안에서 접두어나 접미어가 같은 필드들이 함께 추출할 후보들이다. 이렇게 분리할 컴포넌트를 원래 클래스와 상속 관계로 만드는 게 좋다면
**슈퍼클래스 추출하기**`12.8절`나 **타입 코드를 서브클래스로 바꾸기**`12.6절`를 적용한다.

필드가 너무 많은 클래스와 마찬가지로 코드량이 너무 많은 클래스도 중복 코드와 혼동을 일으킬 여지가 크다. 가장 간단한 해법은 그 클래스 안에서
자체적으로 중복을 제거하는 것이다.

클라이언트들이 거대 클래스를 이용하는지 패턴을 파악하여 그 클래스를 어떻게 쪼갤지 단서를 얻을 수도 있다. 먼저 클라이언트들이 거대 클래스의 특정
기능 그룹만 주로 사용하는지 살핀다. 이때 각각의 기능 그룹이 개별 클래스로 추출될 후보다. 유용한 기능 그룹을 찾았다면
**클래스 추출하기**`7.5절`, **슈퍼클래스 추출하기**`12.8절`, **타입 코드를 서브클래스로 바꾸기**`12.6절` 등을 활용해서 분리한다.

## 3.21 서로 다른 인터페이스의 대안 클래스들
클래스를 사용할 때의 큰 장점은 필요에 따라 언제든 다른 클래스로 교체할 수 있다는 것이다. 단, 교체하려면 인터페이스가 같아야 한다. 따라서
**함수 선언 바꾸기**`6.5절`로 메서드 시그니처를 일치시킨다. 때로는 **함수 옮기기**`8.1절`를 이용하여 인터페이스가 같아질 때까지 필요한 동작들을 클래스
안으로 밀어 넣는다. 그러다 대안 클래스들 사이에 중복 코드가 생기면 **슈퍼클래스 추출하기**`12.8절`를 적용할지 고려해본다.

## 3.22 데이터 클래스
데이터 클래스란 데이터 필드와 게터/세터 메서드로만 구성된 클래스를 말한다. 이런 클래스에 public 필드가 있다면 누가 보기 전에 얼른 
**레코드 캡슐화하기**`7.1절`로 숨기자. 변경하면 안 되는 필드는 **세터 제거하기**`11.7절`로 접근을 제한하자.

다른 클래스에서 데이터 클래스의 게터나 세터를 사용하는 메서드를 찾아서 **함수 옮기기**`8.1절`로 그 메서드를 데이터 클래스로 옮길 수 있는지
살펴보자. 메서드를 통째로 옮기기 어렵다면 **함수 추출하**기`6.1절`를 이용해서 옮길 수 있는 부분만 별도 메서드로 뽑아낸다.

## 3.23 상속 포기
서브클래스는 부모로부터 메서드와 데이터를 물려받는데, 이를 원치 않을 수도 있다. 이럴 때는 먼저 같은 계층에 서브클래스를 하나 새로 만들고,
**메서드 내리기**`12.4절`와 **필드 내리기**`12.5절`를 활용해서 물려받지 않을 부모 코드를 모조리 새로 만든 서브클래스로 넘긴다.

## 3.24 주석
주석이 장황하게 달린 원인이 코드를 잘못 작성했기 때문인 경우가 의외로 많다. 특정 코드 블록이 하는 일에 주석을 남기고 싶다면
**함수 추출하기**`6.1절`를 적용해본다. 이미 추출되어 있는 함수임에도 여전히 설명이 필요하다면 함수 선언 바꾸기`6.5절`로 함수 이름을 바꾼다.
시스템이 동작하기 위한 선행조건을 명시하고 싶다면 **어서션 추가하기**`10.6절`가 대기하고 있다.

> 주석을 남겨야겠다는 생각이 들면, 가장 먼저 주석이 필요 없는 코드로 리팩터링해본다.

뭘 할지를 모를 때라면 주석을 달아두면 좋다. 현재 진행 상황뿐만 아니라 확실하지 않은 부분에 주석에 남긴다.

# CHAPTER 06 기본적인 리팩토링

## 6.1 함수 추출하기(반 : 함수 인라인하기)

### 배경
함수를 추출하는 기준으로 길이, 반복 사용 등의 이유가 있지만 가장 적합한 기준으로는 '목적과 구현을 분리'하는 데에 있다. 코드를 보고 어떤 목적이
있는지 한 번에 알 수 없다면 함수로 추출하고 적절한 함수명을 지어주는 것이 좋다.(함수명을 잘 지어주는 것이 중요하다.)

### 절차
1. 함수를 새로 만들고 목적을 잘 드러내는 이름을 붙인다.('어떻게'가 아닌 '무엇을' 하는지가 드러나야 한다.)
2. 추출할 코드를 원본 함수에서 복사하여 새 함수에 붙여넣는다.
3. 추출한 코드 중 원본 함수의 지역 변수를 참조하거나 추출한 함수의 유효범위를 벗어나는 변수가 없는지 검사한다.
4. 변수를 다 처리했다면 컴파일한다.
5. 원본 함수에서 추출한 코드 부분을 새로 만든 함수를 호출하는 문장으로 바꾼다.
6. 테스트한다.
7. 다른 코드에 방금 추출한 것과 똑같거나 비슷한 코드가 없는지 살핀다. 있다면 방금 추출한 새 함수를 호출하도록 바꾼다.

## 6.2 함수 인라인하기(반 : 함수 추출하기)

### 배경
때로는 함수 본문이 함수명만큼 명확한 경우가 있다. 이럴 때는 그 함수를 제거한다. 간접 호출은 유용할 수도 있지만 쓸데없는 간접 호출은 거슬릴
뿐이다. 리팩터링 과정에서 잘못 추출된 함수들도 다시 인라인한다. 잘못 추출된 함수들을 원래 함수로 합친 다음, 필요하면 원하는 형태로 다시
추출하는 것이다.

간첩 호출을 너무 과하게 쓰는 코드도 흔한 인라인 대상이다. 가령 다른 함수로 단순히 위임만 하는 함수들이 너무 많아서 위임 관계가 복잡하게 얽혀
있으면 인라인해버린다.

### 절차
1. 다형 메서드인지 확인한다.
2. 인라인할 함수를 호출하는 곳을 모두 찾는다.
3. 각 호출문을 함수 본문으로 교체한다.
4. 하나씩 교체할 때마다 테스트한다.
5. 함수 정의(선언)를 삭제한다.

## 6.3 변수 추출하기(반 : 변수 인라인하기)

### 배경
표현식이 너무 복잡해서 이해하기 어려울 때가 있다. 이럴 때 지역 변수를 활용하면 표현식을 쪼개 관리하기 더 쉽게 만들 수 있다. 그러면 복잡한
로직을 구성하는 단계마다 이름을 붙일 수 있어서 코드의 목적을 훨씬 명확하게 드러낼 수 있다.

이 과정에서 추가한 변수는 디버깅에도 도움된다. 디버거에 중단점을 지정하거나 상태를 출력하는 문장을 추가할 수 있기 때문이다.

### 절차
1. 추출하려는 표현식에 부작용은 없는지 확인한다.
2. 불변 변수를 하나 선언하고 이름을 붙일 표현식의 복제본을 대입한다.
3. 원본 표현식을 새로 만든 변수로 교체한다.
4. 테스트한다.
5. 표현식을 여러 곳에서 사용한다면 각각을 새로 만든 변수로 교체한다. 하나 교체할 때마다 테스트한다.

## 6.4 변수 인라인하기(반 : 변수 추출하기)

### 배경
변수명이 원래 표현식과 크게 다를 것이 없을 때나 변수가 주변 코드를 리팩터링 하는 데 방해가 되는 경우에는 그 변수를 인라인하는 것이 좋다.

### 절차
1. 대입문의 우변(표현식)에서 부작용이 생기지는 않는지 확인한다. 
2. 변수가 불변으로 선언되지 않았다면 불변으로 만든 후 테스트한다.
3. 이 변수를 가장 처음 사용하는 코드를 찾아서 대입문 우변의 코드로 바꾼다.
4. 테스트한다.
5. 변수를 사용하는 부분을 모두 교체할 때까지 이 과정을 반복한다.
6. 변수 선언문과 대입문을 지운다.
7. 테스트한다.

## 6.5 함수 선언 바꾸기

### 배경
이름이 좋으면 함수의 구현 코드를 살펴볼 필요 없이 호출문만 보고도 무슨 일을 하는지 파악할 수 있다. 하지만 좋은 이름을 떠올리기란 쉽지 않다.
그래서 이름이 잘못된 함수를 발견하면 더 나은 이름이 떠오르는 즉시 바꾸라는 명령으로 받아들인다. 그래야 나중에 그 코드를 다시 볼 때 무슨 일을
하는지 '또' 고민하지 않게 된다.

함수의 매개변수도 마찬가지이다. 매개변수를 올바르게 선택하기란 단순히 규칙 몇 개로 표현할 수 없다. 따라서 어떻게
연결하는 것이 더 나은지 더 잘 이해하게 될 때마다 그에 맞게 코드를 개선할 수 있도록 함수 선언 바꾸기 리팩터링과 친숙해져야만 한다.

#### 절차
간단한 절차
1. 매개변수를 제거하려거든 먼저 함수 본문에서 제거 대상 매개변수를 참조하는 곳은 없는지 확인한다.
2. 메서드 선언을 원하는 형태로 바꾼다.
3. 기존 메서드 선언을 참조하는 부분을 모두 찾아서 바뀐 형태로 수정한다.
4. 테스트 한다.

마이그레이션 절차
1. 이어지는 추출 단계를 수월하게 만들어야 한다면 함수의 본문을 적절히 리팩토링한다.
2. 함수 본문을 새로운 함수로 추출한다.
3. 추출한 함수에 매개변수를 추가해야 한다면 '간단한 절차'를 따라 추가한다.
4. 테스트한다.
5. 기존 함수를 인라인한다.
6. 이름을 임시로 붙여뒀다면 함수 선언 바꾸기를 한 번 더 적용해서 원래 이름으로 되돌린다.
7. 테스트한다.

## 6.6 변수 캡슐화하기

### 배경
변수를 직접 참조하는 코드가 너무 많아져서 변수를 읽거나 쓰는 코드를 모두 찾아서 수정하기 어려울 때가 있다. 이럴 때는 변수 캡슐화하기를
적용한다.

### 절차
1. 변수로의 접근과 갱신을 모두 제어하는 캡슐화 함수들을 만든다.
2. 정적 검사를 수행한다.
3. 변수를 직접 참조하던 부분을 모두 적절한 캡슐화 함수 호출로 바꾼다. 하나씩 바꿀 때마다 테스트한다.
4. 변수의 접근 범위를 제한한다.
5. 테스트한다.
6. 변수 값이 레코드라면 레코드 캡슐화하기를 적용할지 고려해본다.

## 6.7 변수 이름 바꾸기

### 배경
명확한 프로그래밍의 핵심은 이름짓기다. 변수는 프로그래머가 하려는 일에 관해 많은 것을 설명해준다. 특히 이름의 중요성은 그 사용 범위에 영향을
많이 받는다.

### 절차
1. 폭넓게 쓰이는 변수라면 변수 캡슐화하기를 고려한다.
2. 이름을 바꿀 변수를 참조하는 곳을 모두 찾아서, 하나씩 변경한다.
3. 테스트한다.
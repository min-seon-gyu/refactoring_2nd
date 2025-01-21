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

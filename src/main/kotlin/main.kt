import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.StringBuilder
import java.math.BigInteger

import kotlinx.coroutines.channels.produce

typealias InputChunnk = List<BigInteger>

var numDigitStart = 1

const val chunkSize = 100

const val workerCount = 100

data class BestPersistenceCandidateResult(val n: BigInteger, val persistence: Int, val digitCount: Int = n.toString(10).length) {

    companion object {
        val results: MutableMap<Int, BestPersistenceCandidateResult> = mutableMapOf()

        @Synchronized
        fun addResult(n: BigInteger, persistence: Int, digitCount: Int = n.toString(10).length): Boolean {
            if(results.containsKey(digitCount)){
                val currentBest = results[digitCount]!!
                if(currentBest.persistence < persistence
                    || (currentBest.persistence == persistence && n < currentBest.n)) {

                    results[digitCount] = BestPersistenceCandidateResult(n, persistence, digitCount)
                    return true
                }
            } else {
                results[digitCount] = BestPersistenceCandidateResult(n, persistence, digitCount)
                return true
            }
            return false
        }

        fun getBest(digitCount: Int): BestPersistenceCandidateResult {
            if(digitCount == 0) {
                return BestPersistenceCandidateResult(BigInteger.ZERO, 0, 0)
            }
            return if(results.containsKey(digitCount)) results[digitCount]!!
            else {
                val previousBest = getBest(digitCount - 1)
                addResult(previousBest.n, previousBest.persistence, previousBest.digitCount)
                previousBest
            }
        }
    }
}


fun main(args: Array<String>) {

    if(args.size !in 0..2) {
        println("Possible args are [any positive number for digit start count] and --singleThread")
    }
    var singleThread = false
    for(arg in args) {
        when {
            arg == "--singleThread" -> singleThread = true
            arg.toIntOrNull() != null -> numDigitStart = arg.toInt()
            else -> println("invalid argument: $arg")
        }
    }
    if (singleThread)
        mainSingleThread()
    else
        mainThreaded()
}

@OptIn(ObsoleteCoroutinesApi::class)
val updateCountDispatcher = newFixedThreadPoolContext(1, "update count thread")

@OptIn(ExperimentalCoroutinesApi::class)
fun mainThreaded() {

    var currentHighestDigitCount = 0

    fun CoroutineScope.launchWorker(chunks: ReceiveChannel<InputChunnk>) = launch {
        for(chunk in chunks) {
            for(n in chunk) {
                val numberString = n.toString(10)
                val digitCount = numberString.length

                if (digitCount > currentHighestDigitCount) {
                    launch(updateCountDispatcher) {
                        if (digitCount > currentHighestDigitCount) {
                            currentHighestDigitCount = digitCount
                            if (digitCount % 10 == 0 || digitCount > 50) {
                                println("Digit Count: $digitCount")
                            }
                        }
                    }
                }

                val persistence = calculatePersistence(n)

                val currentBest = BestPersistenceCandidateResult.getBest(digitCount)
                if (currentBest.persistence < persistence) {
                    if (BestPersistenceCandidateResult.addResult(n, persistence, digitCount)) {
                        println("New Best: Digit count: $digitCount, persistence: $persistence, n: $n")
                    }
                }
            }
        }
    }

    runBlocking {

        val candidates = produce {
            for(c in candidateSequenceGenerator(numDigitStart).chunked(chunkSize)) {
                send(c)
            }
        }

        repeat(workerCount) {
            launchWorker(candidates)
        }
    }
}

fun mainSingleThread() {

    var bestPersistence = -1
    var currentDigitCount = 0

    for (n in candidateSequenceGenerator(numDigitStart)) {
        val numberString = n.toString(10)
        val digitCount = numberString.length

        if(digitCount > currentDigitCount) {
            println("DigitCount: $digitCount")
            currentDigitCount = digitCount
        }

        val persistence = calculatePersistence(n)

        if(persistence > bestPersistence) {

            println("New best persistence: $persistence for $numberString")
            bestPersistence = persistence
        }
    }
}

val Boolean.i get() = if(this) 1 else 0
private val bOptions = setOf(true, false)
val Boolean.options get() = bOptions

val Int.countRange get() = if(this > 0) this downTo 0 else listOf(0)

fun candidateSequenceGenerator(startLength: Int): Sequence<BigInteger> = sequence {

    var targetLen = startLength

    while(true) {
        for(has2 in bOptions) {
            for(has3 in bOptions) {
                for(has4 in bOptions) {
                    for(has5 in bOptions) {
                        val tempLen = has2.i + has3.i + has4.i + has5.i
                        if(tempLen > targetLen) continue
                        var restLength = targetLen - tempLen
                        for(sixCount in restLength.countRange) {
                            restLength = targetLen - tempLen - sixCount
                            for(sevenCount in restLength.countRange) {
                                restLength = targetLen - tempLen - sixCount - sevenCount
                                for(eightCount in restLength.countRange) {
                                    restLength = targetLen - tempLen - sixCount - sevenCount - eightCount

                                    val nineCount = restLength

                                    if(has2 && (has3 || has4 || has5)) continue // combinations with 2 and 3,4 or 5 can be achieved with smaller numbers
                                    if(has3 && has4 && has5) continue // 3 * 4 * 5 == 12 * 5 => 0

                                    if(has5 && (sixCount > 0 || eightCount > 0)) continue // 5 * even => 0

                                    val builder = StringBuilder(targetLen)
                                    if(has2) builder.append("2")
                                    if(has3) builder.append("3")
                                    if(has4) builder.append("4")
                                    if(has5) builder.append("5")
                                    repeat(sixCount) { builder.append("6") }
                                    repeat(sevenCount) { builder.append("7") }
                                    repeat(eightCount) { builder.append("8") }
                                    repeat(nineCount) { builder.append("9") }

                                    val number = builder.toString()
                                    check(number.length == targetLen)
                                    yield(number.toBigInteger(10))
                                }
                            }
                        }
                    }
                }
            }
        }
        targetLen++
    }
}

fun calculatePersistence(n: Int): Int = calculatePersistence(n.toBigInteger())
fun calculatePersistence(n: Long): Int = calculatePersistence(n.toBigInteger())

tailrec fun calculatePersistence(n: BigInteger, count: Int = 0): Int {
    return if(n.toString(10).length == 1) count
    else {
        val n = n.toString(10).asSequence()
            .map { it.toString().toBigInteger(10) }
            .reduce { acc, i -> acc * i }
        calculatePersistence(n, count + 1)
    }
}


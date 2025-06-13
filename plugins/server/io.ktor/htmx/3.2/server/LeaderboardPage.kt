package com.example

import kotlinx.html.*
import io.ktor.htmx.html.*
import io.ktor.utils.io.ExperimentalKtorApi
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.random.Random

fun HTML.leaderboardPage(random: Random) {
    head {
        title("HTMX Example")
        script(src = "/web.js") {}
        link(rel = "stylesheet", href = "/leaderboard.css")
    }
    body {
        h1 {
            +"Leaderboard"
        }
        table {
            id = "leaderboard"
            thead {
                tr {
                    th { +"Alias" }
                    th { +"Score" }
                }
            }
            tbody {
                randomRows(random)
            }
        }
        h2 {
            id = "total-count"
            +"Total: 10"
        }
    }
}

fun TBODY.randomRows(random: Random) {
    for (contestant in generateSequence { random.nextContestant() }.take(10)) {
        tr {
            td { +contestant.alias }
            td { +contestant.score.toString() }
        }
    }
    loadMoreRows()
}

@OptIn(ExperimentalKtorApi::class)
fun TBODY.loadMoreRows() {
    tr {
        id = "replaceMe"
        td {
            colSpan = "3"
            style = "text-align: center;"

            button {
                attributes.hx {
                    get = "/more-rows"
                    target = "#replaceMe"
                    swap = "outerHTML"
                    trigger = "click"
                    select = "tr"
                }

                +"Load More..."
            }
        }
    }
}

private val dictionary: List<String> by lazy {
    listOf("/usr/share/dict/words", "/usr/dict/words").map(Paths::get).firstOrNull {
        it.exists()
    }?.let { dictionaryFile ->
        dictionaryFile.useLines(Charsets.UTF_8) { lines ->
            lines.filter { it.length >= 4 }.toList()
        }
    } ?: listOf("red", "blue", "green", "yellow", "pink", "violet", "black")
}

fun Random.nextContestant(): Contestant =
    Contestant(
        nextAlias(),
        nextInt(1_000_000)
    )

fun Random.nextAlias(): String =
    dictionary[nextInt(dictionary.size - 1)]

data class Contestant(
    val alias: String,
    val score: Int,
)

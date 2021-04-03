package nl.joerihofman.gettingtowikipedia

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

const val wikipediaUrl = "https://en.wikipedia.org"

val client = HttpClient(Apache)
val wikiPageUrlRegex: Regex = Regex("href=\"/wiki/[^Help:][^File:](?!#)[^\"]*") // Should not contain italic text or in-page references

fun main() {

    search("/wiki/Wikipedia:Getting_to_Philosophy")

    client.close()
}

private fun search(endpoint: String) {
    val requestBody = doRequest(endpoint)

    val title = findTitle(requestBody)

    if (title != "Philosophy") {
        val firstFoundLink = findFirstLink(requestBody)
        search(firstFoundLink)
    }
}

private fun doRequest(endpoint: String): String {
    val completeUrl = wikipediaUrl + endpoint

    println("Request to $completeUrl")

    return runBlocking {
        val response: HttpResponse

        try {
            response = client.request(completeUrl)
        } catch (e: ClientRequestException) {
            println("Something went wrong! Request to [$completeUrl] failed.")
            throw e
        }

        return@runBlocking response.readText()
    }
}

private fun findTitle(body: String): String {
    var title = body.substringAfter("<title>")

    title = title.substringBefore("</title>")
    title = title.substringBefore(" - Wikipedia") // This is always in the title

    return title.also { println("Title: [$it]") }
}

private fun findFirstLink(body: String) : String {
    val possibleLinks = mutableListOf<String>()

    if (body.contains("id=\"bodyContent\"")) {
        wikiPageUrlRegex.findAll(body).forEach {
            val possibleLink = it.value.substringAfter("href=\"")
            possibleLinks.add(possibleLink)
        }
    }

    println("Found possible first link [${possibleLinks[0]}]")

    return possibleLinks[0]
}

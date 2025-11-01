package er.codes.saltyvote.vote.model

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.jooq.tables.pojos.VoteOptions

private val objectMapper = ObjectMapper()

fun VoteOptions.getAirbnbLink(): String? {
    return try {
        val dataString = this.data?.data() ?: return null
        val jsonNode = objectMapper.readTree(dataString)
        jsonNode.get("airbnbLink")?.asText()
    } catch (_: Exception) {
        null
    }
}

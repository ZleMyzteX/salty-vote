package er.codes.saltyvote.history.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import er.codes.saltyvote.jooq.enums.ActionType
import org.jooq.JSONB
import java.util.UUID

abstract class HistoryEvent(
    val actionType: ActionType,
    val userId: UUID?,
    eventData: Any?,
) {
    val eventData: JSONB = JSONB.valueOf(mapper.writeValueAsString(eventData))

    abstract fun toLog(): String

    companion object {
        private val mapper =
            ObjectMapper().apply {
                registerModule(JavaTimeModule())
                registerModule(kotlinModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
    }
}

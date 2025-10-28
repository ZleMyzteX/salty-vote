package er.codes.saltyvote.history

import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.tables.daos.HistoryDao
import er.codes.saltyvote.jooq.tables.pojos.History
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class HistoryEventHandler(
    private val dao: HistoryDao,
) {
    private val log = KotlinLogging.logger { }

    @Async
    @EventListener
    fun handleHistoryEvent(event: HistoryEvent) {
        log.info { event.toLog() }
        dao.insert(
            History(
                actionType = event.actionType,
                userId = event.userId,
                eventData = event.eventData,
            ),
        )
    }
}

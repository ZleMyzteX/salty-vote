package er.codes.saltyvote.history.model.events

import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.enums.ActionType
import java.util.UUID

class UserLogInEvent(
    userId: UUID,
) : HistoryEvent(
        actionType = ActionType.USER_LOGIN,
        userId = userId,
        eventData = null,
    ) {
    override fun toLog() = "User $userId logged in"
}

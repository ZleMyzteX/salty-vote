package er.codes.saltyvote.history.model.events

import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.enums.ActionType
import java.util.UUID

class UserRegisteredEvent(
    user: UserEntity,
) : HistoryEvent(
        actionType = ActionType.USER_REGISTERED,
        userId = user.id,
        eventData = UserRegisteredEventData(user.id, user.username, user.email),
    ) {
    override fun toLog() = "User $userId registered."

    private data class UserRegisteredEventData(
        val userId: UUID,
        val userName: String,
        val email: String,
    )
}

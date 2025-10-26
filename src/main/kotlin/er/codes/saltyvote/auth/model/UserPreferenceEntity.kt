package er.codes.saltyvote.auth.model

import java.util.UUID

data class UserPreferenceEntity(
    val userId: UUID,
    val currentContextId: Int,
)

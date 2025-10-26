package er.codes.saltyvote.auth.util

import er.codes.saltyvote.auth.model.UserEntity
import org.springframework.security.access.AccessDeniedException

fun checkIfAdminOrThrow(user: UserEntity) {
    if (!user.admin) {
        throw AccessDeniedException("Only administrators can perform this action")
    }
}

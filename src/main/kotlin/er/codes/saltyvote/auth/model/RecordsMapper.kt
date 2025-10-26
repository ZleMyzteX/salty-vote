package er.codes.saltyvote.auth.model

import er.codes.saltyvote.jooq.tables.records.UsersRecord

fun UsersRecord.toEntity() =
    UserEntity(
        id = this.uuid!!,
        email = this.email!!,
        username = this.username!!,
        passwordHash = this.password!!,
        admin = this.admin!!,
        enabled = this.enabled!!,
    )


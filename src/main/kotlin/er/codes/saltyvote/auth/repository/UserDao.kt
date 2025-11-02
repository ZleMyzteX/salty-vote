package er.codes.saltyvote.auth.repository

import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.auth.model.toEntity
import er.codes.saltyvote.jooq.tables.Users.Companion.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class UserDao(
    private val dslContext: DSLContext,
) {
    fun createUser(
        username: String,
        passwordHash: String,
    ): UserEntity {
        val userId = UUID.randomUUID()
        val email = "not-used-currently"
        val record =
            dslContext
                .insertInto(USERS)
                .set(USERS.UUID, userId)
                .set(USERS.USERNAME, username)
                .set(USERS.PASSWORD, passwordHash)
                .set(USERS.EMAIL, email)
                .set(USERS.ADMIN, false)
                .set(USERS.ENABLED, true)
                .returning()
                .fetchOne() ?: throw IllegalStateException("Failed to insert user $username")

        return record.into(USERS).toEntity()
    }

    fun findById(userId: UUID): UserEntity? =
        dslContext
            .selectFrom(USERS)
            .where(USERS.UUID.eq(userId))
            .fetchAny()
            ?.toEntity()

    fun findById(userId: String): UserEntity? = findById(UUID.fromString(userId))

    fun findByUsername(username: String): UserEntity? =
        dslContext
            .selectFrom(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOne()
            ?.toEntity()

    fun findAll(): List<UserEntity> =
        dslContext
            .selectFrom(USERS)
            .orderBy(USERS.CREATED_AT.desc())
            .fetch()
            .map { it.toEntity() }

    fun findAllAdminUsers(): List<UserEntity> =
        dslContext
            .selectFrom(USERS)
            .where(USERS.ADMIN.eq(true))
            .and(USERS.ENABLED.eq(true))
            .orderBy(USERS.CREATED_AT.desc())
            .fetch()
            .map { it.toEntity() }
}

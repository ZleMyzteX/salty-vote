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
        email: String,
    ): UserEntity {
        val userId = UUID.randomUUID()
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

    fun findById(id: Int): UserEntity? = throw NotImplementedError("Use findUserById with UUID instead of Int")

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

    fun findByEmail(email: String): UserEntity? =
        dslContext
            .selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne()
            ?.toEntity()

    fun findAll(): List<UserEntity> =
        dslContext
            .selectFrom(USERS)
            .orderBy(USERS.CREATED_AT.desc())
            .fetch()
            .map { it.toEntity() }

    fun findAllEnabledUsers(): List<UserEntity?> =
        dslContext
            .selectFrom(USERS)
            .where(USERS.ENABLED.eq(true))
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

    fun updateUserPassword(
        userId: UUID,
        newPasswordHash: String,
    ): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.PASSWORD, newPasswordHash)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun updateUserEmail(
        userId: UUID,
        newEmail: String,
    ): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.EMAIL, newEmail)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun updateUserUsername(
        userId: UUID,
        newUsername: String,
    ): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.USERNAME, newUsername)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun enableUser(userId: UUID): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.ENABLED, true)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun disableUser(userId: UUID): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.ENABLED, false)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun makeUserAdmin(userId: UUID): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.ADMIN, true)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun removeUserAdmin(userId: UUID): Boolean {
        val updatedRows =
            dslContext
                .update(USERS)
                .set(USERS.ADMIN, false)
                .set(USERS.UPDATED_AT, OffsetDateTime.now())
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }

    fun deleteUser(userId: UUID): Boolean {
        val deletedRows =
            dslContext
                .deleteFrom(USERS)
                .where(USERS.UUID.eq(userId))
                .execute()
        return deletedRows > 0
    }

    fun userExistsByUsername(username: String): Boolean =
        dslContext
            .selectCount()
            .from(USERS)
            .where(USERS.USERNAME.eq(username))
            .fetchOne(0, Int::class.java)
            ?.let { it > 0 } ?: false

    fun userExistsByEmail(email: String): Boolean =
        dslContext
            .selectCount()
            .from(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne(0, Int::class.java)
            ?.let { it > 0 } ?: false

    fun isUserEnabled(userId: UUID): Boolean =
        dslContext
            .select(USERS.ENABLED)
            .from(USERS)
            .where(USERS.UUID.eq(userId))
            .fetchOne()
            ?.value1() ?: false

    fun isUserAdmin(userId: UUID): Boolean =
        dslContext
            .select(USERS.ADMIN)
            .from(USERS)
            .where(USERS.UUID.eq(userId))
            .fetchOne()
            ?.value1() ?: false

    fun getUserCount(): Int =
        dslContext
            .selectCount()
            .from(USERS)
            .fetchOne(0, Int::class.java) ?: 0

    fun getEnabledUserCount(): Int =
        dslContext
            .selectCount()
            .from(USERS)
            .where(USERS.ENABLED.eq(true))
            .fetchOne(0, Int::class.java) ?: 0

    fun getAdminUserCount(): Int =
        dslContext
            .selectCount()
            .from(USERS)
            .where(USERS.ADMIN.eq(true))
            .fetchOne(0, Int::class.java) ?: 0

    fun searchUsersByUsername(usernamePattern: String): List<UserEntity?> =
        dslContext
            .selectFrom(USERS)
            .where(USERS.USERNAME.likeIgnoreCase("%$usernamePattern%"))
            .orderBy(USERS.CREATED_AT.desc())
            .fetch()
            .map { it.toEntity() }

    fun updateUser(
        userId: UUID,
        username: String?,
        email: String?,
        passwordHash: String?,
        admin: Boolean?,
        enabled: Boolean?,
    ): Boolean {
        val updates = mutableMapOf<org.jooq.Field<*>, Any?>()

        username?.let { updates[USERS.USERNAME] = it }
        email?.let { updates[USERS.EMAIL] = it }
        passwordHash?.let { updates[USERS.PASSWORD] = it }
        admin?.let { updates[USERS.ADMIN] = it }
        enabled?.let { updates[USERS.ENABLED] = it }

        updates[USERS.UPDATED_AT] = OffsetDateTime.now()

        if (updates.isEmpty()) return false

        val updatedRows =
            dslContext
                .update(USERS)
                .set(updates)
                .where(USERS.UUID.eq(userId))
                .execute()
        return updatedRows > 0
    }
}

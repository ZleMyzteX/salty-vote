package er.codes.saltyvote.auth.service

import er.codes.saltyvote.auth.repository.UserDao
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userDao: UserDao,
) {
    fun getAllUsers() = userDao.findAll().map { it.toDto() }

    fun getAllAdmins() = userDao.findAllAdminUsers().map { it.toDto() }
}

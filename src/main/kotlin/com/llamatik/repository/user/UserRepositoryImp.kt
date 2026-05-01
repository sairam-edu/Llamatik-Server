package com.llamatik.repository.user

import com.llamatik.models.DatabaseUser
import com.llamatik.repository.DatabaseFactory.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class UserRepositoryImp : UserRepository {
    override suspend fun addUser(
        email: String,
        name: String,
        passwordHash: String
    ): DatabaseUser? {
        // Previously used a nullable `var` captured from outside the lambda, which forced
        // an extra heap allocation and relied on a side-effect across a suspend boundary.
        // Returning the InsertStatement directly from dbQuery is both safer and avoids the
        // extra allocation.
        val statement = dbQuery {
            Users.insert { user ->
                user[Users.email] = email
                user[Users.name] = name
                user[Users.passwordHash] = passwordHash
            }
        }

        return rowToUser(statement.resultedValues?.get(0))
    }

    override suspend fun findUser(userId: Int) = dbQuery {
        // select(Users.userId) only projected the primary-key column, causing rowToUser to
        // throw when it accessed email/name/passwordHash.  selectAll() fetches every column
        // once, eliminating the partial-projection bug and the need for a follow-up query.
        Users.selectAll().where { Users.userId.eq(userId) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = dbQuery {
        // Same fix as findUser: select(Users.email) was a partial projection that crashed
        // rowToUser.  selectAll() is correct here because all columns are needed.
        Users.selectAll().where { Users.email.eq(email) }
            .map { rowToUser(it) }.singleOrNull()
    }

    private fun rowToUser(row: ResultRow?): DatabaseUser? {
        if (row == null) {
            return null
        }
        return DatabaseUser(
            userId = row[Users.userId],
            email = row[Users.email],
            name = row[Users.name],
            passwordHash = row[Users.passwordHash]
        )
    }
}

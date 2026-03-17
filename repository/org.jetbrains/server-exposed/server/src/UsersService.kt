package kastle

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.UIntIdTable
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

@Serializable
data class ExposedUser(val name: String, val age: Int)

class ExposedUserService(val database: R2dbcDatabase) {
    object Users : UIntIdTable() {
        val name = varchar("name", length = 50)
        val age = integer("age")
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: ExposedUser): UInt = suspendTransaction(database) {
        val newRecord = Users.insert {
            it[name] = user.name
            it[age] = user.age
        }
        newRecord[Users.id].value
    }

    suspend fun read(id: UInt): ExposedUser? {
        return suspendTransaction(database) {
            Users.selectAll()
                .where { Users.id eq id }
                .map { ExposedUser(it[Users.name], it[Users.age]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: UInt, user: ExposedUser) {
        suspendTransaction(database) {
            Users.update({ Users.id eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    suspend fun delete(id: UInt) {
        suspendTransaction(database) { Users.deleteWhere { Users.id.eq(id) } }
    }

}

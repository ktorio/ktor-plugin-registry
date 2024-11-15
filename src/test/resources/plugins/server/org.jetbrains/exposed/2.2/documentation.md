
Exposed is a lightweight SQL library on top of [JDBC](https://en.wikipedia.org/wiki/Java_Database_Connectivity) driver for Kotlin language. Exposed has two flavors of database access: typesafe SQL wrapping DSL and lightweight Data Access Objects (DAO).

With Exposed, you can have two levels of databases Access. You would like to use exposed because the database access includes wrapping DSL and a lightweight data access object. Exposed can be used to help you build applications without dependencies on any specific database engine and switch between them with very little or no changes.

## Usage

### Creating a table
```kotlin
object Users : Table() {
    val id = varchar("id", 10) // Column<String>
    val name = varchar("name", length = 50) // Column<String>
    val cityId = (integer("city_id") references Cities.id).nullable() // Column<Int?>

    override val primaryKey = PrimaryKey(id, name = "PK_User_ID") // name is optional here
}
```
The above code snippet defines a table called "Users" with three columns: `id`, `name``, and `cityId``. The primary key for the table is defined as the `id` column, and the references function is used to define a foreign key constraint between the `cityId` column and the `id` column of the `Cities` table [(see full example)](https://github.com/JetBrains/Exposed).
### Connecting to the database
```kotlin
Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver", user = "root", password = "")
```
The above code snippet connects to the database using the `Database.connect()` function, which takes the [JDBC](https://en.wikipedia.org/wiki/Java_Database_Connectivity) connection string, the `driver` class, and the `user` and `password` as its parameters. You can use any other database by changing the connection string and driver accordingly.
### Performing a transaction
```kotlin
transaction {
    Users.insert {
        it[name] = user.name
        it[age] = user.age
    }
}
```
The above code snippet shows how to perform a transaction using the `transaction` function. Inside the `transaction` block, you can perform any database operations

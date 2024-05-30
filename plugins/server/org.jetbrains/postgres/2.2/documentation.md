
PostgreSQL is a powerful and open-source relational database management system (RDBMS) that is widely used in enterprise and web applications. It supports a wide range of data types, has powerful SQL capabilities, and offers robust performance and reliability.

## Usage

You can use the standard [JDBC](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) interfaces to establish a connection to the database and execute queries.
```kotlin
val dbConnection: Connection = DriverManager.getConnection(
    "jdbc:postgresql://<host>:<port>/<database>",
    "<username>", "<password>"
)
```
Then you may want to execute some requests to the database from Ktor endpoints:
```kotlin
get("/db/select/users") {
    val statement = dbConnection.createStatement()
    val resultSet = statement.executeQuery("SELECT name FROM users")
    val userNames = mutableListOf<String>()
    while (resultSet.next()) {
        userNames += resultSet.getString("name")
    }
    call.respond(userNames)
}
```


MongoDB is a powerful, flexible, and scalable document-oriented database that allows you to store your data in an efficient way. 
It uses a JSON-like document model for storing objects.

## Usage

In order to connect to the database, you have to use `MongoClients` interface:

```kotlin
val uri = "mongodb://127.0.0.1:27017/?maxPoolSize=20&w=majority"
val mongoClient = MongoClients.create(uri)
val database = mongoClient.getDatabase(databaseName)
```

Then, you have to create the collection of the documents in the database:

```kotlin
database.createCollection("users")
val collection = database.getCollection("users")
```
And after that work is done, you could put some data to the collection. 
The data needs to be stored in `org.bson.Document`:

```kotlin
val doc = Document(mapOf("name" to "John", "secondName" to "Smith", "age" to 30))
val id = collection.insertOne(doc)
```

Then you can always get the inserted data using a filter:

```kotlin
val user = collection.find(Filters.eq("_id", id)).first()
```
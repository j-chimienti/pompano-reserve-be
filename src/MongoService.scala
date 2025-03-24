class MongoService(uri: String, db: String) {
  import org.mongodb.scala._

  val client: MongoClient = MongoClient(uri)

  val database: MongoDatabase = client.getDatabase(db)
  val collection: MongoCollection[Document] = database.getCollection("reservations")

}
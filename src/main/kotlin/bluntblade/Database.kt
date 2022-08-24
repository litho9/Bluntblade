package bluntblade

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.*

val client = KMongo.createClient()
val db: MongoDatabase = client.getDatabase("bluntblade") //normal java driver usage
val col = db.getCollection<Account>()

class Database {
}
package sg.edu.nus.iss.order_service.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MongoManager {
    private static final Logger log = LoggerFactory.getLogger(MongoManager.class);

    public MongoClient getMongoClient(String dbName) {
        return MongoSingleton.getMongoClient(dbName);
    }

    /**
     * Find a document in a specific collection within a database
     *
     * @param query          The query document to find the matching document
     * @param dbName         The name of the database
     * @param collectionName The name of the collection
     * @return The found document, or null if no match is found
     */
    public Document findDocument(Document query, String dbName, String collectionName) {
        try{
            MongoClient mongoClient = getMongoClient(dbName);
            if(mongoClient==null){
                log.error("findDocument :: Unable to get mongo client for DB : {} and coll : {}", dbName, collectionName);
                return null;
            }
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            return collection.find(query).first();
        } catch(Exception ex){
            log.error("findDocument :: Exception occurred while finding document in collection: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Update a document in a specific collection within a database
     *
     * @param query          The query to find the document to update
     * @param update         The update to apply to the document
     * @param dbName         The name of the database
     * @param collectionName The name of the collection
     * @return The result of the update operation
     */
    public Document findOneAndUpdate(Document query, Document update, String dbName, String collectionName,
                                     boolean upsert, boolean returnUpdatedDoc) {
        try{
            MongoClient mongoClient = getMongoClient(dbName);
            if(mongoClient==null){
                log.error("updateDocument :: Unable to get mongo client for DB : {} and coll : {}", dbName, collectionName);
                return null;
            }
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
            options.returnDocument(returnUpdatedDoc ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
            options.upsert(upsert);

            return collection.findOneAndUpdate(query, update, options);
        }catch(Exception ex) {
            log.error("updateDocument :: Exception occurred while updating document in collection: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Delete a document in a specific collection within a database
     *
     * @param query          The query to find the document to delete
     * @param dbName         The name of the database
     * @param collectionName The name of the collection
     * @return The result of the delete operation
     */
    public boolean deleteDocument(Document query, String dbName, String collectionName) {
        try{
            MongoClient mongoClient = getMongoClient(dbName);
            if(mongoClient==null){
                log.error("deleteDocument :: Unable to get mongo client for DB : {} and coll : {}", dbName, collectionName);
                return false;
            }
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            return collection.deleteOne(query).wasAcknowledged();
        }catch(Exception ex){
            log.error("deleteDocument :: Exception occurred while deleting document in collection: {}", ex.getMessage());
            return false;
        }
    }

    public boolean insertDocument(Document document, String dbName, String collectionName) {
        try{
            MongoClient mongoClient = getMongoClient(dbName);
            if(mongoClient==null){
                log.error("insertDocument :: Unable to get mongo client for DB : {} and coll : {}", dbName, collectionName);
                return false;
            }
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            return collection.insertOne(document).wasAcknowledged();
        }catch(Exception ex){
            log.error("insertDocument :: Exception occurred while inserting document in collection: {}", ex.getMessage());
            return false;
        }
    }

    public List<Document> findAllDocuments(Document query, String dbName, String collectionName) {
        try{
            MongoClient mongoClient = getMongoClient(dbName);
            if (mongoClient == null) {
                log.error("findAllDocuments :: Unable to get mongo client for DB : {} and coll : {}", dbName, collectionName);
                return null;
            }
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            List<Document> documents = new ArrayList<>();
            try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
                while (cursor.hasNext()) {
                    documents.add(cursor.next());
                }
            }
            return documents;
        }catch(Exception ex){
            log.error("findAllDocuments :: Exception occurred while finding all documents in collection: {}", ex.getMessage());
            return null;
        }
    }
}

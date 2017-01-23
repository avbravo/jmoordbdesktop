/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.facade;

import com.avbravo.jmoordb.interfaces.DatabaseInterface;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

/**
 *
 * @author avbravo
 * @param <T>
 */
public abstract class DatabaseImplement<T> implements DatabaseInterface<T> {
Exception exception  = new Exception();
 private Class<T> entityClass;
    private String database;
    private String collection;
    //lazy load
    private Boolean lazy;
     protected abstract MongoClient getMongoClient();
    /**
     * 
     * @param db
     * @return 
     */



    public DatabaseImplement(Class<T> entityClass, String database, String collection, Boolean... lazy){
        this.entityClass = entityClass;
        this.database = database;
        this.collection = collection;
        Boolean l = false;
        if (lazy.length != 0) {
            l = lazy[0];

        }
        this.lazy = l;
    }

     public MongoDatabase getDB() {
        MongoDatabase db = getMongoClient().getDatabase(database);
        return db;
    }
    public List<String> listCollecctionsT() {
        List<String> list = new ArrayList<>();
        try {
            for (Document name : getDB().listCollections()) {
                list.add(name.get("name").toString());
            }
            
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "listCollecctions()").log(Level.SEVERE, null, e);
            exception = new Exception("listCollecctions() ", e);
        }
        return list;
    }
}

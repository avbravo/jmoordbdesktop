/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.mongodb.interfaces;

import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import org.bson.Document;

/**
 *
 * @author avbravo
 */
public interface AbstractInterface <T>{
     public MongoDatabase getMongoDatabase() ;
//     public Document findOne();
      public T findById(String key, String value);
      public T findById(String key, Integer value);
      
     public T find(String key, Object value);
//     public T find(String key, Integer value);
     public T find(Document document);
     
   //  public Boolean save(T t,Boolean... verifyID);
     
//     public Boolean save(T t);
//     public Document toDocument(T t);
   
     
}

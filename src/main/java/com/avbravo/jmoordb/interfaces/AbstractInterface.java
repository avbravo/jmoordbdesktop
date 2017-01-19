/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.interfaces;

import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import org.bson.Document;

/**
 *
 * @author avbravo
 */
public interface AbstractInterface <T>{
     public MongoDatabase getDB() ;
//     public Document findOne();
      public T findById(String key, String value);
     public T find(String key, Object value);
     
//     public Boolean save(T t);
//     public Document toDocument(T t);
   
     
}

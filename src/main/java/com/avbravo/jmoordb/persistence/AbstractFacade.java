/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.persistence;

import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.PrimaryKey;
import com.avbravo.jmoordb.interfaces.AbstractInterface;
import com.avbravo.jmoordb.util.Util;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author avbravo
 * @param <T>
 */
public abstract class AbstractFacade<T> implements AbstractInterface{
private Class<T> entityClass;
    private String database;
    private String collection;
    //lazy load
    private Boolean lazy;
    List<T> list = new ArrayList<>();
    List<PrimaryKey> primaryKeyList = new ArrayList<>();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    Exception exception;
    Util util = new Util();
    protected abstract MongoClient getMongoClient();

    Integer contador = 0;
    @Override
    public MongoDatabase getDB() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boolean save(Object t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}

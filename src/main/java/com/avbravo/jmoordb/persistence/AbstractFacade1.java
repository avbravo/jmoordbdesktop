/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.persistence;

import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.FieldBeans;
import com.avbravo.jmoordb.PrimaryKey;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.anotations.Embedded;
import com.avbravo.jmoordb.anotations.Id;
import com.avbravo.jmoordb.anotations.Referenced;
import com.avbravo.jmoordb.interfaces.AbstractInterface;
import com.avbravo.jmoordb.internal.ClassDescriptorsCache;
import com.avbravo.jmoordb.util.Util;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
public abstract class AbstractFacade1<T> implements AbstractInterface {
 private ClassDescriptorsCache cache = new ClassDescriptorsCache();
    private Class<T> entityClass;
    private String database;
    private String collection;
    //lazy load
    private Boolean lazy;
    List<T> list = new ArrayList<>();
    List<PrimaryKey> primaryKeyList = new ArrayList<>();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<ReferencedBeans> referencedBeansList = new ArrayList<>();
    List<FieldBeans> fieldBeansList = new ArrayList<>();
    Exception exception;
    Util util = new Util();

    protected abstract MongoClient getMongoClient();

    Integer contador = 0;

    public AbstractFacade1(Class<T> entityClass, String database, String collection, Boolean... lazy) {
        this.entityClass = entityClass;
        this.database = database;
        this.collection = collection;
        Boolean l = false;
        if (lazy.length != 0) {
            l = lazy[0];

        }
        this.lazy = l;

        primaryKeyList = new ArrayList<>();
        embeddedBeansList = new ArrayList<>();
        referencedBeansList = new ArrayList<>();
        fieldBeansList = new ArrayList<>();
        /**
         * lee las anotaciones @Id para obtener los PrimaryKey del documento
         */
        final Field[] variables = entityClass.getDeclaredFields();
        for (final Field variable : variables) {
            final Annotation anotacion = variable.getAnnotation(Id.class);
            final Annotation anotacionEmbedded = variable.getAnnotation(Embedded.class);
            final Annotation anotacionReferenced = variable.getAnnotation(Referenced.class);
            variable.setAccessible(true);

            FieldBeans fieldBeans = new FieldBeans();
            fieldBeans.setIsKey(false);
            fieldBeans.setIsEmbedded(false);
            fieldBeans.setIsReferenced(false);
            fieldBeans.setName(variable.getName());
            fieldBeans.setType(variable.getType().getName());

            //PrimaryKey
            if (anotacion != null) {
                verifyPrimaryKey(variable, anotacion);
                fieldBeans.setIsKey(true);

            }
            if (anotacionEmbedded != null) {
                verifyEmbedded(variable, anotacion);
                fieldBeans.setIsEmbedded(true);

            }
            if (anotacionEmbedded != null) {
                verifyEmbedded(variable, anotacion);
                fieldBeans.setIsEmbedded(true);

            }
            if (anotacionReferenced != null) {
                verifyReferenced(variable, anotacion);
                fieldBeans.setIsReferenced(true);

            }

            fieldBeansList.add(fieldBeans);

            /**
             * carga los documentos embebidos
             */
        }
        //Llave primary
        if (primaryKeyList.isEmpty()) {
            exception = new Exception("No have primaryKey() ");

        }
        if (fieldBeansList.isEmpty()) {
            exception = new Exception("No have fields() ");
        }
//        for (EmbeddedBeans e : embeddedBeansList) {
//            System.out.println("--> embebido " + e.toString());
//        }

    }

    /**
     *
     * @param variable
     * @param anotacion
     * @return
     */
    private Boolean verifyPrimaryKey(Field variable, Annotation anotacion) {
        try {
            final Id anotacionPK = (Id) anotacion;
            PrimaryKey primaryKey = new PrimaryKey();

            Boolean found = false;
            for (PrimaryKey pk : primaryKeyList) {
                if (pk.getName().equals(primaryKey.getName())) {
                    found = true;
                }
            }

            primaryKey.setName(variable.getName());
            primaryKey.setType(variable.getType().getName());

            // obtengo el valor del atributo
            if (!found) {
                primaryKeyList.add(primaryKey);
            }
            return true;
        } catch (Exception e) {
            System.out.println("verifyPrimaryKey() " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param variable
     * @param anotacion
     * @return
     */
    private Boolean verifyEmbedded(Field variable, Annotation anotacion) {
        try {
            // final Embedded anotacionPK = (Embedded) anotacionEmbedded;
            EmbeddedBeans embeddedBeans = new EmbeddedBeans();
            embeddedBeans.setName(variable.getName());
            embeddedBeans.setType(variable.getType().getName());
            embeddedBeansList.add(embeddedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyPrimaryKey() " + e.getLocalizedMessage());
        }
        return false;
    }

    private Boolean verifyReferenced(Field variable, Annotation anotacion) {
        try {
            // final Embedded anotacionPK = (Embedded) anotacionEmbedded;
            ReferencedBeans referencedBeans = new ReferencedBeans();
            referencedBeans.setName(variable.getName());
            referencedBeans.setType(variable.getType().getName());
            referencedBeansList.add(referencedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyPrimaryKey() " + e.getLocalizedMessage());
        }
        return false;
    }

    
    @Override
    public MongoDatabase getDB() {
        MongoDatabase db = getMongoClient().getDatabase(database);
        return db;
    }

//    @Override
    public Boolean save(T t) {
        try {
         

            getDB().getCollection(collection).insertOne(getDocument(t));
        return true;
        } catch (Exception ex) {
            Logger.getLogger(AbstractFacade1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * 
     * @param t2
     * @return devuelve el Pojo convertido a documento.
     */
    public Document getDocument(T t2) {  
        Document doc = new Document();
        try {
   
            for (FieldBeans f : fieldBeansList) {
                
                if(f.getIsEmbedded()){
                    //hay que crearlo en el documento
                }
                else{
                    if (f.getIsReferenced()){
                        // solo debe buscar la referencia en la anotacion del otro beans.
                      
                    }else{
                         doc.append(f.getName(), getReflectionValue(f.getName(), t2 ));
                    }
                }
               
            }

        } catch (Exception ex) {
            exception = ex;
        }
        //   doc.toBsonDocument(t, codecRegistry);
        return doc;
    }

    
    @Override
    public Object findById(Object t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param name campo del entity que sera convertido a getCampo y se invocara
     * @param t2
     * @return un object correspondiente a la ejecucion del metodo get
     */
    public Object getReflectionValue(String name, T t2) {
        Object v = new Object();
        
         String getName = "get" + util.letterToUpper(name);
        try {
            Object t = entityClass.newInstance();

            Method method;
            try {
              Class noparams[] = {};
                method = entityClass.getDeclaredMethod(getName,noparams);
                 v = method.invoke(t2);
                 return v;
            } catch (Exception e) {
                Logger.getLogger(AbstractFacade1.class.getName()).log(Level.SEVERE, null, e);
                exception = new Exception("getReflectionValue() ", e);
            }

           
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade1.class.getName() + "getReflectionValue()").log(Level.SEVERE, null, e);
            exception = new Exception("getReflectionValue)) ", e);
        }
        System.out.println("getName() "+getName  + " value "+v);
        return v;
    }

}

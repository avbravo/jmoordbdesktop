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
import com.avbravo.jmoordb.anotations.Entity;
import com.avbravo.jmoordb.anotations.Id;
import com.avbravo.jmoordb.anotations.Referenced;
import com.avbravo.jmoordb.interfaces.AbstractInterface;
import com.avbravo.jmoordb.interfaces.FindInterface;
import com.avbravo.jmoordb.internal.JavaToDocument;
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
public abstract class AbstractFacade<T> implements AbstractInterface, FindInterface {

    JavaToDocument javaToDocument = new JavaToDocument();

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

    public AbstractFacade(Class<T> entityClass, String database, String collection, Boolean... lazy) {
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
        /**
         * Descompone la anotacion entity
         */
//            Entity entity = entityClass.getClass().getAnnotation(Entity.class);
//System.out.println("Descomponiendo el entity");
//System.out.println("document: " + entity.document());
//System.out.println("lazy:" + entity.lazy());
//System.out.println("dateformat:" + entity.dateformat());

        final Field[] fields = entityClass.getDeclaredFields();
        for (final Field field : fields) {
            Annotation anotacion = field.getAnnotation(Id.class);
            Annotation anotacionEmbedded = field.getAnnotation(Embedded.class);
            Annotation anotacionReferenced = field.getAnnotation(Referenced.class);

            Embedded embedded = field.getAnnotation(Embedded.class);
            Referenced referenced = field.getAnnotation(Referenced.class);

            field.setAccessible(true);

            FieldBeans fieldBeans = new FieldBeans();
            fieldBeans.setIsKey(false);
            fieldBeans.setIsEmbedded(false);
            fieldBeans.setIsReferenced(false);
            fieldBeans.setName(field.getName());
            fieldBeans.setType(field.getType().getName());

            //PrimaryKey
            if (anotacion != null) {
                verifyPrimaryKey(field, anotacion);
                fieldBeans.setIsKey(true);

            }
            if (anotacionEmbedded != null) {

                verifyEmbedded(field, anotacionEmbedded);
                fieldBeans.setIsEmbedded(true);

            }
//            if (anotacionEmbedded != null) {
//                verifyEmbedded(variable, anotacion);
//                fieldBeans.setIsEmbedded(true);
//
//            }
            if (anotacionReferenced != null) {

                verifyReferenced(field, anotacionReferenced, referenced);
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
            System.out.println("verifyEmbedded() " + e.getLocalizedMessage());
        }
        return false;
    }
/**
 * guarda la informacion de la anotacion
 * @param variable
 * @param anotacion
 * @param referenced
 * @return 
 */
    private Boolean verifyReferenced(Field variable, Annotation anotacion, Referenced referenced) {
        try {

//  Referenced anotacionRef = (Referenced) anotacion;
            ReferencedBeans referencedBeans = new ReferencedBeans();
            referencedBeans.setName(variable.getName());
            referencedBeans.setType(variable.getType().getName());
            referencedBeans.setDocument(referenced.documment());
            referencedBeans.setField(referenced.field());
          
            referencedBeansList.add(referencedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyReferenced() " + e.getLocalizedMessage());
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

            //  getDB().getCollection(collection).insertOne(getDocument(t));
            getDB().getCollection(collection).insertOne(toDocument(t));
            return true;
        } catch (Exception ex) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     *
     * @param t2
     * @return devuelve el Pojo convertido a documento.
     */
  
    public Document toDocument(Object t) {
        return javaToDocument.toDocument(t, embeddedBeansList, referencedBeansList);
    }

    @Override
    public Object findById(Object t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   
}

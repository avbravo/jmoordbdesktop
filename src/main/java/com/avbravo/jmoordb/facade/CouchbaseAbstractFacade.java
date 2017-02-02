/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.facade;

import com.avbravo.jmoordb.DatePatternBeans;
import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.FieldBeans;
import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.PrimaryKey;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.anotations.DatePattern;
import com.avbravo.jmoordb.anotations.Embedded;
import com.avbravo.jmoordb.anotations.Id;
import com.avbravo.jmoordb.anotations.Referenced;
import com.avbravo.jmoordb.interfaces.AbstractInterface;
import com.avbravo.jmoordb.interfaces.CouchbaseAbstractInterface;
import com.avbravo.jmoordb.internal.DocumentToJava;
import com.avbravo.jmoordb.internal.JavaToDocument;
import com.avbravo.jmoordb.util.Util;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.Function;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.descending;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 *
 * @author avbravo
 * @param <T>
 */
public abstract class CouchbaseAbstractFacade<T> implements CouchbaseAbstractInterface {
protected abstract Cluster getCluster();
    private JavaToDocument javaToDocument = new JavaToDocument();
    private DocumentToJava documentToJava = new DocumentToJava();
    T t1, tlocal;
    private Class<T> entityClass;
    private String database;
    private String collection;
    private Boolean haveElements = false;
    //lazy load
    private Boolean lazy;
    List<T> list = new ArrayList<>();
    List<PrimaryKey> primaryKeyList = new ArrayList<>();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<ReferencedBeans> referencedBeansList = new ArrayList<>();
    List<DatePatternBeans> datePatternBeansList = new ArrayList<>();
    List<FieldBeans> fieldBeansList = new ArrayList<>();
    Exception exception;
    Util util = new Util();

       

    Integer contador = 0;

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public CouchbaseAbstractFacade(Class<T> entityClass, String database, String collection, Boolean... lazy) {
//        databaseImplement = new DatabaseImplement
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
        datePatternBeansList = new ArrayList<>();
        fieldBeansList = new ArrayList<>();
        /**
         * lee las anotaciones @Id para obtener los PrimaryKey del documento
         */
        /**
         * Descompone la anotacion entity
         */

        final Field[] fields = entityClass.getDeclaredFields();
        for (final Field field : fields) {
            Annotation anotacion = field.getAnnotation(Id.class);
            Annotation anotacionEmbedded = field.getAnnotation(Embedded.class);
            Annotation anotacionReferenced = field.getAnnotation(Referenced.class);
            Annotation anotacionDateFormat = field.getAnnotation(DatePattern.class);

            Embedded embedded = field.getAnnotation(Embedded.class);
            Referenced referenced = field.getAnnotation(Referenced.class);
            DatePattern datePattern = field.getAnnotation(DatePattern.class);

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

            if (anotacionReferenced != null) {

                verifyReferenced(field, anotacionReferenced, referenced);
                fieldBeans.setIsReferenced(true);

            }
            if (anotacionDateFormat != null) {

                verifyDatePattern(field, anotacionReferenced, datePattern);
                //fieldBeans.setIsReferenced(true);

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

    }

    
     @Override
    public Bucket getBucket() {
        Bucket bucket  = getCluster().openBucket(database);
        return bucket ;
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
     *
     * @param variable
     * @param anotacion
     * @param referenced
     * @return
     */
    private Boolean verifyReferenced(Field variable, Annotation anotacion, Referenced referenced) {
        try {

            ReferencedBeans referencedBeans = new ReferencedBeans();
            referencedBeans.setName(variable.getName());
            referencedBeans.setType(variable.getType().getName());
            referencedBeans.setDocument(referenced.documment());
            referencedBeans.setField(referenced.field());
            referencedBeans.setJavatype(referenced.javatype());
            referencedBeans.setFacade(referenced.facade());
            referencedBeans.setLazy(referenced.lazy());

            referencedBeansList.add(referencedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyReferenced() " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     *
     * @param variable
     * @param anotacion
     * @param referenced
     * @return
     */
    private Boolean verifyDatePattern(Field variable, Annotation anotacion, DatePattern datePattern) {
        try {

            DatePatternBeans datePatternBeans = new DatePatternBeans();
            datePatternBeans.setName(variable.getName());
            datePatternBeans.setType(variable.getType().getName());
            datePatternBeans.setDateformat(datePattern.dateformat());

            datePatternBeansList.add(datePatternBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyReferenced() " + e.getLocalizedMessage());
        }
        return false;
    }

    

   
    /**
     *
     * @param doc
     * @param verifyID
     * @return
     */
    public Boolean save(JsonObject  doc, Boolean... verifyID) {
        try {
            getBucket().upsert(JsonDocument.create("u:king_arthur", doc));
            return true;

        } catch (Exception ex) {
            Logger.getLogger(CouchbaseAbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("save() " + ex.getLocalizedMessage());
            exception = new Exception("save() " + ex.getLocalizedMessage());
        }
        return false;
    }

  
 @Override
    public Object find(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object find(Document document) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

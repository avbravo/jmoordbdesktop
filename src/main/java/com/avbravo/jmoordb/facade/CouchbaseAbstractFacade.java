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
import com.avbravo.jmoordb.interfaces.CouchbaseAbstractInterface;
import com.avbravo.jmoordb.internal.Analizador;
import com.avbravo.jmoordb.internal.DocumentToJavaCouchbase;
import com.avbravo.jmoordb.internal.DocumentToJavaMongoDB;
import com.avbravo.jmoordb.internal.JavaToDocumentCouchbase;
import com.avbravo.jmoordb.util.Util;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.mongodb.util.JSON;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

/**
 *
 * @author avbravo
 * @param <T>
 */
public abstract class CouchbaseAbstractFacade<T> implements CouchbaseAbstractInterface {

    protected abstract Cluster getCluster();
    private JavaToDocumentCouchbase javaToDocumentCouchbase = new JavaToDocumentCouchbase();
    private DocumentToJavaCouchbase documentToJavaCouchbase = new DocumentToJavaCouchbase();
    private DocumentToJavaMongoDB documentToJavaMongoDB = new DocumentToJavaMongoDB();
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
        Analizador analizador = new Analizador();
        analizador.analizar(fields);
        primaryKeyList = analizador.getPrimaryKeyList();
        embeddedBeansList = analizador.getEmbeddedBeansList();
        referencedBeansList = analizador.getReferencedBeansList();
        datePatternBeansList = analizador.getDatePatternBeansList();
        fieldBeansList = analizador.getFieldBeansList();

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
        Bucket bucket = getCluster().openBucket(database);
        return bucket;
    }

    /**
     *
     * @param doc
     * @param verifyID
     * @return
     */
    public JsonObject toDocument(Object t) {
        return javaToDocumentCouchbase.toDocument(t, embeddedBeansList, referencedBeansList);
    }

    /**
     * Crea un indice primario
     *
     * @return
     */
    public Boolean createPrimaryIndex() {
        try {
            getBucket().bucketManager().createN1qlPrimaryIndex(database, true, false);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(CouchbaseAbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("createPrimaryIndex() " + ex.getLocalizedMessage());
            exception = new Exception("createPrimaryIndex() " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param t
     * @param verifyID
     * @return
     */
    public Boolean save(T t, Boolean autoid, Boolean... verifyID) {
        try {
            String id = "";
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];
            }
            if (verificate) {
                Optional<T> t2 = findById(t);

                if (t2.isPresent()) {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }

            if (autoid) {
                id = UUID.randomUUID().toString();
            } else {
                id = (String) getPrimaryKeyValue(t);
            }

            JsonObject doc = toDocument(t);
//            String id = UUID.randomUUID().toString();
            JsonDocument document = JsonDocument.create(id, doc);
            JsonDocument response = getBucket().upsert(document);
            return true;

        } catch (Exception ex) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("save() " + ex.getLocalizedMessage());
            exception = new Exception("save() " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param doc
     * @param verifyID
     * @return
     */
    public Boolean save(JsonObject doc, Boolean autoid, Boolean... verifyID) {
        try {
            String id = "";
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];

            }
            Document docId = jsonToPojo(doc.toString());
            T t_ = (T) documentToJavaMongoDB.fromDocument(entityClass, docId, embeddedBeansList, referencedBeansList);

            if (verificate) {

                Optional<T> t2 = findById(t_);

                if (t2.isPresent()) {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }
            if (autoid) {
                id = UUID.randomUUID().toString();
            } else {
                id = (String) getPrimaryKeyValue(t_);
            }
            JsonDocument document = JsonDocument.create(id, doc);
            JsonDocument response = getBucket().upsert(document);
            return true;

        } catch (Exception ex) {
            Logger.getLogger(CouchbaseAbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("save() " + ex.getLocalizedMessage());
            exception = new Exception("save() " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     * guarda un documento que contiene un id generado automaticamente
     *
     * @param doc
     * @param verifyID
     * @return
     */
    public Boolean saveWithPreID(JsonDocument doc, Boolean... verifyID) {
        try {
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];
            }
            Document docId = jsonDocumentToDocument(doc);
            T t_ = (T) documentToJavaMongoDB.fromDocument(entityClass, docId, embeddedBeansList, referencedBeansList);
            if (verificate) {
                Optional<T> t2 = findById(t_);
                if (t2.isPresent()) {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }
            JsonDocument response = getBucket().upsert(doc);
            return true;

        } catch (Exception ex) {
            Logger.getLogger(CouchbaseAbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("save() " + ex.getLocalizedMessage());
            exception = new Exception("save() " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     * devuelve el valor de la llave primaria
     *
     * @param t2
     * @return
     */
    private Object getPrimaryKeyValue(T t2) {
        Object o = new Object();
        try {
            Object t = entityClass.newInstance();
            for (PrimaryKey p : primaryKeyList) {

                String name = "get" + util.letterToUpper(p.getName());
                Method method;
                try {

                    method = entityClass.getDeclaredMethod(name);
                    o = method.invoke(t2);

                } catch (Exception e) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
                    exception = new Exception("getDocumentPrimaryKey() ", e);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "getDocumentPrimaryKey()").log(Level.SEVERE, null, e);
            exception = new Exception("getDocumentPrimaryKey() ", e);
        }
        return o;
    }

    private String getPrimaryKeyType(T t2) {
        String type = "String";
        try {
            Object t = entityClass.newInstance();
            for (PrimaryKey p : primaryKeyList) {
                type = p.getType();

            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "getDocumentPrimaryKey()").log(Level.SEVERE, null, e);
            exception = new Exception("getDocumentPrimaryKey() ", e);
        }
        return type;
    }

    /**
     *
     * @return Document() correspondiente a la llave primaria
     */
    private JsonObject findDocPrimaryKey(T t2) {
        JsonObject doc = JsonObject.create();
        try {
            Object t = entityClass.newInstance();
            for (PrimaryKey p : primaryKeyList) {
                String name = "get" + util.letterToUpper(p.getName());
                Method method;
                try {

                    method = entityClass.getDeclaredMethod(name);

                    doc.put(p.getName(), method.invoke(t2));

                } catch (Exception e) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
                    exception = new Exception("getDocumentPrimaryKey() ", e);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "getDocumentPrimaryKey()").log(Level.SEVERE, null, e);
            exception = new Exception("getDocumentPrimaryKey() ", e);
        }
        return doc;
    }

    /**
     * convierte un row a String Json
     *
     * @param row
     * @return
     */
    private Document jsonDocumentToDocument(JsonDocument doc) {
        Document docR = new Document();
        String texto = doc.toString();
        try {

            Integer pos1 = texto.indexOf("content=");

            Integer pos2 = texto.lastIndexOf(", mutationToken=");

            String n = texto.substring(pos1 + 8, pos2);
            System.out.println("n:::  " + n);
            docR = Document.parse(n);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "rowToString()").log(Level.SEVERE, null, e);
            exception = new Exception("rowToString() ", e);
        }
        return docR;
    }

    private Document rowToDocument(N1qlQueryRow row) {
        Document doc = new Document();
        String text = row.value().toString();
        try {
            String texto = row.value().toString();
            Integer pos1 = texto.indexOf(":");

            Integer pos2 = texto.lastIndexOf("}");

            String n = texto.substring(pos1 + 1, pos2);

            doc = Document.parse(n);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "rowToString()").log(Level.SEVERE, null, e);
            exception = new Exception("rowToString() ", e);
        }
        return doc;
    }

    /**
     *
     * @param texto
     * @return
     */
    private Document jsonToPojo(String texto) {
        Document doc = new Document();

        try {

            doc = Document.parse(texto);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "rowToString()").log(Level.SEVERE, null, e);
            exception = new Exception("rowToString() ", e);
        }
        return doc;
    }

    /**
     * Busca todos los documentos
     *
     * @return
     */
    /**
     * Busca por la llave primaria del documento
     *
     * @param t2
     * @return
     */
    public Optional<T> findById(T t2) {
        String statement = "select * from " + database;
        String where = " where ";
        try {
            Integer contador = 0;
            Object t = entityClass.newInstance();
            for (PrimaryKey p : primaryKeyList) {
                String name = "get" + util.letterToUpper(p.getName());
                Method method;
                try {
                    if (contador > 0) {
                        where += " , ";
                    }
                    method = entityClass.getDeclaredMethod(name);
                    where += p.getName() + " = '" + method.invoke(t2) + "'";
                    contador++;
                } catch (Exception e) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
                    exception = new Exception("findById() ", e);
                }
            }

            statement += where;

            return find(statement);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "findById()").log(Level.SEVERE, null, e);
            exception = new Exception("findById() ", e);
        }
        return Optional.empty();
    }

    public List< T> findAll() {
        list = new ArrayList<>();
        try {
            String statement = "select * from " + database;
            N1qlQuery query = N1qlQuery.simple(statement);

            N1qlQueryResult result = getBucket().query(query);
            for (N1qlQueryRow row : result) {

                Document doc = rowToDocument(row);

                t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
                list.add(t1);

            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return list;
    }

    /**
     *
     * @param statement
     * @return
     */
    public Optional<T> find(String statement) {
        list = new ArrayList<>();
        //  Document sortQuery = new Document();
        try {
            N1qlQuery query = N1qlQuery.simple(statement);

            N1qlQueryResult result = getBucket().query(query);
            for (N1qlQueryRow row : result) {

                Document doc = rowToDocument(row);

                t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
                list.add(t1);
                return Optional.of(t1);

            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return Optional.empty();
    }

    public Optional<T> find(String key, Object value) {
        list = new ArrayList<>();
        //  Document sortQuery = new Document();
        try {
            String statement = "select * from " + database + " where " + key + " = " + value;
            N1qlQuery query = N1qlQuery.simple(statement);

            N1qlQueryResult result = getBucket().query(query);
            for (N1qlQueryRow row : result) {

                Document doc = rowToDocument(row);

                t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
                list.add(t1);
                return Optional.of(t1);

            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return Optional.empty();
    }

    /**
     *
     * @param statement
     * @return
     */
    public List< T> findBy(String statement) {
        list = new ArrayList<>();
        try {
            N1qlQuery query = N1qlQuery.simple(statement);

            N1qlQueryResult result = getBucket().query(query);
            for (N1qlQueryRow row : result) {

                Document doc = rowToDocument(row);

                t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
                list.add(t1);

            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findBy() ", e);
            new JmoordbException("findBy()");
        }

        return list;
    }

    public Boolean disconnect() {
        return getCluster().disconnect();
    }

//     public Integer deleteAll() {
//        Integer cont = 0;
//        try {
////            while(!findAll().isEmpty()){
//                for(List<T> t: findAll()){
//                getBucket().remove(t1);
//            }
//           
//
//           
//        } catch (Exception e) {
//            Logger.getLogger(AbstractFacade.class.getName() + "removeDocument()").log(Level.SEVERE, null, e);
//            exception = new Exception("removeAll() ", e);
//        }
//        return cont;
//    }
    
     public Boolean delete(String key, Object value) {
        try {
           String statement = "delete  from " + database + " where " + key + " = " + value;
           
           JsonDocument removed = getBucket().remove("document_id");
          return true;
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "delete()").log(Level.SEVERE, null, e);
            exception = new Exception("delete() ", e);
        }
        return false;
    }
}

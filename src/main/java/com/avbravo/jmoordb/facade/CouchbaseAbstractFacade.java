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
         * @return 
         */
        public Boolean createPrimaryIndex(){
            try {
                 getBucket().bucketManager().createN1qlPrimaryIndex(database,true, false);
                return true;
            } catch (Exception ex) {
                 Logger.getLogger(CouchbaseAbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("createPrimaryIndex() " + ex.getLocalizedMessage());
            exception = new Exception("createPrimaryIndex() " + ex.getLocalizedMessage());
            }
               return false; 
        }
//         private T findInternal(Document document) {
//        try {
//            //   Object t = entityClass.newInstance();
//            MongoDatabase db = getMongoClient().getDatabase(database);
//            FindIterable<Document> iterable = db.getCollection(collection).find(document);
//            tlocal = (T) iterableSimple(iterable);
//            return tlocal;
//            //return (T) tlocal;
//        } catch (Exception e) {
//            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
//            exception = new Exception("find() ", e);
//            new JmoordbException("find()");
//        }
//       return null;
//    }
       /**
     *
     * @param t
     * @param verifyID
     * @return
     */
    public Boolean save(T t, Boolean... verifyID) {
        try {
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];

            }
             JsonObject doc = toDocument(t);
             String id = UUID.randomUUID().toString();
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
        
    public Boolean save(JsonObject doc, Boolean... verifyID) {
        try {
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];

            }
//            if(verificate){
//                 t1 = (T) documentToJavaCouchbase.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
//                T t_ = (T) findInternal(findDocPrimaryKey(t1));
//
//                if (t_ == null) {
//                    // no lo encontro
//                } else {
//                    exception = new Exception("A document with the primary key already exists.");
//                    return false;
//                }
//            }
            String id = UUID.randomUUID().toString();
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
    public Boolean saveQueYaTieneID(JsonDocument doc, Boolean... verifyID) {
        try {
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];

            }
//            if(verificate){
//                 t1 = (T) documentToJavaCouchbase.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
//                T t_ = (T) findInternal(findDocPrimaryKey(t1));
//
//                if (t_ == null) {
//                    // no lo encontro
//                } else {
//                    exception = new Exception("A document with the primary key already exists.");
//                    return false;
//                }
//            }
          //  String id = UUID.randomUUID().toString();
//            JsonDocument document = JsonDocument.create(id, doc);
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

//     private T findInternal(Document document) {
//        try {
//             
//            //   Object t = entityClass.newInstance();
//            JsonDocument retrieved = bucket.get(id);
//      
//            tlocal = (T) iterableSimple(iterable);
//            return tlocal;
//            //return (T) tlocal;
//        } catch (Exception e) {
//            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
//            exception = new Exception("find() ", e);
//            new JmoordbException("find()");
//        }
//       return null;
//    }
    public List< T> findAll(String statement) {
        list = new ArrayList<>();
        //  Document sortQuery = new Document();
        try {
            N1qlQuery query = N1qlQuery.simple(statement);

            N1qlQueryResult result = getBucket().query(query);

            
        
            List<Map<String, Object>> content = new ArrayList<>();
            Integer contador =0;
            //     JSONSerializer serializer = new JSONSerializer();
            for (N1qlQueryRow row : result) {
               
                      content.add(row.value().toMap());
                       System.out.println("row.value() ------> " + row.value());
                System.out.println("content: "+content);
                String idplaneta = row.value().getString("idplaneta");
                String planeta = row.value().getString("planeta");
                System.out.println("         idplaneta: "+idplaneta + " planeta: "+planeta);
     
//                JsonDocument doc = JsonDocument.create("walter", row.value());
//                System.out.println("doc.id====  ");doc.id();
//                JsonArray a=doc.content().getArray("default");
                
//                Document doc1 = (Document) JSON.parse(row.value().toString()); 
/*
System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
                Document doc1 = Document.parse(row.value().toString());
                System.out.println("-------------- Convertido ----------------");
                System.out.println("doc1 "+doc1.toString());
                System.out.println("doc1.json() "+doc1.toJson());

              t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc1, embeddedBeansList, referencedBeansList);
              list.add(t1);
*/
//              t1 = (T) documentToJavaCouchbase.fromDocument(entityClass, row.value(), embeddedBeansList, referencedBeansList);
//              list.add(t1);
//               
               
//               
//             JsonDocument js = JsonDocument.create("1", row.value());
//         
//                System.out.println("        Contentent ("+ contador + " ) "+content.get(contador++).entrySet().toString());

                      
              //  System.out.println("paso 1>>");
                Map<String, Object> m = row.value().toMap();
                System.out.println("map " + m.toString());
                Set set = m.entrySet();
                Iterator i = set.iterator();

                // Display elements
                System.out.println("...........................");
                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry) i.next();
                    System.out.println("key " + me.getKey() + " value: " + me.getValue());
                    
                    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
                    
                Document doc1 = Document.parse(me.getValue().toString());
                System.out.println("-------------- Convertido ----------------");
                System.out.println("doc1 "+doc1.toString());
                System.out.println("doc1.json() "+doc1.toJson());
                 t1 = (T) documentToJavaMongoDB.fromDocument(entityClass, doc1, embeddedBeansList, referencedBeansList);
              list.add(t1);
                }
                
                
                

//                System.out.println("...........................");
//                String var = (String) m.get(1);
//                System.out.println("Value at index 2 is: " + var);
//
//
//                System.out.println(" row:  " + row);
//
               
            }
            
//            System.out.println("###########################################");
//            System.out.println("content: "+content.toString());
//            System.out.println("++++++++++++++++++++++++++++++++++++++++");
//            


            
//            for(Map<String, Object> e:content){
//                
//                e.forEach((k,v)->System.out.println("Item : " + k + " Count : " + v));
//                T t12 =(T) e.values();
//                System.out.println("t12 "+t12.toString());
//                System.out.println("---> en el for "+e.values());
//            }

            //View todoView = this.getBucket().getView("todos");
//prints:
// "Hello, users in their fifties:
// Walter!"
//            if (docSort.length != 0) {
//                sortQuery = docSort[0];
//
//            }
            //    list = iterableList(iterable);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return list;
    }

    public Boolean disconnetc() {
        return getCluster().disconnect();
    }

    /**
     *
     * @param t
     * @param verifyID
     * @return
     */
    @Override
    public Object find(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object find(Document document) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

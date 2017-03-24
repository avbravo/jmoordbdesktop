/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.mongodb.facade;


import com.avbravo.jmoordb.DatePatternBeans;
import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.FieldBeans;
import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.PrimaryKey;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.mongodb.interfaces.AbstractInterface;
import com.avbravo.jmoordb.mongodb.internal.DocumentToJavaMongoDB;
import com.avbravo.jmoordb.mongodb.internal.JavaToDocument;
import com.avbravo.jmoordb.util.Analizador;
import com.avbravo.jmoordb.util.Test;
import com.avbravo.jmoordb.util.Util;
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
public abstract class AbstractFacade<T> implements AbstractInterface {
 protected abstract MongoClient getMongoClient();

    Integer contador = 0;
    private JavaToDocument javaToDocument = new JavaToDocument();
    private DocumentToJavaMongoDB documentToJava = new DocumentToJavaMongoDB();
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
MongoDatabase db_;
    
    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

     @Override 
    public MongoDatabase getMongoDatabase() {  
        try {     
//            if(getMongoClient() == null){
//                System.out.println("conexion es nula");
//            }else{
//                System.out.println("Este conectado ");
//            }
         //getMongoClient().getConnectPoint();
            System.out.println("---> getConnectPoint()" +getMongoClient().getConnectPoint());
            MongoDatabase db = getMongoClient().getDatabase(database);
         if(db == null){
             Test.msg("+++AbstractFacade.getMonogDatabase() == null");
         }else{
             Test.msg("+++AbstractFacade.getMonogDatabase() != null");
         }
             return db;
         } catch (Exception ex) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
            new JmoordbException("getMongoDatabase() " + ex.getLocalizedMessage());
            exception = new Exception("getMongoDatabase() " + ex.getLocalizedMessage());
        }
         return null;
    }
    

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
        datePatternBeansList = new ArrayList<>();
        fieldBeansList = new ArrayList<>();
      
        /**
         * lee las anotaciones @Id para obtener los PrimaryKey del documento
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
db_ = getMongoDatabase();
    }

   
 

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
            if (verificate) {
                //Buscar llave primaria

                T t_ = (T) findInternal(findDocPrimaryKey(t));

//                if (t_ == null) {
                if (t_ == null) {
                    // no lo encontro
                } else {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }

            getMongoDatabase().getCollection(collection).insertOne(toDocument(t));

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
    public Boolean save(Document doc, Boolean... verifyID) {
        try {
            Boolean verificate = true;
            if (verifyID.length != 0) {
                verificate = verifyID[0];

            }
            if (verificate) {
                //Buscar llave primaria

                t1 = (T) documentToJava.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
                T t_ = (T) findInternal(findDocPrimaryKey(t1));

                if (t_ == null) {
                    // no lo encontro
                } else {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }

            getMongoDatabase().getCollection(collection).insertOne(doc);

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
     * @param t2
     * @return devuelve el Pojo convertido a documento.
     */
    public Document toDocument(Object t) {
        return javaToDocument.toDocument(t, embeddedBeansList, referencedBeansList);
    }

    /**
     *
     * @return Document() correspondiente a la llave primaria
     */
    private Document findDocPrimaryKey(T t2) {
        Document doc = new Document();
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
     * Crea un Index en base a la llave primaria
     *
     * @return
     */
    private Document getIndexPrimaryKey() {
        Document doc = new Document();
        try {
            primaryKeyList.forEach((p) -> {
                doc.put(p.getName(), 1);
            });
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "getIndexPrimaryKey()").log(Level.SEVERE, null, e);
            exception = new Exception("getIndexPrimaryKey() ", e);
        }
        return doc;
    }

    /**
     *
     * @param doc
     * @return
     */
    public Boolean createIndex(Document... doc) {
        Document docIndex = new Document();
        try {
            if (doc.length != 0) {
                docIndex = doc[0];

            } else {
                docIndex = getIndexPrimaryKey();
            }
            getMongoDatabase().getCollection(collection).createIndex(docIndex);
            return true;
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "createIndex()").log(Level.SEVERE, null, e);
            exception = new Exception("createIndex() ", e);
        }
        return false;
    }

    /**
     * Busca por la llave primaria del documento
     *
     * @param t2
     * @return
     */
    public Optional<T> findById(T t2) {
        Document doc = new Document();
        try {
            Object t = entityClass.newInstance();
            for (PrimaryKey p : primaryKeyList) {
                String name = "get" + util.letterToUpper(p.getName());
                Method method;
                try {

                    method = entityClass.getDeclaredMethod(name);

                    doc.put(p.getName(), method.invoke(t2));

                    return find(doc);
                } catch (Exception e) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
                    exception = new Exception("findById() ", e);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "findById()").log(Level.SEVERE, null, e);
            exception = new Exception("findById() ", e);
        }
        return Optional.empty();
    }

    public Optional<T> findById(Document doc) {

        try {
            //  t1 = (T) documentToJava.fromDocument(entityClass, doc, embeddedBeansList, referencedBeansList);
            T t_ = (T) find(doc);

            if (t_ == null) {
                // no lo encontro
            } else {
                return Optional.of(t_);
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "findById()").log(Level.SEVERE, null, e);
            exception = new Exception("findById() ", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<T> find(String key, Object value) {
        try {

            //   Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);

            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, value));

            haveElements = false;
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    try {
                        haveElements = true;
                        tlocal = (T) documentToJava.fromDocument(entityClass, document, embeddedBeansList, referencedBeansList);
                    } catch (Exception e) {
                        Logger.getLogger(AbstractFacade.class.getName() + "find()").log(Level.SEVERE, null, e);
                        exception = new Exception("find() ", e);
                    }

                }
            });
            if (haveElements) {

//                return tlocal;
                return Optional.of(tlocal);
            }
//            return null;
            return Optional.empty();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);

        }

//        return null;
        return Optional.empty();
    }
   
 public T search(String key, Object value) {
        try {
Test.msg("=====================================================");
Test.msg("Llego al search()");
Test.msg("=====================================================");
            //   Object t = entityClass.newInstance();
            //MongoDatabase db = getMongoClient().getDatabase(database);
     //   MongoDatabase db =    getMongoDatabase();
//     getMongoClient()
//         MongoDatabase db = getMongoClient().getDatabase(database);
        MongoDatabase db = db_;
         if(db == null){
             Test.msg("+++AbstractFacade.getMonogDatabase() == null");
         }else{
             Test.msg("+++AbstractFacade.getMonogDatabase() != null");
         }
if(db == null){
    Test.msg("+++ db is null");
    return null;
}else{
    Test.msg("+++ db no is null");
}
            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, value));
Test.msg("+++ paso iterable");
            haveElements = false;
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    try {
                        haveElements = true;
                        tlocal = (T) documentToJava.fromDocument(entityClass, document, embeddedBeansList, referencedBeansList);
                    } catch (Exception e) {
                        Logger.getLogger(AbstractFacade.class.getName() + "search()").log(Level.SEVERE, null, e);
                        exception = new Exception("search() ", e);
                    }

                }
            });
            if (haveElements) {
                return tlocal;
            }
            return null;

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("search() ", e);

        }

       return null;

    }
   
    /**
     *
     * @param document
     * @return
     */
    @Override
    public Optional<T> find(Document document) {
        try {
            //   Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(document);
            tlocal = (T) iterableSimple(iterable);
            return Optional.of(tlocal);
            //return (T) tlocal;
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }
        return Optional.empty();
//        return null;
    }
    private T findInternal(Document document) {
        try {
            //   Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(document);
            tlocal = (T) iterableSimple(iterable);
            return tlocal;
            //return (T) tlocal;
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }
       return null;
    }


    /**
     * Internamente recorre el iterable
     *
     * @param iterable
     * @return
     */
    private T iterableSimple(FindIterable<Document> iterable) {
        try {
            //      System.out.println("$$$$$$$iterable simple");
            haveElements = false;

            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    try {
                        haveElements = true;
                        t1 = (T) documentToJava.fromDocument(entityClass, document, embeddedBeansList, referencedBeansList);
                    } catch (Exception e) {
                        Logger.getLogger(AbstractFacade.class.getName() + "find()").log(Level.SEVERE, null, e);
                        exception = new Exception("find() ", e);
                    }

                }
            });

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("iterableSimple() ", e);

        }
        if (haveElements) {
            return (T) t1;
        }
        return null;

    }

    private List< T> iterableList(FindIterable<Document> iterable) {
        List< T> l = new ArrayList<>();
        try {
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    try {
                        t1 = (T) documentToJava.fromDocument(entityClass, document, embeddedBeansList, referencedBeansList);
                        l.add(t1);
                    } catch (Exception e) {
                        Logger.getLogger(AbstractFacade.class.getName() + "find()").log(Level.SEVERE, null, e);
                        exception = new Exception("find() ", e);
                    }

                }
            });

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("iterableSimple() ", e);

        }

        return l;
    }

    /**
     *
     * @param doc
     * @return el numero de documentos en la coleccion
     */
    public Integer count(Document... doc) {
        try {
            contador = 0;
            Document documento = new Document();
            if (doc.length != 0) {
                documento = doc[0];
                MongoDatabase db = getMongoClient().getDatabase(database);
                FindIterable<Document> iterable = db.getCollection(collection).find(documento);

                iterable.forEach(new Block<Document>() {
                    @Override
                    public void apply(final Document document) {
                        try {
                            contador++;
                        } catch (Exception e) {
                            Logger.getLogger(AbstractFacade.class.getName() + "count()").log(Level.SEVERE, null, e);
                            exception = new Exception("count()", e);
                        }
                    }
                });

            } else {
                // no tiene parametros
                contador = (int) getMongoClient().getDatabase(database).getCollection(collection).count();

            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "count()").log(Level.SEVERE, null, e);
            exception = new Exception("count()", e);
        }
        return contador;
    }

    /**
     *
     * @param document
     * @return
     */
    public List< T> findAll(Document... docSort) {
        list = new ArrayList<>();
        Document sortQuery = new Document();
        try {
            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find().sort(sortQuery);
            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return list;
    }

    /**
     *
     * @param key
     * @param value
     * @param field
     * @return
     */
    public T findOneAndUpdate(String key, String value, String field, Integer... incremento) {

        try {
            Integer increment = 1;
            if (incremento.length != 0) {
                increment = incremento[0];

            }
            Document doc = new Document(key, value);
            Document inc = new Document("$inc", new Document(field, increment));

            FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
            findOneAndUpdateOptions.upsert(true);

            findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);

            Object t = entityClass.newInstance();

            MongoDatabase db = getMongoClient().getDatabase(database);
            Document iterable = db.getCollection(collection).findOneAndUpdate(doc, inc, findOneAndUpdateOptions);

            try {

                t1 = (T) documentToJava.fromDocument(entityClass, iterable, embeddedBeansList, referencedBeansList);

            } catch (Exception e) {
                Logger.getLogger(AbstractFacade.class.getName() + "findOneAndUpdate()").log(Level.SEVERE, null, e);
                exception = new Exception("findOneAndUpdate()", e);
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findOneAndUpdate()", e);
        }

        return t1;
    }

    /**
     * findOneAndUpdate
     *
     * @param doc
     * @param field
     * @param incremento
     * @return
     */
    public T findOneAndUpdate(Document doc, String field, Integer... incremento) {
        try {
            Integer increment = 1;
            if (incremento.length != 0) {
                increment = incremento[0];

            }

            Document inc = new Document("$inc", new Document(field, increment));

            FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
            findOneAndUpdateOptions.upsert(true);

            findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);

            Object t = entityClass.newInstance();
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);
            Document iterable = db.getCollection(collection).findOneAndUpdate(doc, inc, findOneAndUpdateOptions);

            try {
                t1 = (T) documentToJava.fromDocument(entityClass, iterable, embeddedBeansList, referencedBeansList);
//                Method method = entityClass.getDeclaredMethod("toPojo", Document.class);
//                list.add((T) method.invoke(t, iterable));
            } catch (Exception e) {
                Logger.getLogger(AbstractFacade.class.getName() + "findOneAndUpdate()").log(Level.SEVERE, null, e);
                exception = new Exception("findOneAndUpdate()", e);
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findOneAndUpdate()", e);
        }

        return t1;
    }

    /**
     *
     * @param doc
     * @param inc
     * @param incremento
     * @return
     */
    public T findOneAndUpdate(Document doc, Document inc, Integer... incremento) {
        try {

            FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
            findOneAndUpdateOptions.upsert(true);

            findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);

            Object t = entityClass.newInstance();
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);
            Document iterable = db.getCollection(collection).findOneAndUpdate(doc, inc, findOneAndUpdateOptions);

            try {
                t1 = (T) documentToJava.fromDocument(entityClass, iterable, embeddedBeansList, referencedBeansList);
//                Method method = entityClass.getDeclaredMethod("toPojo", Document.class);
//                list.add((T) method.invoke(t, iterable));
            } catch (Exception e) {
                Logger.getLogger(AbstractFacade.class.getName() + "findOneAndUpdate()").log(Level.SEVERE, null, e);
                exception = new Exception("findOneAndUpdate()", e);
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findOneAndUpdate()", e);
        }

        return t1;
    }

    /**
     *
     * @param doc
     * @param docSort
     * @return
     */
    public List<T> findBy(Document doc, Document... docSort) {
        Document sortQuery = new Document();
        try {
            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(doc).sort(sortQuery);
            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findBy() ", e);
        }
        return list;
    }

    public List<T> findBy(String key, Object value, Document... docSort) {
        Document sortQuery = new Document();
        try {
            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            list = new ArrayList<>();
            Document doc = new Document(key, value);
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(doc).sort(sortQuery);
            list = iterableList(iterable);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findBy() ", e);
        }
        return list;
    }

    /**
     *
     * @param filter
     * @param docSort
     * @return
     */
    public List<T> filters(Bson filter, Document... docSort) {
        Document sortQuery = new Document();
        try {

            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);

            FindIterable<Document> iterable = db.getCollection(collection).find(filter).sort(sortQuery);
            list = iterableList(iterable);
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findBy() ", e);
        }
        return list;
    }

    /**
     *
     * @param key
     * @param value
     * @param docSort
     * @return
     */
    public List<T> findLikeOld(String key, String value, Document... docSort) {
        Document sortQuery = new Document();
        list = new ArrayList<>();

        try {

            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            Object t = entityClass.newInstance();
            Pattern regex = Pattern.compile(value);

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, regex)).sort(sortQuery);
            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findLike()", e);
        }
        return list;
    }
    public List<T> findLike(String key, String value, Document... docSort) {
        Document sortQuery = new Document();
        list = new ArrayList<>();

        try {

            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            Object t = entityClass.newInstance();
            Pattern regex = Pattern.compile(value);

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, regex)).sort(sortQuery);
            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findLike()", e);
        }
        return list;
    }

    /**
     * Requiere que se cree un indice primero
     * URL:https://docs.mongodb.com/manual/reference/operator/query/text/
     * Indice: db.planetas.createIndex( { idplaneta: "text" } )
     * @param key
     * @param value
     * @param docSort
     * @return 
     */
    
    public List<T> findText(String key, String value, Document... docSort) {    
        Document sortQuery = new Document();
        list = new ArrayList<>();

        try {

            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
 FindIterable<Document> iterable = db.getCollection(collection)
         .find(new Document("$text", new Document("$search", value)));
                      
            list = iterableList(iterable);
          
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findText()", e);
        }
        return list;
    }
    /**
     * devuelva la lista de colecciones
     *
     * @return
     */
    public List<String> listCollecctions() {
        List<String> list = new ArrayList<>();
        try {
            for (Document name : getMongoDatabase().listCollections()) {
                list.add(name.get("name").toString());
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "drop()").log(Level.SEVERE, null, e);
            exception = new Exception("listCollecctions() ", e);
        }
        return list;
    }

    /**
     * verifica si existe una coleccion
     *
     * @param collection
     * @return
     */
    public Boolean existsCollection(String nameCollection) {
        try {
            Boolean found = false;
            for (String s : listCollecctions()) {
                if (s.equals(nameCollection)) {
                    return true;
                }
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "existsCollection()").log(Level.SEVERE, null, e);
            exception = new Exception("existsCollection() ", e);
        }
        return false;
    }

    /**
     * createCollection
     *
     * @param nameCollection
     * @return
     */
    public Boolean createCollection(String nameCollection) {
        try {
            getMongoDatabase().createCollection(nameCollection);
            return true;
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "existsCollection()").log(Level.SEVERE, null, e);
            exception = new Exception("existsCollection() ", e);
        }
        return false;
    }

    /**
     * elimina la coleccion actual
     *
     * @return
     */
    public Boolean drop() {

        try {
            if (existsCollection(collection)) {
                getMongoDatabase().getCollection(collection).drop();
                return true;
            }

            return false;

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "drop()").log(Level.SEVERE, null, e);
            exception = new Exception("drop() ", e);
        }
        return false;
    }

    /**
     * elimina la coleccion que se indiquem como parametro
     *
     * @param collection
     * @return
     */
    public Boolean drop(String collection) {

        try {
            getMongoDatabase().getCollection(collection).drop();

            return true;

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "drop()").log(Level.SEVERE, null, e);
            exception = new Exception("drop() ", e);
        }
        return false;
    }

    /**
     *
     */
    public Boolean dropDatabase() {

        try {
            getMongoDatabase().drop();

            return true;

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "drop()").log(Level.SEVERE, null, e);
            exception = new Exception("drop() ", e);
        }
        return false;
    }

    public List<T> findHelperSort(String predicate, Document doc, String key, String value) {
        try {

            Object t = entityClass.newInstance();
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = getIterable();
            switch (predicate) {
                case "ascending":
                    iterable = db.getCollection(collection).find(doc).sort(ascending(key, value));
                    break;
                case "descending":
                    iterable = db.getCollection(collection).find(doc).sort(descending(key, value));
                    break;

            }

            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("findHelperSort()", e);
        }
        return list;
    }

    /**
     *
     * @param predicate eq,gt.lt
     * @param key
     * @param value
     * @param docSort
     * @return
     */
    public List<T> helpers(String predicate, String key, Object value, Document... docSort) {
        Document sortQuery = new Document();
        try {
            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            Object t = entityClass.newInstance();
            list = new ArrayList<>();

            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = getIterable();
            switch (predicate) {
                case "eq":
                    iterable = db.getCollection(collection).find(eq(key, value)).sort(sortQuery);
                    break;
                case "lt":
                    iterable = db.getCollection(collection).find(lt(key, value)).sort(sortQuery);
                    break;
                case "gt":
                    iterable = db.getCollection(collection).find(gt(key, value)).sort(sortQuery);
                    break;
            }

            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("helpers()", e);
        }
        return list;
    }

    private FindIterable<Document> getIterable() {
        FindIterable<Document> iterable = new FindIterable<Document>() {
            @Override
            public FindIterable<Document> filter(Bson bson) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> limit(int i) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> skip(int i) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> maxTime(long l, TimeUnit tu) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> maxAwaitTime(long l, TimeUnit tu) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> modifiers(Bson bson) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> projection(Bson bson) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> sort(Bson bson) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> noCursorTimeout(boolean bln) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> oplogReplay(boolean bln) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> partial(boolean bln) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> cursorType(CursorType ct) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> batchSize(int i) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public MongoCursor<Document> iterator() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Document first() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public <U> MongoIterable<U> map(Function<Document, U> fnctn) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void forEach(Block<? super Document> block) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public <A extends Collection<? super Document>> A into(A a) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FindIterable<Document> collation(Collation cltn) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        return iterable;
    }

    /**
     * elimina un documento
     *
     * @param doc
     * @return
     */
    public Boolean delete(String key, Object value) {
        try {
            Document doc = new Document(key, value);
            DeleteResult dr = getMongoDatabase().getCollection(collection).deleteOne(doc);
            if (dr.getDeletedCount() >= 0) {
                return true;
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "delete()").log(Level.SEVERE, null, e);
            exception = new Exception("delete() ", e);
        }
        return false;
    }

    /**
     * elimina un documento
     *
     * @param doc
     * @return
     */
    public Boolean delete(Document doc) {
        try {
            DeleteResult dr = getMongoDatabase().getCollection(collection).deleteOne(doc);
            if (dr.getDeletedCount() >= 0) {
                return true;
            }

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "remove()").log(Level.SEVERE, null, e);
            exception = new Exception("remove() ", e);
        }
        return false;
    }

    /**
     *
     * @param doc
     * @return
     */
    public Integer deleteMany(String key, Object value) {
        Integer cont = 0;
        try {
            Document doc = new Document(key, value);
            DeleteResult dr = getMongoDatabase().getCollection(collection).deleteMany(doc);
            cont = (int) dr.getDeletedCount();
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "deleteManye()").log(Level.SEVERE, null, e);
            exception = new Exception("deleteMany() ", e);
        }
        return cont;
    }

    /**
     *
     * @param doc
     * @return
     */
    public Integer deleteMany(Document doc) {
        Integer cont = 0;
        try {
            DeleteResult dr = getMongoDatabase().getCollection(collection).deleteMany(doc);
            cont = (int) dr.getDeletedCount();
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "deleteManye()").log(Level.SEVERE, null, e);
            exception = new Exception("deleteMany() ", e);
        }
        return cont;
    }

    /**
     * Remove all documment of a collection
     *
     * @return count of document delete
     */
    public Integer deleteAll() {
        Integer cont = 0;
        try {
            DeleteResult dr = getMongoDatabase().getCollection(collection).deleteMany(new Document());

            cont = (int) dr.getDeletedCount();
        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "removeDocument()").log(Level.SEVERE, null, e);
            exception = new Exception("removeAll() ", e);
        }
        return cont;
    }

    public Boolean update(T t) {

        Integer n = update(t, new Document("$set", toDocument(t)));
        if (n >= 1) {
            return true;
        } else {
            return false;
        }
    }

    private Integer update(T t2, Document doc) {
        Integer documentosModificados = 0;
        Document search = new Document();

        try {
            search = findDocPrimaryKey(t2);

            UpdateResult updateResult = getMongoDatabase().getCollection(collection).updateOne(search, doc);
            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "updateOne()").log(Level.SEVERE, null, e);
            exception = new Exception("updateOne() ", e);
        }
        return 0;
    }

    public Integer update(Document docSearch, Document docUpdate) {
        Integer documentosModificados = 0;

        try {

            UpdateResult updateResult = getMongoDatabase().getCollection(collection).updateOne(docSearch, docUpdate);
            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "updateOne()").log(Level.SEVERE, null, e);
            exception = new Exception("updateOne() ", e);
        }
        return 0;
    }

    /**
     * Actualiza multiples documentos
     *
     * @param docSearch
     * @param docUpdate
     * @return
     */
    public Integer updateMany(Document docSearch, Document docUpdate) {
        Integer documentosModificados = 0;

        try {

            UpdateResult updateResult = getMongoDatabase().getCollection(collection).updateMany(docSearch, docUpdate);
            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "updateMany()").log(Level.SEVERE, null, e);
            exception = new Exception("updateMany() ", e);
        }
        return 0;
    }

    /**
     * implementa replaceOne
     *
     * @param key
     * @param value
     * @param docUpdate
     * @return
     */
    public Integer replaceOne(String key, String value, Document docUpdate) {
        Integer documentosModificados = 0;

        try {
            UpdateResult updateResult = getMongoDatabase().getCollection(collection).replaceOne(Filters.eq(key, value), docUpdate);

            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "replaceOne()").log(Level.SEVERE, null, e);
            exception = new Exception("replaceOne() ", e);
        }
        return 0;
    }

    /**
     *
     * @param key
     * @param value
     * @param docUpdate
     * @return
     */
    public Integer replaceOne(Bson search, Document docUpdate) {
        Integer documentosModificados = 0;

        try {
            UpdateResult updateResult = getMongoDatabase().getCollection(collection).replaceOne(search, docUpdate);

            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "replaceOne()").log(Level.SEVERE, null, e);
            exception = new Exception("replaceOne() ", e);
        }
        return 0;
    }

    /**
     *
     * @param docSearch
     * @param docUpdate
     * @param options
     * @return
     */
    public Integer replaceOne(Document docSearch, Document docUpdate, String... options) {
        Integer documentosModificados = 0;

        try {

            UpdateResult updateResult = getMongoDatabase().getCollection(collection).replaceOne(docSearch, docUpdate);
            return (int) updateResult.getModifiedCount();

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName() + "updateOne()").log(Level.SEVERE, null, e);
            exception = new Exception("updateOne() ", e);
        }
        return 0;
    }
    
    
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

}

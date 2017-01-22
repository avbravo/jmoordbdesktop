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
import com.avbravo.jmoordb.internal.DocumentToJava;
import com.avbravo.jmoordb.internal.JavaToDocument;
import com.avbravo.jmoordb.util.Util;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
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
public abstract class AbstractFacade<T> implements AbstractInterface {

    private JavaToDocument javaToDocument = new JavaToDocument();
    private DocumentToJava documentToJava = new DocumentToJava();
    T t1;
    private Class<T> entityClass;
    private String database;
    private String collection;
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

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

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

    @Override
    public MongoDatabase getDB() {
        MongoDatabase db = getMongoClient().getDatabase(database);
        return db;
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

                T t_ = (T) find(findDocPrimaryKey(t));

                if (t_ == null) {
                    // no lo encontro
                } else {
                    exception = new Exception("A document with the primary key already exists.");
                    return false;
                }
            }

            getDB().getCollection(collection).insertOne(toDocument(t));

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
            getDB().getCollection(collection).createIndex(docIndex);
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
    public T findById(T t2) {
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
        return null;
    }

    @Override
    public T find(String key, Object value) {
        try {
            Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, value));
            t1 = iterableSimple(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return (T) t1;
    }

    /**
     *
     * @param document
     * @return
     */
    @Override
    public T find(Document document) {
        try {
            Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(document);
            t1 = iterableSimple(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return (T) t1;
    }

    @Override
    public T find(String key, Integer value) {
        try {
            Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find(new Document(key, value));
            t1 = iterableSimple(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return (T) t1;
    }

    /**
     * Internamente recorre el iterable
     *
     * @param iterable
     * @return
     */
    private T iterableSimple(FindIterable<Document> iterable) {
        try {
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    try {
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

        return (T) t1;
    }


    private List< T > iterableList(FindIterable<Document> iterable) {
       List< T >   l = new ArrayList<>();
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
    * @return  el numero de documentos en la coleccion
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
                contador = (int)getMongoClient().getDatabase(database).getCollection(collection).count();

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
    
    public List< T > findAll(Document... docSort) {
       list = new ArrayList<>();
             Document sortQuery = new Document();
        try {
            if (docSort.length != 0) {
                sortQuery = docSort[0];

            }
            Object t = entityClass.newInstance();
            MongoDatabase db = getMongoClient().getDatabase(database);
            FindIterable<Document> iterable = db.getCollection(collection).find().sort(sortQuery);
            list = iterableList(iterable);

        } catch (Exception e) {
            Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, e);
            exception = new Exception("find() ", e);
            new JmoordbException("find()");
        }

        return   list;
    }

}

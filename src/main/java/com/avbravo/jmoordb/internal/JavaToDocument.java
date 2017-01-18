/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.internal;

import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.ReferencedBeans;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 *
 * @author avbravo
 */
public class JavaToDocument {

    private ClassDescriptorsCache cache = new ClassDescriptorsCache();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<ReferencedBeans> referencedBeansList = new ArrayList<>();
    ReferencedBeans referencedBeans = new ReferencedBeans();

    public Document toDocument(Object obj, List<EmbeddedBeans> embeddedBeansList, List<ReferencedBeans> referencedBeansList) {
        if (obj == null) {
            return null;
        }
        this.embeddedBeansList = embeddedBeansList;
        this.referencedBeansList = referencedBeansList;
        
        Document dbObject = new Document();
        ClassDescriptor classDescriptor = cache.get(obj.getClass());
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {

      //      System.out.println("            [Analizando :  " + fieldDescriptor.getName() + " ]");
            dbObject.put(fieldDescriptor.getName(), toDBObjectRecursive(obj, fieldDescriptor, embeddedBeansList, referencedBeansList));

        }
        return dbObject;
    }
/**
 * Se utiliza cuando se pasan List<Entity> referenciados
 * @param obj
 * @param idreferenciado
 * @return 
 */
    private Document toDocumentReferenced(Object obj, String idreferenciado) {
        if (obj == null) {
            return null;
        }

        
        Document dbObject = new Document();
        ClassDescriptor classDescriptor = cache.get(obj.getClass());
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            if (fieldDescriptor.getName().equals(idreferenciado)) {
        dbObject.put(fieldDescriptor.getName(), toDBObjectRecursive(obj, fieldDescriptor, embeddedBeansList, referencedBeansList));
            }

        }
        return dbObject;
    }

    @SuppressWarnings("rawtypes")
    public Object toDBObjectRecursive(Object object, FieldDescriptor fieldDescriptor, List<EmbeddedBeans> embeddedBeansList, List<ReferencedBeans> referencedBeansList) {
        
        if (object == null) {
            return null;
        }
        if (fieldDescriptor.isArray()) {
            
            System.out.println(" isArray");
                        if (ReflectionUtils.isSimpleClass(fieldDescriptor.getField().getType().getComponentType())) {
                return fieldDescriptor.getFieldValue(object);
            } else {
                Object[] array = (Object[]) fieldDescriptor.getFieldValue(object);
                BasicDBList fieldObj = new BasicDBList();
                for (Object el : array) {
                    fieldObj.add(toDocument(el, embeddedBeansList, referencedBeansList));
                }
                return fieldObj;
            }
        } else if (fieldDescriptor.isIterable()) {
           

            Iterable col = (Iterable) fieldDescriptor.getFieldValue(object);
            BasicDBList fieldObj = new BasicDBList();
            if (col != null) {
                for (Object el : col) {
                    
                    if (ReflectionUtils.isSimpleClass(el.getClass())) {
                    
                        fieldObj.add(el);
                    } else {
                    
                        if (isEmbedded(fieldDescriptor.getName())) {

                            fieldObj.add(toDocument(el, embeddedBeansList, referencedBeansList));
                        } else {
                            if (isReferenced(fieldDescriptor.getName())) {
                                //aris
                                ClassDescriptor classD = cache.get(el.getClass());
                                for (FieldDescriptor fieldDesc : classD.getFields()) {
                                    
                                    if (fieldDesc.getName().equals(referencedBeans.getField())) {
                                        fieldObj.add(toDocumentReferenced(el, referencedBeans.getField()));
                                    }
                                }
                                //aris

                            } else {
                                System.out.println("..........no es embebido ni referenciado");
                            }
                        }

                    }
                }
            }
            
            return fieldObj;
        } else if (fieldDescriptor.isObject()) {
            
            //System.out.println("           fieldDescriptor.isObject()        ");
            if (isEmbedded(fieldDescriptor.getName())) {
                //Embebido
                Object fieldValue = fieldDescriptor.getFieldValue(object);
                if (fieldValue == null) {
                    return null;
                }
                DBObject dbObject = new BasicDBObject();
                for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                    dbObject.put(childDescriptor.getName(), toDBObjectRecursive(fieldValue, childDescriptor, embeddedBeansList, referencedBeansList));
                }
                return dbObject;

            } else {

                if (isReferenced(fieldDescriptor.getName())) {
                    //Referenciado
                    Object fieldValue = fieldDescriptor.getFieldValue(object);
                    if (fieldValue == null) {
                        return null;
                    }
                    DBObject dbObject = new BasicDBObject();
                    for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {

                        if (childDescriptor.getName().equals(referencedBeans.getField())) {
                            dbObject.put(childDescriptor.getName(), toDBObjectRecursive(fieldValue, childDescriptor, embeddedBeansList, referencedBeansList));
                        }

                    }
                    return dbObject;

                } else {
                    System.out.println("                     [No es Referenced]");
                    new JmoordbException("@Embedded or @Reference is required for this field " + fieldDescriptor.getName());
                    return new BasicDBObject();
                }

            }

        } else if (fieldDescriptor.isMap()) {
            System.out.println("==========================");
            System.out.println("fieldDescriptor.isMap()");
            System.out.println("==========================");
            DBObject dbObject = new BasicDBObject();
            Map map = (Map) fieldDescriptor.getFieldValue(object);
            for (Object key : map.keySet()) {
                Object el = map.get(key);
                if (el == null || ReflectionUtils.isSimpleClass(el.getClass())) {
                    dbObject.put(key.toString(), el);
                } else {
                    dbObject.put(key.toString(), toDocument(el, embeddedBeansList, referencedBeansList));
                }
            }
            return dbObject;
        } else {
            //valor del atributo que no es otra clase
           
            return fieldDescriptor.getFieldValue(object);
        }
    }

    /**
     *
     * @param name
     * @return
     */
    private Boolean isEmbedded(String name) {
        try {
            if (embeddedBeansList.stream().anyMatch((eb) -> (eb.getName().equals(name)))) {
                return true;
            }
            return false;
        } catch (Exception e) {
            new JmoordbException("isEmbedded() " + e.getLocalizedMessage());
        }
        return false;
    }
/**
 * 
 * @param name
 * @return 
 */
    private Boolean isReferenced(String name) {
        try {

            for (ReferencedBeans eb : referencedBeansList) {
                if (eb.getName().equals(name)) {
                    referencedBeans = eb;
                    //   System.out.println("Referenced() "+eb.toString());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            new JmoordbException("isReferenced() " + e.getLocalizedMessage());
        }
        return false;
    }
}

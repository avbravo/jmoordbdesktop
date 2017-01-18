/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.internal;

import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.persistence.AbstractFacade;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;

/**
 *
 * @author avbravo
 */
public class DocumentToJava {

    private ClassDescriptorsCache cache = new ClassDescriptorsCache();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<ReferencedBeans> referencedBeansList = new ArrayList<>();
    ReferencedBeans referencedBeans = new ReferencedBeans();

    @SuppressWarnings("unchecked")
    public <T> T fromDocument(Class<T> clazz, Document dbObject,List<EmbeddedBeans> embeddedBeansList, List<ReferencedBeans> referencedBeansList) {
        if (dbObject == null) {
            return null;
        }
         this.embeddedBeansList = embeddedBeansList;
        this.referencedBeansList = referencedBeansList;
        ClassDescriptor classDescriptor = cache.get(clazz);
        Object object = classDescriptor.newInstance();
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            try {
                fieldDescriptor.getField().set(object,
                        fromDocumentRecursive(dbObject.get(fieldDescriptor.getName()), fieldDescriptor));
            } catch (Exception e) {
                throw new JmoordbException("Failed to set field value " + fieldDescriptor.getName(), e);
            }
        }
        return (T) object;
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @param dbObject
     * @return
     */
    public <T> T fromDocumentReferenced(Class<T> clazz, Document dbObject) {
        if (dbObject == null) {
            return null;
        }
        ClassDescriptor classDescriptor = cache.get(clazz);
        Object object = classDescriptor.newInstance();
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            try {
                fieldDescriptor.getField().set(object,
                        fromDocumentRecursive(dbObject.get(fieldDescriptor.getName()), fieldDescriptor));
            } catch (Exception e) {
                throw new JmoordbException("Failed to set field value " + fieldDescriptor.getName(), e);
            }
        }
        return (T) object;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object fromDocumentRecursive(Object dbObject, FieldDescriptor fieldDescriptor) {
        System.out.println("_____________________________________");
        System.out.println("  fromDBObjectRecursive: " + fieldDescriptor.getName());
        if (dbObject == null) {
            System.out.println("  dbObject == null" +fieldDescriptor.getDefaultValue());
            return fieldDescriptor.getDefaultValue();
        }
        
        

        Class<?> fieldType = fieldDescriptor.getField().getType();
        if (fieldDescriptor.isSimple()) {
            System.out.println("-------------> isSimple() "+fieldDescriptor.getSimpleValue(dbObject));
            return fieldDescriptor.getSimpleValue(dbObject);
        } else if (fieldDescriptor.isArray()) {
            System.out.println("--------------> isArray");
            BasicDBList dbList = (BasicDBList) dbObject;
            if (fieldType.getComponentType().isPrimitive()) {
                System.out.println("------------>isPrimitive");
                return ReflectionUtils.dbListToArrayOfPrimitives(dbList, fieldType);
            }
            List list = new ArrayList();
            for (Object listEl : dbList) {
                System.out.println("------> for dbList");
                if (listEl == null || ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    System.out.println("---------->  <A.1>");
                    list.add(listEl);
                } else {
                    System.out.println("---------->  <A.2>");
                    list.add(fromDocument((Class<Object>) fieldType.getComponentType(), (Document) listEl,embeddedBeansList,referencedBeansList));
                }
            }
            System.out.println("---------->  <A.3>");
            Object[] arrayPrototype = (Object[]) Array.newInstance(fieldType.getComponentType(), 0);
            return list.toArray(arrayPrototype);
        } else if (fieldDescriptor.isList()) {
            System.out.println("----------->isList()");
            BasicDBList dbList = (BasicDBList) dbObject;
            List list = (List) fieldDescriptor.newInstance();
            for (Object listEl : dbList) {
                System.out.println("-------------->  <B>");
                if (ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    System.out.println("-------------->  <B.1>");
                    list.add(listEl);
                } else {
                    System.out.println("---------->  <B.2>");
                    list.add(fromDocument(ReflectionUtils.genericType(fieldDescriptor.getField()), (Document) listEl,embeddedBeansList,referencedBeansList));
                }
            }
            System.out.println("---------->  <B.3>");
            return list;
        } else if (fieldDescriptor.isSet()) {
            System.out.println("------------->isSet()");
            BasicDBList dbList = (BasicDBList) dbObject;
            Set set = (Set) fieldDescriptor.newInstance();
            for (Object listEl : dbList) {

                if (ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    System.out.println("---------->  <C>");
                    set.add(listEl);
                } else {
                    System.out.println("---------->  <C.1>");
                    set.add(fromDocument(ReflectionUtils.genericType(fieldDescriptor.getField()), (Document) listEl,embeddedBeansList,referencedBeansList));
                }
            }
            return set;
        } else if (fieldDescriptor.isMap()) {
            
            DBObject dbMap = (DBObject) dbObject;
            Map map = (Map) fieldDescriptor.newInstance();
            for (Object key : dbMap.keySet()) {
            
                Object mapEl = dbMap.get(key.toString());
                if (mapEl == null || ReflectionUtils.isSimpleClass(mapEl.getClass())) {
            
                    map.put(key, mapEl);
                } else {
            
                    map.put(key,
                            fromDocument(ReflectionUtils.genericTypeOfMapValue(fieldDescriptor.getField()),
                                    (Document) mapEl,embeddedBeansList,referencedBeansList));
                }
            }
            return map;
        } else if (fieldDescriptor.isObject()) {
            System.out.println("   [isObject] "+fieldDescriptor.getName() +" ]");
            if (isEmbedded(fieldDescriptor.getName())) {
                System.out.println("[es Embebido]");
                System.out.println("--> creare una instancia nueva");
                Object object = fieldDescriptor.newInstance();
               
                for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                    System.out.println("       [childDescriptor.getField() " + childDescriptor.getField().getName() + " ]");
                    try {
                        childDescriptor.getField()
                                .set(object,
                                        fromDocumentRecursive(((Document) dbObject).get(childDescriptor.getName()),
                                                childDescriptor));
                    } catch (Exception e) {
                        throw new JmoordbException("Failed to set field value " + childDescriptor.getName(), e);
                    }
                }
                System.out.println("fromDBObjectRecursive.return object; " + object);
                return object;
            } else {
                if (isReferenced(fieldDescriptor.getName())) {
                    //Referenciado
                    System.out.println("     [es Referenciado]");
                    
                    ///---- usar reflexion Aqui
                     Object object = fieldDescriptor.newInstance();
                      AbstractFacade a = new AbstractFacade(object.getClass(), "fantasy", "continentes", true) {
                    @Override
                    protected MongoClient getMongoClient() {
                         MongoClient mongoClient = new MongoClient();
         return mongoClient;
                    }
                };
                Object c = a.find("idplaneta", "tr");
                System.out.println("paso c");
                    System.out.println("c="+c);
                for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                    System.out.println("        [childDescriptor.getField() " + childDescriptor.getField().getName() + " ]");
                    try {
                        childDescriptor.getField()
                                .set(object,
                                        fromDocumentRecursive(((Document) dbObject).get(childDescriptor.getName()),
                                                childDescriptor));
                    } catch (Exception e) {
                        throw new JmoordbException("Failed to set field value " + childDescriptor.getName(), e);
                    }
                }
                System.out.println("fromDBObjectRecursive.return object; " + object);
                return object;
                    
                    
                    
                }else{
                    System.out.println("                     [No es Referenced]");
                    new JmoordbException("@Embedded or @Reference is required for this field " + fieldDescriptor.getName());
                    return new Document();
                }
            }

        }

        return null;
    }

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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.couchbase.internal;

import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.util.ClassDescriptor;
import com.avbravo.jmoordb.util.ClassDescriptorsCache;
import com.avbravo.jmoordb.util.FieldDescriptor;
import com.avbravo.jmoordb.util.ReflectionUtils;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author avbravo
 */
public class JavaToDocumentCouchbase {

    private ClassDescriptorsCache cache = new ClassDescriptorsCache();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<ReferencedBeans> referencedBeansList = new ArrayList<>();
    ReferencedBeans referencedBeans = new ReferencedBeans();

    public JsonObject toDocument(Object obj, List<EmbeddedBeans> embeddedBeansList, List<ReferencedBeans> referencedBeansList) {
        // Test.msg("-----{{{toDocument}}----");
        if (obj == null) {
            return null;
        }
        this.embeddedBeansList = embeddedBeansList;
        this.referencedBeansList = referencedBeansList;

        JsonObject dbObject = JsonObject.empty();
        ClassDescriptor classDescriptor = cache.get(obj.getClass());
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            // Test.msg("[Analizando :  " + fieldDescriptor.getName() + " ]");
            dbObject.put(fieldDescriptor.getName(), toDBObjectRecursive(obj, fieldDescriptor, embeddedBeansList, referencedBeansList));

        }
        // Test.msg(" .....return dbObject " + dbObject.toString());
        return dbObject;
    }

    /**
     * Se utiliza cuando se pasan List<Entity> referenciados
     *
     * @param obj
     * @param idreferenciado
     * @return
     */
    private JsonObject toDocumentReferenced(Object obj, String idreferenciado) {
        if (obj == null) {
            return null;
        }

        JsonObject dbObject = JsonObject.empty();
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
        // Test.msg("           toDBObjectRecursive() " + fieldDescriptor.getName());
        if (object == null) {
            // Test.msg(" object == null");
            return null;
        }
        if (fieldDescriptor.isArray()) {

            // Test.msg(" isArray");
            if (ReflectionUtils.isSimpleClass(fieldDescriptor.getField().getType().getComponentType())) {
                return fieldDescriptor.getFieldValue(object);
            } else {
                Object[] array = (Object[]) fieldDescriptor.getFieldValue(object);
                List<JsonObject> fieldObj = new ArrayList<>();

                for (Object el : array) {
                    fieldObj.add(toDocument(el, embeddedBeansList, referencedBeansList));
                }
                return fieldObj;
            }
        } else if (fieldDescriptor.isIterable()) {
            // Test.msg(" -------------------isIterable---------------------------");

            Iterable col = (Iterable) fieldDescriptor.getFieldValue(object);
            //   JsonArray
            List<Object> content = new ArrayList<Object>();
            
            List<JsonObject> fieldObj = new ArrayList<>();

            if (col != null) {
                for (Object el : col) {

                    if (ReflectionUtils.isSimpleClass(el.getClass())) {
                        // Test.msg("     isIterable.isSimpleClass()");
                        JsonObject dbObject2 = JsonObject.empty();
                        dbObject2.put("", el);
//                        fieldObj.add(el);
                        fieldObj.add(dbObject2);
                    } else {

                        if (isEmbedded(fieldDescriptor.getName())) {
                            // Test.msg("     isIterable.isEmbedded()");
                            fieldObj.add(toDocument(el, embeddedBeansList, referencedBeansList));
                        } else {
                            if (isReferenced(fieldDescriptor.getName())) {
                                // Test.msg("     isIterable.isReferenced()");
                                //aris
                                ClassDescriptor classD = cache.get(el.getClass());
                                for (FieldDescriptor fieldDesc : classD.getFields()) {

                                    if (fieldDesc.getName().equals(referencedBeans.getField())) {
                                        fieldObj.add(toDocumentReferenced(el, referencedBeans.getField()));
                                    }
                                }
                                //aris

                            } else {
                                // Test.msg("..........no es embebido ni referenciado");
                            }
                        }

                    }
                }
            }
        

            return JsonArray.from(fieldObj);

        } else if (fieldDescriptor.isObject()) {

          
            if (isEmbedded(fieldDescriptor.getName())) {

                Object fieldValue = fieldDescriptor.getFieldValue(object);
                if (fieldValue == null) {
                    return null;
                }
                JsonObject dbObject = JsonObject.empty();
                for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                    dbObject.put(childDescriptor.getName(), toDBObjectRecursive(fieldValue, childDescriptor, embeddedBeansList, referencedBeansList));
                }
                return dbObject;

            } else {

                if (isReferenced(fieldDescriptor.getName())) {
              
                    Object fieldValue = fieldDescriptor.getFieldValue(object);
                    if (fieldValue == null) {
                        return null;
                    }
                    JsonObject dbObject = JsonObject.empty();
                    for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {

                        if (childDescriptor.getName().equals(referencedBeans.getField())) {
                            dbObject.put(childDescriptor.getName(), toDBObjectRecursive(fieldValue, childDescriptor, embeddedBeansList, referencedBeansList));
                        }

                    }
                    return dbObject;

                } else {
                    // Test.msg("                     [No es Referenced]");
                    new JmoordbException("@Embedded or @Reference is required for this field " + fieldDescriptor.getName());
                    return JsonObject.empty();
                }

            }

        } else if (fieldDescriptor.isMap()) {
//            // Test.msg("==========================");
//            // Test.msg("fieldDescriptor.isMap()");
//            // Test.msg("==========================");
            JsonObject dbObject = JsonObject.empty();
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
            // Test.msg("              " + fieldDescriptor.getFieldValue(object));
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

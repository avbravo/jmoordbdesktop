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
import com.mongodb.DBObject;
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
    public <T> T fromDBObject(Class<T> clazz, Document dbObject) {
        if (dbObject == null) {
            return null;
        }
        ClassDescriptor classDescriptor = cache.get(clazz);
        Object object = classDescriptor.newInstance();
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            try {
                fieldDescriptor.getField().set(object,
                        fromDBObjectRecursive(dbObject.get(fieldDescriptor.getName()), fieldDescriptor));
            } catch (Exception e) {
                throw new JmoordbException("Failed to set field value " + fieldDescriptor.getName(), e);
            }
        }
        return (T) object;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object fromDBObjectRecursive(Object dbObject, FieldDescriptor fieldDescriptor) {
        if (dbObject == null) {
            return fieldDescriptor.getDefaultValue();
        }
        Class<?> fieldType = fieldDescriptor.getField().getType();
        if (fieldDescriptor.isSimple()) {
            return fieldDescriptor.getSimpleValue(dbObject);
        } else if (fieldDescriptor.isArray()) {
            BasicDBList dbList = (BasicDBList) dbObject;
            if (fieldType.getComponentType().isPrimitive()) {
                return ReflectionUtils.dbListToArrayOfPrimitives(dbList, fieldType);
            }
            List list = new ArrayList();
            for (Object listEl : dbList) {
                if (listEl == null || ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    list.add(listEl);
                } else {
                    list.add(fromDBObject((Class<Object>) fieldType.getComponentType(), (Document) listEl));
                }
            }

            Object[] arrayPrototype = (Object[]) Array.newInstance(fieldType.getComponentType(), 0);
            return list.toArray(arrayPrototype);
        } else if (fieldDescriptor.isList()) {
            BasicDBList dbList = (BasicDBList) dbObject;
            List list = (List) fieldDescriptor.newInstance();
            for (Object listEl : dbList) {
                if (ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    list.add(listEl);
                } else {
                    list.add(fromDBObject(ReflectionUtils.genericType(fieldDescriptor.getField()), (Document) listEl));
                }
            }
            return list;
        } else if (fieldDescriptor.isSet()) {
            BasicDBList dbList = (BasicDBList) dbObject;
            Set set = (Set) fieldDescriptor.newInstance();
            for (Object listEl : dbList) {
                if (ReflectionUtils.isSimpleClass(listEl.getClass())) {
                    set.add(listEl);
                } else {
                    set.add(fromDBObject(ReflectionUtils.genericType(fieldDescriptor.getField()), (Document) listEl));
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
                            fromDBObject(ReflectionUtils.genericTypeOfMapValue(fieldDescriptor.getField()),
                                    (Document) mapEl));
                }
            }
            return map;
        } else if (fieldDescriptor.isObject()) {
            System.out.println("isObject()");
            Object object = fieldDescriptor.newInstance();
            for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                System.out.println("childDescriptor.getField() "+childDescriptor.getField());
                try {
                    childDescriptor.getField()
                            .set(object,
                                    fromDBObjectRecursive(((Document) dbObject).get(childDescriptor.getName()),
                                            childDescriptor));
                } catch (Exception e) {
                    throw new JmoordbException("Failed to set field value " + childDescriptor.getName(), e);
                }
            }
            System.out.println("fromDBObjectRecursive.return object; "+object);
            return object;
        }

        return null;
    }
}

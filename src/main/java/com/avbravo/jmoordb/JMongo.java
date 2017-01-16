package com.avbravo.jmoordb;

import com.avbravo.jmoordb.internal.ClassDescriptor;
import com.avbravo.jmoordb.internal.ClassDescriptorsCache;
import com.avbravo.jmoordb.internal.FieldDescriptor;
import com.avbravo.jmoordb.internal.ReflectionUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;



import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class JMongo {

    private ClassDescriptorsCache cache = new ClassDescriptorsCache();

    public DBObject toDBObject(Object obj) {
        if (obj == null) {
            return null;
        }
        DBObject dbObject = new BasicDBObject();
        ClassDescriptor classDescriptor = cache.get(obj.getClass());
        for (FieldDescriptor fieldDescriptor : classDescriptor.getFields()) {
            System.out.println("++++++++++++toDBObject() ++++++++++++++");
            System.out.println("            analizar -->fieldDescriptor.getName() " + fieldDescriptor.getName());
            dbObject.put(fieldDescriptor.getName(), toDBObjectRecursive(obj, fieldDescriptor));
            System.out.println("+++++++++++++++++++++++++++++++++++");
        }
        return dbObject;
    }

    @SuppressWarnings("rawtypes")
    public Object toDBObjectRecursive(Object object, FieldDescriptor fieldDescriptor) {
        if (object == null) {
            return null;
        }
        if (fieldDescriptor.isArray()) {
            System.out.println("==========================");
            System.out.println(" isArray");
            System.out.println("==========================");
            if (ReflectionUtils.isSimpleClass(fieldDescriptor.getField().getType().getComponentType())) {
                return fieldDescriptor.getFieldValue(object);
            } else {
                Object[] array = (Object[]) fieldDescriptor.getFieldValue(object);
                BasicDBList fieldObj = new BasicDBList();
                for (Object el : array) {
                    fieldObj.add(toDBObject(el));
                }
                return fieldObj;
            }
        } else if (fieldDescriptor.isIterable()) {
            System.out.println("==========================");
            System.out.println(" fieldDescriptor.isIterable()");
            System.out.println("==========================");
            Iterable col = (Iterable) fieldDescriptor.getFieldValue(object);
            BasicDBList fieldObj = new BasicDBList();
            if (col != null) {
                for (Object el : col) {
                    if (ReflectionUtils.isSimpleClass(el.getClass())) {
                        fieldObj.add(el);
                    } else {
                        fieldObj.add(toDBObject(el));
                    }
                }
            }
            return fieldObj;
        } else if (fieldDescriptor.isObject()) {
            System.out.println("................................................");
            System.out.println("if(fieldDescriptor.isObject())");

            Object fieldValue = fieldDescriptor.getFieldValue(object);
            if (fieldValue == null) {
                return null;
            }
            DBObject dbObject = new BasicDBObject();
            for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {

                System.out.println("      childDescriptor.getName() " + childDescriptor.getName() + " toDBObjectRecursive(fieldValue, childDescriptor) " + toDBObjectRecursive(fieldValue, childDescriptor));
                System.out.println("-------------------------------");

                dbObject.put(childDescriptor.getName(), toDBObjectRecursive(fieldValue, childDescriptor));
            }
            System.out.println("return dbObject " + dbObject);
            System.out.println("................................................");
            return dbObject;
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
                    dbObject.put(key.toString(), toDBObject(el));
                }
            }
            return dbObject;
        } else {
            System.out.println("       toDBObjectRecursive()->return fieldDescriptor.getFieldValue(object) " + fieldDescriptor.getFieldValue(object));
            return fieldDescriptor.getFieldValue(object);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T fromDBObject(Class<T> clazz, DBObject dbObject) {
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
                    list.add(fromDBObject((Class<Object>) fieldType.getComponentType(), (DBObject) listEl));
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
                    list.add(fromDBObject(ReflectionUtils.genericType(fieldDescriptor.getField()), (DBObject) listEl));
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
                    set.add(fromDBObject(ReflectionUtils.genericType(fieldDescriptor.getField()), (DBObject) listEl));
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
                                    (DBObject) mapEl));
                }
            }
            return map;
        } else if (fieldDescriptor.isObject()) {
            Object object = fieldDescriptor.newInstance();
            for (FieldDescriptor childDescriptor : fieldDescriptor.getChildren()) {
                try {
                    childDescriptor.getField()
                            .set(object,
                                    fromDBObjectRecursive(((DBObject) dbObject).get(childDescriptor.getName()),
                                            childDescriptor));
                } catch (Exception e) {
                    throw new JmoordbException("Failed to set field value " + childDescriptor.getName(), e);
                }
            }
            return object;
        }

        return null;
    }
}

package com.avbravo.jmoordb.util;

import com.avbravo.jmoordb.JmoordbException;
import com.avbravo.jmoordb.anotations.Ignore;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;



import com.mongodb.BasicDBList;

public class ReflectionUtils {
	public static boolean shouldPersist(Field field) {
		if (field.getAnnotation(Ignore.class) != null) {
			return false;
		}
		return !field.isSynthetic() && !Modifier.isStatic(field.getModifiers());
	}

	public static boolean isSimpleField(Field field) {
		Class<?> fieldClass = field.getType();
		return shouldPersist(field) && isSimpleClass(fieldClass);
	}

	public static boolean isSimpleClass(Class<?> fieldClass) {
		boolean isSimple = fieldClass.isPrimitive() || fieldClass.equals(String.class)
				|| Number.class.isAssignableFrom(fieldClass) || Date.class.isAssignableFrom(fieldClass)
				|| ObjectId.class.equals(fieldClass) || fieldClass.isEnum()
				|| Boolean.class.isAssignableFrom(fieldClass);
		return isSimple;
	}

	public static boolean isPrimitive(Field field) {
		return field.getType().isPrimitive();
	}

	public static boolean isNumber(Field field) {
		return Number.class.isAssignableFrom(field.getType());
	}

	public static boolean isBoolean(Field field) {
		return Boolean.class.isAssignableFrom(field.getType());
	}

	public static boolean isIterable(Field field) {
		return Iterable.class.isAssignableFrom(field.getType());
	}

	public static boolean isList(Field field) {
		return List.class.isAssignableFrom(field.getType());
	}

	public static boolean isSet(Field field) {
		return Set.class.isAssignableFrom(field.getType());
	}

	public static boolean isMap(Field field) {
		return Map.class.isAssignableFrom(field.getType());
	}

	public static boolean isArray(Field field) {
		return field.getType().isArray();
	}

	public static Object dbListToArrayOfPrimitives(BasicDBList dbList, Class<?> fieldType) {
		if (fieldType.equals(int[].class)) {
			int[] array = new int[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Integer) object).intValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(long[].class)) {
			long[] array = new long[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Long) object).longValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(double[].class)) {
			double[] array = new double[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Double) object).doubleValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(float[].class)) {
			float[] array = new float[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Float) object).floatValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(char[].class)) {
			char[] array = new char[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Character) object).charValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(byte[].class)) {
			byte[] array = new byte[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Byte) object).byteValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(short[].class)) {
			short[] array = new short[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Short) object).shortValue();
				i++;
			}
			return array;
		}
		if (fieldType.equals(boolean[].class)) {
			boolean[] array = new boolean[dbList.size()];
			int i = 0;
			for (Object object : dbList) {
				array[i] = ((Boolean) object).booleanValue();
				i++;
			}
			return array;
		}
		return null;
	}

	public static Class<?> genericType(Field field) {
		ParameterizedType paramzType = (ParameterizedType) field.getGenericType();
		Type genericType = paramzType.getActualTypeArguments()[0];
		Class<?> itemsType;
		if (genericType instanceof Class<?>) {
			itemsType = (Class<?>) genericType;
		} else if (genericType instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) genericType;
			itemsType = (Class<?>) wildcardType.getUpperBounds()[0];
		} else {
			throw new JmoordbException("Unexpected generic type " + genericType);
		}
		return itemsType;
	}

	public static Class<?> genericTypeOfMapValue(Field field) {
		ParameterizedType paramzType = (ParameterizedType) field.getGenericType();
		Type genericType = paramzType.getActualTypeArguments()[1];
		Class<?> itemsType;
		if (genericType instanceof Class<?>) {
			itemsType = (Class<?>) genericType;
		} else if (genericType instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) genericType;
			itemsType = (Class<?>) wildcardType.getUpperBounds()[0];
		} else {
			throw new JmoordbException("Unexpected generic type " + genericType);
		}
		return itemsType;
	}
}

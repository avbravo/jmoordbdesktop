package com.avbravo.jmoordb.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassDescriptorsCache {
	private final Map<String, ClassDescriptor> descriptors;

	public ClassDescriptorsCache() {
		descriptors = new ConcurrentHashMap<String, ClassDescriptor>();
	}

	public ClassDescriptor get(Class<?> clazz) {
		ClassDescriptor descriptor = descriptors.get(clazz.getCanonicalName());
		if (descriptor == null) {
			descriptor = ClassDescriptor.get(clazz);
			descriptors.put(clazz.getCanonicalName(), descriptor);
		}
		return descriptor;
	}

	int getSize() {
		return descriptors.size();
	}

}

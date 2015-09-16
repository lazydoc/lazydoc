package org.lazydoc.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Inspector {
	
	public static Class<?> getGenericClassOfList(Class<?> type, Type listGenericClass) {
        if(type.isArray()) {
            return type.getComponentType();
        }
		if (listGenericClass instanceof ParameterizedType) {
			for (Type typeArgument : ((ParameterizedType) listGenericClass).getActualTypeArguments()) {
				return (Class<?>) typeArgument;
			}
		}
		return null;
	}

    public static boolean isListSetOrArray(Class<?> propertyType) {
		return List.class.isAssignableFrom(propertyType) || Set.class.isAssignableFrom(propertyType) || propertyType.isArray();
	}

	public static boolean isMap(Class<?> propertyType) {
		return Map.class.isAssignableFrom(propertyType);
	}
}

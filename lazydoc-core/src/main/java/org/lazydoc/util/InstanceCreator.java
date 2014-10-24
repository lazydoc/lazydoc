package org.lazydoc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class InstanceCreator {

    private static Map<Object, Object> argumentTypes = new HashMap<>();

    static {
        argumentTypes.put(List.class, new ArrayList<>());
        argumentTypes.put(Set.class, new HashSet<>());
        argumentTypes.put(SortedSet.class, new TreeSet<>());
        argumentTypes.put(Map.class, new HashMap<>());
        argumentTypes.put(SortedMap.class, new TreeMap<>());
        argumentTypes.put(Boolean.class, true);
        argumentTypes.put(Boolean.TYPE, true);
        argumentTypes.put(Character.class, 'Z');
        argumentTypes.put(Character.TYPE, 'Z');
        argumentTypes.put(Byte.class, (byte) 10);
        argumentTypes.put(Byte.TYPE, (byte) 10);
        argumentTypes.put(Short.class, (short) 10);
        argumentTypes.put(Short.TYPE, (short) 10);
        argumentTypes.put(Integer.class, 10);
        argumentTypes.put(Integer.TYPE, 10);
        argumentTypes.put(Long.class, 10L);
        argumentTypes.put(Long.TYPE, 10L);
        argumentTypes.put(Float.class, 3.14159F);
        argumentTypes.put(Float.TYPE, 3.14159F);
        argumentTypes.put(Double.class, 3.14159);
        argumentTypes.put(Double.TYPE, 3.14159);
        argumentTypes.put(Calendar.class, Calendar.getInstance());
        argumentTypes.put(BigDecimal.class, new BigDecimal("3.14159"));
        argumentTypes.put(BigInteger.class, BigInteger.ZERO);
        argumentTypes.put(String.class, "DEFAULT");
        argumentTypes.put(Throwable.class, new Exception());
        argumentTypes.put(String[].class, new String[] { "DEFAULT" });
    }

    public static Object createInstanceOf(Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                try {
                    return constructor.newInstance(getParametersForTypes(constructor.getParameterTypes()));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    private static Object[] getParametersForTypes(Class<?>[] parameterTypes) {
        List<Object> parameterList = new ArrayList<Object>();
        for (Class<?> parameterType : parameterTypes) {
            Object parameter = argumentTypes.get(parameterType);
            if (parameter == null) {
                if (parameterType.isEnum()) {
                    parameter = getEnumParameter(parameterType);
                } else {
                    parameter = createInstanceOf(parameterType);
                }
            }
            parameterList.add(parameter);
        }
        Object[] parameters = parameterList.toArray();
        return parameters;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object getEnumParameter(Class parameterType) {
        for (Field field : parameterType.getDeclaredFields()) {
            if (field.isEnumConstant()) {
                return Enum.valueOf(parameterType, field.getName());
            }
        }
        return null;
    }

}

package org.lazydoc.parser;

import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lazydoc.annotation.IgnoreForDocumentation;
import org.lazydoc.annotation.PropertyDescription;
import org.lazydoc.annotation.Sample;
import org.lazydoc.model.DocDataType;
import org.lazydoc.model.DocParameter;
import org.lazydoc.model.DocProperty;
import org.lazydoc.reporter.DocumentationReporter;
import org.lazydoc.util.Inspector;

public class DataTypeParser {

	private static final Logger log = LogManager.getLogger(DataTypeParser.class);
	
	private Map<String, DocDataType> dataTypes = new TreeMap<>();
	
	private DocumentationReporter reporter;
    private Class<?> configuredBaseDTOClass = Object.class;
	
	public DataTypeParser(DocumentationReporter reporter, String configuredBaseDTOClass) {
		super();
		this.reporter = reporter;
		if(StringUtils.isNotBlank(configuredBaseDTOClass)) {
	        try {
	            this.configuredBaseDTOClass = Class.forName(configuredBaseDTOClass);
	        } catch (ClassNotFoundException e) {
	            log.debug("Could not find configured base DTO class "+configuredBaseDTOClass);
	        }
		}
    }

	public void addListDataTypeStubAndAddRealDataType(DocParameter docParameter, Class<?> parameterType, String dataTypeName) {
		String parameterTypeName = removeEnd(removeEnd(parameterType.getSimpleName(), "[]"), "VO");
		String listDataTypeName = parameterTypeName + "List";
		docParameter.setDataType(listDataTypeName);
		docParameter.setList(true);
		DocDataType dataType = new DocDataType();
		dataType.setName(listDataTypeName);
		dataType.setList(true);
		DocProperty property = new DocProperty();
		property.setDescription("List of " + parameterTypeName);
		property.setList(true);
		property.setType(dataTypeName);
		dataType.getProperties().add(property);
		dataTypes.put(dataType.getName(), dataType);
		addDataType(docParameter.getDataTypeClass().getComponentType());
	}
	
	public void addDataType(Class<?> clazz) {
		if (isJavaType(clazz)) {
			return;
		}

		log.debug("Inspecting data type " + clazz.getName());
		if (!isClassInstanceOfBaseVO(clazz) && isNotJavaType(clazz)) {
			throw new RuntimeException("Class " + clazz.getSimpleName() + " is not an inherited class of BaseVO");
		}
		DocDataType dataType = new DocDataType();
		dataType.setName(clazz.getSimpleName());
		addDataType(clazz, dataType);
		dataTypes.put(dataType.getName(), dataType);
	}

	private void addDataType(Class<?> clazz, DocDataType dataType) {
		BeanInfo beanInfo;
		try {
            if(clazz == null) {
                return;
            }
			if (isSuperClassNotBaseVO(clazz)) {
				addDataType(getSuperClassOfVO(clazz), dataType);
			}
			beanInfo = Introspector.getBeanInfo(clazz);
			for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
				Class<?> propertyType = descriptor.getPropertyType();
				Field propertyField = getPropertyField(clazz, descriptor);
				if (skipThisField(propertyField)) {
					continue;
				}
				DocProperty property = new DocProperty();
                addEnumValuesToProperty(propertyType, property, propertyField);
				property.setName(descriptor.getName());
				property.setType(getPropertyType(propertyType, propertyField));
				property.setRequired(isFieldRequired(propertyField));
				property.setRequest(isForRequest(propertyField));
				property.setResponse(isForResponse(propertyField));
				property.setDescription(getDescription(propertyField, property));
				property.setSample(getSample(propertyField));
				dataType.getProperties().add(property);
				addFurtherVOClasses(propertyType);
			}
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
	}

	private void addEnumValuesToProperty(Class<?> propertyType, DocProperty property, Field propertyField) {
		if (propertyType.isEnum()) {
			for (Enum<?> enumElement : (Enum[]) propertyType.getEnumConstants()) {
				property.addEnumValue(enumElement.toString());
			}
		} else if(Inspector.isListSetOrArray(propertyType)) {
            Class<?> genericClassOfList = Inspector.getGenericClassOfList(propertyType, propertyField.getGenericType());
            if (genericClassOfList.isEnum()) {
                for (Enum<?> enumElement : (Enum[]) genericClassOfList.getEnumConstants()) {
                    property.addEnumValue(enumElement.toString());
                }
            }
        }
	}

	private boolean isFieldRequired(Field propertyField) {
		if (propertyField.isAnnotationPresent(NotNull.class)) {
			return true;
		}
		PropertyDescription propertyDescription = getPropertyDescription(propertyField);
		if (propertyDescription != null) {
			return propertyDescription.required();
		}
		return false;
	}

	private boolean isSuperClassNotBaseVO(Class<?> clazz) {
        if(clazz.isArray()) {
            return clazz.getComponentType().equals(configuredBaseDTOClass);
        }
		return !clazz.getSuperclass().equals(configuredBaseDTOClass);
	}

	private String getPropertyType(Class<?> propertyType, Field propertyField) {
		if (propertyField != null) {
			PropertyDescription propertyDescription = getPropertyDescription(propertyField);
			if (propertyDescription != null && !propertyDescription.type().equals(void.class)) {
				addFurtherVOClasses(propertyDescription.type());
				return propertyDescription.type().getSimpleName();
			}
			if (Inspector.isListSetOrArray(propertyType)) {
				Class<?> genericClassOfList = Inspector.getGenericClassOfList(propertyType, propertyField.getGenericType());
				if (genericClassOfList.isEnum()) {
					return "List[String]";
				}
				addFurtherVOClasses(genericClassOfList);
				return "List[" + removeEnd(genericClassOfList.getSimpleName(), "VO") + "]";
			}
		}
		if (propertyType.isEnum()) {
			return "String";
		}
		if (propertyType.getSimpleName().equals("BigDecimal")) {
			return "Long";
		}
		if (propertyType.getSimpleName().equals("Integer")) {
			return "int";
		}
		return propertyType.getSimpleName();
	}
	
	private String[] getSample(Field propertyField) {
		if (propertyField != null && propertyField.isAnnotationPresent(Sample.class)) {
			return propertyField.getAnnotation(Sample.class).value();
		}
		return new String[] {};
	}

	private boolean isForRequest(Field propertyField) {
		if (propertyField != null && propertyField.isAnnotationPresent(PropertyDescription.class)) {
			PropertyDescription propertyDescription = getPropertyDescription(propertyField);
			return (!propertyDescription.onlyRequest() && !propertyDescription.onlyResponse()) || propertyDescription.onlyRequest();
		}
		return false;
	}

	private boolean isForResponse(Field propertyField) {
		if (propertyField != null && propertyField.isAnnotationPresent(PropertyDescription.class)) {
			PropertyDescription propertyDescription = getPropertyDescription(propertyField);
			return (!propertyDescription.onlyRequest() && !propertyDescription.onlyResponse()) || propertyDescription.onlyResponse();
		}
		return false;
	}

	private String getDescription(Field propertyField, DocProperty property) {
		String description = "";
		if (propertyField != null) {
			if (propertyField.isAnnotationPresent(PropertyDescription.class)) {
                reporter.addDocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
				PropertyDescription propertyDescription = getPropertyDescription(propertyField);
				description = propertyDescription.description();
				if (property.hasEnumValues() && propertyDescription.addPossibleEnumValues()) {
					description += " Possible values: " + property.getEnumValues();
				}
			} else {
				reporter.addUndocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
			}
		}
		return description;
	}

	private PropertyDescription getPropertyDescription(Field propertyField) {
		return propertyField.getAnnotation(PropertyDescription.class);
	}

	private Field getPropertyField(Class<?> clazz, PropertyDescriptor descriptor) {
		try {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(descriptor.getName())) {
					return field;
				}
			}
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		if (!descriptor.getName().equals("class")) {
			log.debug("ERROR: Could not find field for descriptor " + descriptor.getName() + " in class " + clazz.getSimpleName());
		}
		return null;
	}
	
	private void addFurtherVOClasses(Class<?> voClass) {
		if (isClassInstanceOfBaseVO(voClass)) {
			addDataType(voClass);
		}
	}

	private boolean isClassInstanceOfBaseVO(Class<?> voClass) {

		boolean isInstanceOf = configuredBaseDTOClass.isAssignableFrom(voClass);

		if (!isInstanceOf && isNotJavaType(voClass)) {
            if(Inspector.isListSetOrArray(voClass)) {
                return configuredBaseDTOClass.isAssignableFrom(Inspector.getGenericClassOfList(voClass, voClass));
            }
		}
		return isInstanceOf;
	}

	private boolean isNotJavaType(Class<?> clazz) {
		return !isJavaType(clazz);
	}

	private boolean isJavaType(Class<?> clazz) {
		return clazz.getName().startsWith("java.") || clazz.isEnum() || clazz.equals(void.class) || clazz.isPrimitive();
	}

    private Class<?> getSuperClassOfVO(Class<?> clazz) {
        if(clazz.isArray()) {
            return clazz.getComponentType().getSuperclass();
        }
        return clazz.getSuperclass();
    }

    private boolean skipThisField(Field propertyField) {
        if(propertyField == null) {
            return true;
        }
        if(propertyField.isAnnotationPresent(IgnoreForDocumentation.class)) {
            reporter.addIgnoredField(propertyField.getDeclaringClass(), propertyField.getName());
            return true;
        }
		return false;
	}

    public Map<String, DocDataType> getDataTypes() {
		return dataTypes;
	}
    



}

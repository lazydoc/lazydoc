package org.lazydoc.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.lazydoc.annotation.IgnoreForDocumentation;
import org.lazydoc.annotation.PropertyDescription;
import org.lazydoc.annotation.PropertyMapDescription;
import org.lazydoc.annotation.Sample;
import org.lazydoc.model.DocDataType;
import org.lazydoc.model.DocParameter;
import org.lazydoc.model.DocProperty;
import org.lazydoc.reporter.DocumentationReporter;
import org.lazydoc.util.Inspector;

import javax.validation.constraints.NotNull;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.removeEnd;

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
				PropertyDescription propertyDescription = getPropertyDescription(propertyField, descriptor);
				if (skipThisField(propertyField, propertyDescription, descriptor) || propertyDescription == null) {
					continue;
				}
				DocProperty property = new DocProperty();
                addEnumValuesToProperty(propertyType, property, propertyField);
				property.setName(descriptor.getName());
				property.setOrder(propertyDescription.order());
				property.setRequired(isFieldRequired(propertyField, propertyDescription));
				property.setRequest(isForRequest(propertyDescription, descriptor));
				property.setResponse(isForResponse(propertyDescription, descriptor));
				property.setDescription(getDescription(propertyField, property, propertyDescription));
				property.setSample(getSample(propertyField, descriptor));
				property.setDeprecated(isDeprecated(propertyField, descriptor));
				property.setType(getPropertyType(propertyType, propertyField));
				property.setList(Inspector.isListSetOrArray(propertyType));
				property.setPrimitive(propertyType.isPrimitive());
				property.setRequestNullValueSample(allowNullValueSample(descriptor.getReadMethod()));
				property.setRequestNullValueSample(allowNullValueSample(descriptor.getWriteMethod()));
				if (Inspector.isMap(propertyType)) {
					property.setMap(true);
					if(propertyField.isAnnotationPresent(PropertyMapDescription.class)) {
						PropertyMapDescription mapDescription = propertyField.getAnnotation(PropertyMapDescription.class);
						property.setMapKeyDescription(mapDescription.keyDescription());
						property.setMapValueDescription(mapDescription.valueDescription());
					}
				}
				dataType.setNullValuesInSample(allowNullValuesInSample(clazz));
				dataType.getProperties().add(property);
				addFurtherVOClasses(propertyType);
			}
			if(clazz.isAnnotationPresent(JsonPropertyOrder.class)) {
				JsonPropertyOrder propertyOrder = clazz.getAnnotation(JsonPropertyOrder.class);
				int order = 1;
				for (String propertyName : propertyOrder.value()) {
					DocProperty property = getPropertyByName(propertyName, dataType.getProperties());
					if(property != null) {
						property.setOrder(order++);
					} else {
						log.warn("Property "+propertyName+" not found in property list of type "+dataType.getName());
					}

				}
			}
			Collections.sort(dataType.getProperties());
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean allowNullValueSample(Method method) {
		if(method != null && method.isAnnotationPresent(JsonSerialize.class)) {
			JsonSerialize jsonSerialize = method.getAnnotation(JsonSerialize.class);
			// TODO include modern way
			if(jsonSerialize.include().equals(JsonSerialize.Inclusion.NON_NULL)) {
				return false;
			}
		}
		return true;
	}

	private boolean allowNullValuesInSample(Class<?> clazz) {
		if(clazz != null && clazz.isAnnotationPresent(JsonSerialize.class)) {
			JsonSerialize jsonSerialize = clazz.getAnnotation(JsonSerialize.class);
			// TODO include modern way
			if(jsonSerialize.include().equals(JsonSerialize.Inclusion.NON_NULL)) {
				return false;
			}
		}
		return true;
	}


	private PropertyDescription getPropertyDescription(Method method) {
		return method != null ? method.getAnnotation(PropertyDescription.class) : null;
	}

	private DocProperty getPropertyByName(String propertyName, List<DocProperty> properties) {
		for (DocProperty property : properties) {
			if(property.getName().equals(propertyName)) {
				return property;
			}
		}
		return null;
	}

	private boolean isDeprecated(Field propertyField, PropertyDescriptor property) {
		if(propertyField != null && propertyField.isAnnotationPresent(Deprecated.class)) {
			return true;
		}
		boolean readMethodIsDeprecated = property.getReadMethod() != null && property.getReadMethod().isAnnotationPresent(Deprecated.class);
		boolean writeMethodIsDeprecated = property.getWriteMethod() != null && property.getWriteMethod().isAnnotationPresent(Deprecated.class);
		return readMethodIsDeprecated || writeMethodIsDeprecated;
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

	private boolean isFieldRequired(Field propertyField, PropertyDescription propertyDescription) {
		if (propertyField != null && (propertyField.isAnnotationPresent(NotNull.class) || propertyField.isAnnotationPresent(NotEmpty.class) || propertyField.isAnnotationPresent(NotBlank.class))) {
			return true;
		}
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
					return "String";
				}
				addFurtherVOClasses(genericClassOfList);
				return removeEnd(genericClassOfList.getSimpleName(), "VO");
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
	
	private String[] getSample(Field propertyField, PropertyDescriptor descriptor) {
		if (propertyField != null && propertyField.isAnnotationPresent(Sample.class)) {
			return propertyField.getAnnotation(Sample.class).value();
		}
		Method readMethod = descriptor.getReadMethod();
		Method writeMethod = descriptor.getWriteMethod();
		if(readMethod != null && readMethod.isAnnotationPresent(Sample.class)) {
			return readMethod.getAnnotation(Sample.class).value();
		}
		if (writeMethod != null &&writeMethod.isAnnotationPresent(Sample.class)) {
			return writeMethod.getAnnotation(Sample.class).value();
		}
		return new String[] {};
	}

	private boolean isForRequest(PropertyDescription propertyDescription, PropertyDescriptor descriptor) {
		if(descriptor.getWriteMethod() != null && descriptor.getWriteMethod().isAnnotationPresent(JsonIgnore.class)) {
			return false;
		}
		return (!propertyDescription.onlyRequest() && !propertyDescription.onlyResponse()) || propertyDescription.onlyRequest();
	}

	private boolean isForResponse(PropertyDescription propertyDescription, PropertyDescriptor descriptor) {
		if(descriptor.getReadMethod() != null && descriptor.getReadMethod().isAnnotationPresent(JsonIgnore.class)) {
			return false;
		}
		return (!propertyDescription.onlyRequest() && !propertyDescription.onlyResponse()) || propertyDescription.onlyResponse();
	}

	private String getDescription(Field propertyField, DocProperty property, PropertyDescription propertyDescription) {
		String description = "";
		if (propertyField != null) {
			if (propertyField.isAnnotationPresent(PropertyDescription.class)) {
                reporter.addDocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
				description = propertyDescription.description();
				// TODO Move to doumentwriter
				if (property.hasEnumValues() && propertyDescription.addPossibleEnumValues()) {
					description += " Possible values: " + property.getEnumValues();
				}
			} else {
				reporter.addUndocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
			}
		} else {
			description = propertyDescription.description();
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

    private boolean skipThisField(Field propertyField, PropertyDescription propertyDescription, PropertyDescriptor descriptor) {
        if(propertyField != null && propertyField.isAnnotationPresent(IgnoreForDocumentation.class)) {
            reporter.addIgnoredField(propertyField.getDeclaringClass(), propertyField.getName());
            return true;
        }
		if (propertyDescription == null) {
			Method readMethod = descriptor.getReadMethod();
			Method writeMethod = descriptor.getWriteMethod();
			if(readMethod != null && readMethod.isAnnotationPresent(JsonIgnore.class)) {
				if(writeMethod != null && writeMethod.isAnnotationPresent(JsonProperty.class)) {
					reporter.addUndocumentedField(readMethod != null ? readMethod.getDeclaringClass() : writeMethod.getDeclaringClass(), descriptor.getName());
					return true;
				}
				reporter.addIgnoredField(readMethod != null ? readMethod.getDeclaringClass() : writeMethod.getDeclaringClass(), descriptor.getName());
				return true;
			}
			if(propertyField != null) {
				reporter.addUndocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
				return true;
			} else {
				reporter.addUndocumentedField(readMethod != null ? readMethod.getDeclaringClass() : writeMethod.getDeclaringClass(), descriptor.getName());
				return true;
			}
		}
		return false;
	}

	private PropertyDescription getPropertyDescription(Field propertyField, PropertyDescriptor descriptor) {
		PropertyDescription propertyDescription = null;
		if(propertyField == null) {
			Method readMethod = descriptor.getReadMethod();
			propertyDescription = getPropertyDescription(readMethod);
			if (propertyDescription == null) {
				propertyDescription = getPropertyDescription(descriptor.getWriteMethod());
				reporter.addDocumentedMethod(readMethod != null ? readMethod.getDeclaringClass() : descriptor.getWriteMethod().getDeclaringClass(), descriptor.getName());
			}
		} else {
			propertyDescription = getPropertyDescription(propertyField);
			if(propertyDescription != null) {
				reporter.addDocumentedField(propertyField.getDeclaringClass(), propertyField.getName());
			}
		}
		return propertyDescription;
	}


	public Map<String, DocDataType> getDataTypes() {
		return dataTypes;
	}
    



}

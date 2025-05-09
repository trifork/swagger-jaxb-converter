package com.trifork.swagger.jaxb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

public class JaxbModelConverter implements ModelConverter {

    // No-arg constructor for ServiceLoader SPI (required)
    public JaxbModelConverter() {
    }

    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType _type = Json.mapper().constructType(type.getType());
        if (_type != null) {
            Class<?> rawClass = _type.getRawClass();

            XmlType xmlType = rawClass.getAnnotation(XmlType.class);

            if (xmlType != null) {
                String schemaName = xmlType.name();
                Schema model = new Schema().name(schemaName);

                XmlEnum xmlEnum = rawClass.getAnnotation(XmlEnum.class);
                if (xmlEnum != null) {
                    List<String> enumValues = getXmlEnumValues((Class<? extends Enum<?>>) rawClass);
                    model.setEnum(enumValues);
                    model.setType("string");
                    model.addType("string");

                } else {

                    model.setType("object");
                    model.addType("object");
                    model.setProperties(new LinkedHashMap<>());

                    // Scan for JAXB annotations like @XmlElement, @XmlAttribute
                    for (Field field : rawClass.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()))
                            continue; // Skip static fields

                        String name = field.getName();
                        Class<?> fieldType = field.getType();

                        Schema<?> fieldModel;
                        if (List.class.isAssignableFrom(fieldType)) {
                            // A multivalued field
                            Class<?> elementType = getElementType(field);

                            Schema<?> fieldItemModel = context.resolve(new AnnotatedType(elementType));
                            fieldModel = new ArraySchema().items(fieldItemModel);

                        } else {
                            fieldModel = context.resolve(new AnnotatedType(fieldType));
                        }

                        XmlElement xmlElement = field.getAnnotation(XmlElement.class);
                        XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
                        XmlValue xmlValue = field.getAnnotation(XmlValue.class);

                        if (xmlElement != null && !"##default".equals(xmlElement.name())) {
                            name = xmlElement.name();
                            model.getProperties().put(name, fieldModel);
                            if (xmlElement.required()) {
                                model.addRequiredItem(name);
                            }
                        } else if (xmlAttribute != null) {
                            name = xmlAttribute.name();
                            if (xmlAttribute.required()) {
                                model.addRequiredItem(name);
                            }
                        } else if (xmlValue != null) {
                            name = "$";
                        }

                         model.getProperties().put(name, fieldModel);

                    }

                    // Reorder properties based on the `propOrder` from @XmlType
                    if (xmlType != null && xmlType.propOrder().length > 0) {
                        model.setProperties(reorderProperties(model.getProperties(), xmlType.propOrder()));
                    }

                }

                context.defineModel(schemaName, model, type, null);

                model = new Schema().$ref(Components.COMPONENTS_SCHEMAS_REF + schemaName);
                return model;
            }

        }

        if (chain.hasNext())

        {
            return chain.next().resolve(type, context, chain);
        } else {
            return null;
        }
    }

    private List<String> getXmlEnumValues(Class<? extends Enum<?>> enumClass) {
        List<String> result = new ArrayList<>();

        for (Enum<?> constant : enumClass.getEnumConstants()) {
            try {
                Field field = enumClass.getField(constant.name());
                XmlEnumValue annotation = field.getAnnotation(XmlEnumValue.class);
                String value = annotation != null ? annotation.value() : constant.name();
                result.add(value);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Enum constant not found as field: " + constant.name(), e);
            }
        }

        return result;
    }

    private Class<?> getElementType(Field field) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type rawType = pt.getRawType();

            if (rawType instanceof Class && List.class.isAssignableFrom((Class<?>) rawType)) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length == 1) {
                    Type elementType = typeArgs[0];
                    if (elementType instanceof Class) {
                        return (Class<?>) elementType;
                    } else if (elementType instanceof ParameterizedType) {
                        // Handle cases like List<Map<String, Object>>
                        return (Class<?>) ((ParameterizedType) elementType).getRawType();
                    }
                }
            }
        }

        return null; // Not a parameterized List or couldn't determine
    }

    private LinkedHashMap<String, Schema> reorderProperties(Map<String, Schema> props, String[] order) {
        LinkedHashMap<String, Schema> sorted = new LinkedHashMap<>();
        Set<String> added = new HashSet<>();

        // First, add the properties in the order specified by propOrder
        for (String key : order) {
            if (props.containsKey(key)) {
                sorted.put(key, props.get(key));
                added.add(key);
            }
        }

        // Add any remaining properties that were not reordered
        for (Map.Entry<String, Schema> entry : props.entrySet()) {
            if (!added.contains(entry.getKey())) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }

        return sorted;
    }
}
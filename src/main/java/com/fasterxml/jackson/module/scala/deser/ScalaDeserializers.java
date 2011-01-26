package com.fasterxml.jackson.module.scala.deser;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.BeanDescription;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.Deserializers;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.deser.CustomDeserializerFactory;
import org.codehaus.jackson.map.deser.EnumSetDeserializer;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.type.ArrayType;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.EnumSet;

/**
 */
public class ScalaDeserializers extends CustomDeserializerFactory implements Deserializers {

	public JsonDeserializer<?> findArrayDeserializer(ArrayType arrayType, DeserializationConfig deserializationConfig, DeserializerProvider deserializerProvider, BeanProperty beanProperty, TypeDeserializer typeDeserializer, JsonDeserializer<?> jsonDeserializer) throws JsonMappingException {
		return null;
	}

	public JsonDeserializer<?> findCollectionDeserializer(CollectionType type,
														  DeserializationConfig config,
														  DeserializerProvider deserializerProvider,
														  BeanDescription beanDescription,
														  BeanProperty beanProperty,
														  TypeDeserializer typeDeserializer,
														  JsonDeserializer<?> jsonDeserializer) throws JsonMappingException {

		if( type.getRawClass() != scala.collection.immutable.List.class ) // filter non-scala stuff to superclass
			return super.createCollectionDeserializer(config,type,deserializerProvider);

		Class<?> collectionClass = type.getRawClass();
		BasicBeanDescription beanDesc = config.introspectClassAnnotations(collectionClass);
		// Explicit deserializer to use? (@JsonDeserialize.using)
		JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, beanDesc.getClassInfo(), beanProperty);
		if (deser != null) {
			return deser;
		}
		// If not, any type modifiers? (@JsonDeserialize.as)
		type = modifyTypeByAnnotation(config, beanDesc.getClassInfo(), type, null);

		JavaType contentType = type.getContentType();
		// Very first thing: is deserializer hard-coded for elements?
		JsonDeserializer<Object> contentDeser = contentType.getValueHandler();

		if (contentDeser == null) { // not defined by annotation
			// One special type: EnumSet:
			if (EnumSet.class.isAssignableFrom(collectionClass)) {
				return new EnumSetDeserializer(constructEnumResolver(contentType.getRawClass(), config));
			}
			// But otherwise we can just use a generic value deserializer:
			// 'null' -> collections have no referring fields
			contentDeser = deserializerProvider.findValueDeserializer(config, contentType, type, null);
		}

		/* One twist: if we are being asked to instantiate an interface or
				 * abstract Collection, we need to either find something that implements
				 * the thing, or give up.
				 *
				 * Note that we do NOT try to guess based on secondary interfaces
				 * here; that would probably not work correctly since casts would
				 * fail later on (as the primary type is not the interface we'd
				 * be implementing)
				 */
		/*
				 * GWZ 11/9/10  Disregard this abstract check in the case of a Scala Collection, which are usually abstract in Java.
				 *    No worries...Scala knows what to do with them!
				 *
				if (type.isInterface() || type.isAbstract()) {
					@SuppressWarnings("unchecked")
					Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
					if (fallback == null) {
						throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
					}
					collectionClass = fallback;
				}
				*/
		// Then optional type info (1.5): if type has been resolved, we may already know type deserializer:

		TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
		// but if not, may still be possible to find:
		if (contentTypeDeser == null) {
			contentTypeDeser = findTypeDeserializer(config, contentType);
		}

		boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
		@SuppressWarnings("unchecked")
		Constructor<Collection<Object>> ctor = ClassUtil.findConstructor((Class<Collection<Object>>)collectionClass, fixAccess);
		return new ScalaListDeserializer(type, contentDeser, contentTypeDeser, ctor);
	}

	public JsonDeserializer<?> findEnumDeserializer(Class<?> aClass, DeserializationConfig deserializationConfig, BeanDescription beanDescription, BeanProperty beanProperty) throws JsonMappingException {
		return null;
	}

	public JsonDeserializer<?> findMapDeserializer(MapType type,
												   DeserializationConfig config,
												   DeserializerProvider deserializerProvider,
												   BeanDescription beanDescription,
												   BeanProperty beanProperty,
												   KeyDeserializer keyDeserializer,
												   TypeDeserializer typeDeserializer,
												   JsonDeserializer<?> jsonDeserializer) throws JsonMappingException {

		if( type.getRawClass() != scala.collection.immutable.Map.class ) // filter non-scala stuff to superclass
			return super.createMapDeserializer(config,type,deserializerProvider);

		Class<?> mapClass = type.getRawClass();

		BasicBeanDescription beanDesc = config.introspectForCreation(type);

		// If not, any type modifiers? (@JsonDeserialize.as)
		type = modifyTypeByAnnotation(config, beanDesc.getClassInfo(), type, null);

		JavaType keyType = type.getKeyType();
		JavaType contentType = type.getContentType();

		// First: is there annotation-specified deserializer for values?
		@SuppressWarnings("unchecked")
		JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();
		if (contentDeser == null) { // nope...
			// 'null' -> maps have no referring fields
			contentDeser = deserializerProvider.findValueDeserializer(config, contentType, type, null);
		}

		// Otherwise, generic handler works ok.
		// Ok: need a key deserializer (null indicates 'default' here)
		KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
		if (keyDes == null) {
			keyDes = deserializerProvider.findKeyDeserializer(config, keyType);
		}

		// Cut abstract check out of original... Scala knows what to do with these.

		// [JACKSON-153]: allow use of @JsonCreator
		boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);

		// Then optional type info (1.5); either attached to type, or resolve separately:
		TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
		// but if not, may still be possible to find:
		if (contentTypeDeser == null) {
			contentTypeDeser = findTypeDeserializer(config, contentType);
		}

		return new ScalaMapDeserializer(type, contentDeser, contentTypeDeser, null);
	}

	public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> aClass, DeserializationConfig deserializationConfig, BeanProperty beanProperty) throws JsonMappingException {
		return null;
	}

	public JsonDeserializer<?> findBeanDeserializer(JavaType type,
													DeserializationConfig config,
													DeserializerProvider deserializerProvider,
													BeanDescription beanDescription,
													BeanProperty beanProperty) throws JsonMappingException {

		if( type.getRawClass() != scala.Option.class && type.getRawClass() != scala.Enumeration.class )
			return super.createBeanDeserializer(config, type, deserializerProvider);
		if( type.getRawClass() == scala.Option.class ) {
			BasicBeanDescription beanDesc = config.introspect(type);
			TypeDeserializer contentTypeDeser = findTypeDeserializer(config, type.containedType(0));
			JsonDeserializer<Object> contentDeser = deserializerProvider.findValueDeserializer(config, type.containedType(0), type, null);
			return new ScalaOptionDeserializer(config, type, contentDeser, beanDesc);
		}
		return new ScalaEnumerationDeserializer(type);
	}

	public ScalaDeserializers(Module.SetupContext context) {
	}
}

package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.databind.{PropertyNamingStrategy, PropertyName, AnnotationIntrospector, JavaType}
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect._

import com.fasterxml.jackson.module.scala.util.Implicts._

import scala.collection.JavaConverters._
import scala.collection.mutable

import java.util
import reflect.NameTransformer
import com.fasterxml.jackson.databind.util.BeanUtil
import java.lang.reflect.Field

class ScalaPropertiesCollector(config: MapperConfig[_],
                               forSerialization: Boolean,
                               `type`: JavaType,
                               classDef: AnnotatedClass,
                               mutatorPrefix: String)
  extends POJOPropertiesCollector(config, forSerialization, `type`, classDef, mutatorPrefix) {

  private [this] val _descriptor = BeanIntrospector(`type`.getRawClass)

  private [this] val _propertyIntrospector =
    Option(_annotationIntrospector).map(ai => new ScalaPropertyIntrospector(ai))

  private [this] val _getPropertyName: (PropertyDescriptor) => Option[PropertyName] =
    (_propertyIntrospector, _forSerialization) match {
      case (None, _) => (m) => None
      case (Some(pi), true) => (pi.findNameForSerialization _)
      case (Some(pi), false) => (pi.findNameForDeserialization _)
    }

  private [this] val _hasIgnoreMarker: (AnnotatedMember) => Boolean =
    Option(_annotationIntrospector).map(ai => (m: AnnotatedMember) => ai.hasIgnoreMarker(m)).getOrElse((x: AnnotatedMember) => false)

  private [this] lazy val anyGetters: mutable.Buffer[AnnotatedMember] = {
    if (_anyGetters == null) {
      _anyGetters = new util.LinkedList[AnnotatedMember]()
    }
    _anyGetters.asScala
  }

  private [this] lazy val anySetters: mutable.Buffer[AnnotatedMethod] = {
    if (_anySetters == null) {
      _anySetters = new util.LinkedList[AnnotatedMethod]()
    }
    _anySetters.asScala
  }

  private [this] lazy val jsonValueGetters: mutable.Buffer[AnnotatedMethod] = {
    if (_jsonValueGetters == null) {
      _jsonValueGetters = new util.LinkedList[AnnotatedMethod]()
    }
    _jsonValueGetters.asScala
  }

  private [this] lazy val creatorProperties: mutable.Buffer[POJOPropertyBuilder] = {
    if (_creatorProperties == null) {
      _creatorProperties = new util.LinkedList[POJOPropertyBuilder]()
    }
    _creatorProperties.asScala
  }

  override def _addFields() {
    val fields = classDef.fields().asScala
    lazy val ctors = classDef.getConstructors.asScala
    lazy val methods = classDef.memberMethods().asScala

    _descriptor.properties collect {
      case pd @ PropertyDescriptor(name, optParam, Some(f), optGetter, optSetter) => {
        val pn = _getPropertyName(pd)
        val explName = pn.optMap(_.getSimpleName).map(_.orIfEmpty(name))

        val annotatedParam = optParam.map { cp =>
          ctors.find(_.getAnnotated == cp.constructor).get.getParameter(cp.index)
        }
        val annotatedField = fields.find(_.getMember == f).get
        val prop = _addField(name, explName, annotatedField)
        optParam.foreach { cp => _addFieldCtor(prop, annotatedParam.get, explName) }
        optGetter.foreach { g => _addGetterMethod(prop, explName, methods.find(_.getMember == g).get) }
        optSetter.foreach { s => _addSetterMethod(prop, explName, methods.find(_.getMember == s).get) }
      }
    }
  }

  private def _addField(implName: String, explName: Option[String], field: AnnotatedField) = {
    val visible = explName.isDefined || _visibilityChecker.isFieldVisible(field)
    val ignored = _hasIgnoreMarker(field)
    val prop = _property(implName)
    prop.addField(field, explName.orNull, visible, ignored)
    prop
  }

  private def _addFieldCtor(prop: POJOPropertyBuilder, param: AnnotatedParameter, explName: Option[String]) {
    prop.addCtor(param, explName.orNull, true, false)
    creatorProperties += prop
  }

  private def _isPropertyHandled(m: AnnotatedMethod): Boolean = {
    _properties.asScala.exists {
      case (_, p) => m.equals(p.getGetter) || m.equals(p.getSetter) || m.equals(p.getMutator)
    }
  }

  override def _addMethods() {
    val ai = Option(_annotationIntrospector)
    val methods = classDef.memberMethods().asScala

    _descriptor.properties collect {
      case pd @ PropertyDescriptor(name, _, None, Some(g), optSetter) =>
        val pn = _getPropertyName(pd)
        val explName = pn.optMap(_.getSimpleName).map(_.orIfEmpty(name))
        val prop = _property(name)
        _addGetterMethod(prop, explName, methods.find(_.getMember == g).get)
        optSetter.foreach(s => _addSetterMethod(prop, explName, methods.find(_.getMember == s).get))
    }

    // Any method we haven't dealt with yet, handle as the base class would
    // This should filter out any @BeanProperty generated methods for fields
    // we've already detected. TODO: Maybe fold those into PropertyDescriptor?
    methods.filterNot(_isPropertyHandled).foreach { m =>
      m.getParameterCount match {
        case 0 => _addGetterMethod(m, ai.orNull)
        case 1 => _addSetterMethod(m, ai.orNull)
        case 2 if ai.map(_.hasAnySetterAnnotation(m)).getOrElse(false) => anySetters += m
        case _ => // do nothing
      }
    }
  }

  /*
   * This is essentially the base class function, except that we don't check
   * BeanUtils to see if the name is permissible as a setter method (Scala
   * has different rules there and ScalaBeans has already vetted the method).
   */
  private def _addGetterMethod(prop: POJOPropertyBuilder, explName: Option[String], m: AnnotatedMethod) {
    Option(_annotationIntrospector) match {
      case Some(ai) => {
        if (ai.hasAnyGetterAnnotation(m)) {
          anyGetters += m
          return
        }
        if (ai.hasAsValueAnnotation(m)) {
          jsonValueGetters += m
          return
        }
      }
      case None =>
    }

    val visible = explName.isDefined ||  _visibilityChecker.isGetterVisible(m)
    val ignore = _hasIgnoreMarker(m)
    prop.addGetter(m, explName.orNull, visible, ignore)
  }

  private def _addSetterMethod(prop: POJOPropertyBuilder, explName: Option[String], m: AnnotatedMethod) {
    val visible = explName.isDefined || _visibilityChecker.isSetterVisible(m)
    val ignore = _hasIgnoreMarker(m)
    prop.addSetter(m, explName.orNull, visible, ignore)
  }

  /**
   * Similar to the base implementation, but checks for annotations in a different
   * order due to scalas annotation application rules.
   * @param naming The PropertyNamingStrategy to query for names
   */
  override protected def _renameUsing(naming: PropertyNamingStrategy) {
    val props = _properties.values().toArray(Array.ofDim[POJOPropertyBuilder](_properties.size()))
    _properties.clear()

    for (prop <- props) {
      var name = prop.getName
      if (prop.hasConstructorParameter) {
        name = naming.nameForConstructorParameter(_config, prop.getConstructorParameter, name)
      } else if (prop.hasField) {
        name = naming.nameForField(_config, prop.getField, name)
      } else if (_forSerialization && prop.hasGetter) {
        name = naming.nameForGetterMethod(_config, prop.getGetter, name)
      } else if (!_forSerialization && prop.hasSetter) {
        name = naming.nameForSetterMethod(_config, prop.getSetter, name)
      }
      val newProp = if (name != prop.getName) prop.withName(name) else prop
      val old = _properties.get(name)
      if (old == null) {
        _properties.put(name, newProp)
      }
      else {
        old.addAll(newProp)
      }
      if (newProp != prop) {
        val idx = _creatorProperties.indexOf(prop)
        if (idx != -1) {
          _creatorProperties.set(idx, newProp)
        }
      }
    }
  }


  class ScalaPropertyIntrospector(ai: AnnotationIntrospector) {

    require(ai != null, "Argument ai must be non-null")

    private [this] val _ctors = classDef.getConstructors.asScala
    private [this] val _fields = classDef.fields().asScala
    private [this] val _methods = classDef.memberMethods().asScala

    def findNameForSerialization(prop: PropertyDescriptor): Option[PropertyName] = {
      prop match {
        case PropertyDescriptor(name, optParam, Some(f), _, _) => {
          val annotatedParam = optParam.flatMap { cp =>
            _ctors.find(_.getAnnotated == cp.constructor).map(_.getParameter(cp.index))
          }
          val annotatedField = _fields.find(_.getMember == f)
          val paramName = annotatedParam.optMap(ai.findNameForDeserialization(_))
          val fieldName = annotatedField.optMap(ai.findNameForSerialization(_))
          fieldName orElse paramName
        }

        case PropertyDescriptor(name, _, None, Some(g), _) =>
          _methods.find(_.getMember == g).optMap(ai.findNameForSerialization(_))

        case _ => None
      }
    }

    def findNameForDeserialization(prop: PropertyDescriptor): Option[PropertyName] = {
      prop match {
        case PropertyDescriptor(name, optParam, Some(f), _, _) => {
          val annotatedParam = optParam.flatMap { cp =>
            _ctors.find(_.getAnnotated == cp.constructor).map(_.getParameter(cp.index))
          }
          val annotatedField = _fields.find(_.getMember == f)
          val paramName = annotatedParam.optMap(ai.findNameForDeserialization(_))
          val fieldName = annotatedField.optMap(ai.findNameForDeserialization(_))
          fieldName orElse paramName
        }

        case PropertyDescriptor(name, _, None, _, Some(s)) =>
          _methods.find(_.getMember == s).optMap(ai.findNameForDeserialization(_))

        case _ => None
      }
    }

  }

}

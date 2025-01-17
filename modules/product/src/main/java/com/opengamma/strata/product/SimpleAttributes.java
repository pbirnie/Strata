/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A simple implementation of attributes.
 */
@BeanDefinition(style = "light", constructorScope = "package")
final class SimpleAttributes
    implements Attributes, ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  static final Attributes EMPTY = new SimpleAttributes(ImmutableMap.of());

  /**
   * The attributes.
   * <p>
   * Attributes provide the ability to associate arbitrary information in a key-value map.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<AttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<AttributeType<?>> getAttributeTypes() {
    return attributes.keySet();
  }

  @Override
  public <T> Optional<T> findAttribute(AttributeType<T> type) {
    return Optional.ofNullable(type.fromStoredForm(attributes.get(type)));
  }

  @Override
  public <T> SimpleAttributes withAttribute(AttributeType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<AttributeType<?>, Object> updatedAttributes = new HashMap<>(attributes);
    if (value == null) {
      updatedAttributes.remove(type);
    } else {
      updatedAttributes.put(type, type.toStoredForm(value));
    }
    return new SimpleAttributes(updatedAttributes);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimpleAttributes}.
   */
  private static final TypedMetaBean<SimpleAttributes> META_BEAN =
      LightMetaBean.of(
          SimpleAttributes.class,
          MethodHandles.lookup(),
          new String[] {
              "attributes"},
          ImmutableMap.of());

  /**
   * The meta-bean for {@code SimpleAttributes}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SimpleAttributes> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param attributes  the value of the property, not null
   */
  SimpleAttributes(
      Map<AttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(attributes, "attributes");
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public TypedMetaBean<SimpleAttributes> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the attributes.
   * <p>
   * Attributes provide the ability to associate arbitrary information in a key-value map.
   * @return the value of the property, not null
   */
  public ImmutableMap<AttributeType<?>, Object> getAttributes() {
    return attributes;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleAttributes other = (SimpleAttributes) obj;
      return JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SimpleAttributes{");
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}

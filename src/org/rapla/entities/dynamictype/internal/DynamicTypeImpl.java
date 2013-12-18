/*--------------------------------------------------------------------------*
  | Copyright (C) 2006 Christopher Kohlhaas                                  |
  |                                                                          |
  | This program is free software; you can redistribute it and/or modify     |
  | it under the terms of the GNU General Public License as published by the |
  | Free Software Foundation. A copy of the license has been included with   |
  | these distribution in the COPYING file, if not go to www.fsf.org         |
  |                                                                          |
  | As a special exception, you are granted the permissions to link this     |
  | program with every library, which license fulfills the Open Source       |
  | Definition as published by the Open Source Initiative (OSI).             |
  *--------------------------------------------------------------------------*/
package org.rapla.entities.dynamictype.internal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.MultiLanguageName;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.ParsedText.EvalContext;
import org.rapla.entities.dynamictype.internal.ParsedText.Function;
import org.rapla.entities.dynamictype.internal.ParsedText.ParseContext;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.ReferenceHandler;
import org.rapla.entities.storage.internal.SimpleEntity;

public class DynamicTypeImpl extends SimpleEntity<DynamicType> implements DynamicType
{
    // added an attribute array for performance reasons
    transient private boolean attributeArrayUpToDate = false;
    transient Attribute[] attributes;

    MultiLanguageName name  = new MultiLanguageName();
    String elementKey = "";

    HashMap<String,ParsedText> annotations = new HashMap<String,ParsedText>();

    public DynamicTypeImpl() {
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        attributeArrayUpToDate = false;
    }

    public RaplaType<DynamicType> getRaplaType() {return TYPE;}

    public Classification newClassification() {
    	return newClassification( true );
    }
    
    public Classification newClassification(boolean useDefaults) {
    	if ( !isPersistant()) {
    		throw new IllegalStateException("You can only create Classifications from a persistant Version of DynamicType");
    	}
        final ClassificationImpl classification = new ClassificationImpl(this);
        // Array could not be up todate
        final Attribute[] attributes2 = getAttributes();
        if ( useDefaults)
        {
	        for ( Attribute att: attributes2)
	        {
	            final Object defaultValue = att.defaultValue();
	            if ( defaultValue != null)
	            {
	                classification.setValue(att, defaultValue);
	            }   
	        }
        }
        return classification;
    }

    public Classification newClassification(Classification original) {
        if ( !isPersistant()) {
            throw new IllegalStateException("You can only create Classifications from a persistant Version of DynamicType");
        }
        final ClassificationImpl newClassification = (ClassificationImpl) newClassification(true);
        {
            Attribute[] attributes = original.getAttributes();
            for (int i=0;i<attributes.length;i++) {
                Attribute originalAttribute = attributes[i];
                String attributeKey = originalAttribute.getKey();
                Attribute newAttribute = newClassification.getAttribute( attributeKey );
                Object defaultValue = originalAttribute.defaultValue();
                Object originalValue = original.getValue( attributeKey );
                if ( newAttribute != null  && newAttribute.getType().equals( originalAttribute.getType())) 
                {
                	Object newDefaultValue = newAttribute.defaultValue();
                	// If the default value of the new type differs from the old one and the value is the same as the old default then use the new default
                	if (  newDefaultValue != null && ((defaultValue == null && originalValue == null )|| (defaultValue != null && originalValue != null && !newDefaultValue.equals(defaultValue) && (originalValue.equals( defaultValue)))))
                	{
                		newClassification.setValue( newAttribute, newDefaultValue);
                	}
                	else
                	{
                		newClassification.setValue( newAttribute, newAttribute.convertValue( originalValue ));
                	}
                }
            }
            return newClassification;
        }
    }

    public ClassificationFilter newClassificationFilter() {
    	if ( !isPersistant()) {
    		throw new IllegalStateException("You can only create ClassificationFilters from a persistant Version of DynamicType");
    	}
        return new ClassificationFilterImpl(this);
    }

    public MultiLanguageName getName() {
        return name;
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        name.setReadOnly( enable );
    }

    public String getName(Locale locale) {
    	if ( locale == null)
    	{
    		return name.getName( null);
    	}
        String language = locale.getLanguage();
		return name.getName(language);
    }

    public String getAnnotation(String key) {
        ParsedText parsedAnnotation = annotations.get(key);
        if ( parsedAnnotation != null) 
        {
            DynamicTypeParseContext parseContext = new DynamicTypeParseContext(this);
			return parsedAnnotation.getExternalRepresentation(parseContext);
        } 
        else 
        {
            return null;
        }
    }
    
    public boolean isRefering(RefEntity<?> entity) {
        Attribute[] attributes = getAttributes();
        for ( int i=0;i<attributes.length;i++)
        {
            RefEntity<?> attribute = (RefEntity<?>)attributes[i];
            if ( attribute.isRefering( entity))
            {
                return true;
            }
        }
        return super.isRefering(entity);
    }

    public String getAnnotation(String key, String defaultValue) {
        String annotation = getAnnotation( key );
        return annotation != null ? annotation : defaultValue;
    }

    public void setAnnotation(String key,String annotation) throws IllegalAnnotationException {
        checkWritable();
        if (annotation == null) {
            annotations.remove(key);
            return;
        }
        DynamicTypeParseContext parseContext = new DynamicTypeParseContext(this);
		annotations.put(key,new ParsedText(annotation, parseContext));
    }

    public String[] getAnnotationKeys() {
        return annotations.keySet().toArray(RaplaObject.EMPTY_STRING_ARRAY);
    }

    public void setElementKey(String elementKey) {
        checkWritable();
        this.elementKey = elementKey;
    }

    public String getElementKey() {
        return elementKey;
    }

    /** exchange the two attribute positions */
    public void exchangeAttributes(int index1, int index2) {
        checkWritable();
        Attribute[] attribute = getAttributes();
        Attribute attribute1 = attribute[index1];
        Attribute attribute2 = attribute[index2];
        ReferenceHandler subEntityHandler = getSubEntityHandler();
		subEntityHandler.clearReferences();
        for (int i=0;i<attributes.length;i++) {
            if (i == index1)
                subEntityHandler.add((RefEntity<?>)attribute2);
            else if (i == index2)
                subEntityHandler.add((RefEntity<?>)attribute1);
            else
                subEntityHandler.add((RefEntity<?>)attributes[i]);
        }
        attributeArrayUpToDate = false;
    }

    /** find an attribute in the dynamic-type that equals the specified attribute. */
    public Attribute findAttribute(Attribute copy) {
        return (Attribute) super.findEntity((RefEntity<?>)copy);
    }

    public Attribute findAttributeForId(Object id) {
        Attribute[] typeAttributes = getAttributes();
        for (int i=0; i<typeAttributes.length; i++) {
            if (((RefEntity<?>)typeAttributes[i]).getId().equals(id)) {
                return typeAttributes[i];
            }
        }
        return null;
    }


    public void removeAttribute(Attribute attribute) {
        checkWritable();
        if ( findAttribute( attribute ) == null) {
            return;
        }
        attributeArrayUpToDate = false;
        super.removeEntity((RefEntity<?>) attribute);
        if (this.equals(attribute.getDynamicType()))
        {
        
        	if (((AttributeImpl) attribute).isReadOnly())
        	{
        		throw new IllegalArgumentException("Attribute is not writable. It does not belong to the same dynamictype instance");
        	}
            ((AttributeImpl) attribute).setParent(null);
        }
    }

    public void addAttribute(Attribute attribute) {
        checkWritable();
        attributeArrayUpToDate = false;
        super.addEntity((RefEntity<?>) attribute);
        if (attribute.getDynamicType() != null
            && !this.isIdentical(attribute.getDynamicType()))
            throw new IllegalStateException("Attribute '" + attribute
                                            + "' belongs to another dynamicType :"
                                            + attribute.getDynamicType());
        ((AttributeImpl) attribute).setParent(this);
    }

    private void updateAttributeArray() {
        if (attributeArrayUpToDate)
            return;
        Collection<Attribute> attributeList = new ArrayList<Attribute>();
        for (RefEntity<?> o:super.getSubEntities())
        {
            if (o.getRaplaType() == Attribute.TYPE) {
                attributeList.add((Attribute)o);
            }
        }
        attributes =  attributeList.toArray(Attribute.ATTRIBUTE_ARRAY);
        attributeArrayUpToDate = true;
    }

    public boolean hasAttribute(Attribute attribute) {
        return getReferenceHandler().isRefering((RefEntity<?>)attribute);
    }

    public Attribute[] getAttributes() {
        updateAttributeArray();
        return attributes;
    }

    public Attribute getAttribute(String key) {
        Attribute[] attributes = getAttributes();
        for (int i=0;i<attributes.length;i++) {
            String att = attributes[i].getKey();
            if (att.equals(key))
                return attributes[i];
        }
        return null;
    }

    ParsedText getParsedAnnotation(String key) {
        return  annotations.get( key );
    }

    static private void copy(DynamicTypeImpl source,DynamicTypeImpl dest) {
       
        dest.name = (MultiLanguageName) source.name.clone();
        dest.elementKey = source.elementKey;
        for (RefEntity<?> att:dest.getSubEntities())
        {
            ((AttributeImpl)att).setParent(dest);
        }
        dest.attributeArrayUpToDate = false;
        dest.annotations = new LinkedHashMap<String, ParsedText>();
        for (Map.Entry<String,ParsedText> entry: source.annotations.entrySet())
        {
            String annotation = entry.getKey();
            ParsedText parsedAnnotation =entry.getValue();
            DynamicTypeParseContext parseContext = new DynamicTypeParseContext(dest);
            String parsedValue = parsedAnnotation.getExternalRepresentation(parseContext);
            try {
                dest.setAnnotation(annotation, parsedValue);
            } catch (IllegalAnnotationException e) {
                throw new IllegalStateException("Can't parse annotation back", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
	public void copy(DynamicType obj) {
        super.copy((SimpleEntity<DynamicType>)obj);
        copy((DynamicTypeImpl) obj,this);
    }

    public DynamicType deepClone() {
        DynamicTypeImpl clone = new DynamicTypeImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" [");
        buf.append ( super.toString()) ;
        buf.append("] key=");
        buf.append( getElementKey() );
        buf.append(": ");
        if ( attributes != null ) {
            Attribute[] att = getAttributes();
            for ( int i=0;i<att.length; i++){
                if ( i> 0)
                    buf.append(", ");
                buf.append( att[i].getKey());
            }
        }
        return buf.toString();
    }

	/**
	 * @param newType
	 * @param attributeId
	 */
	public boolean hasAttributeChanged(DynamicTypeImpl newType, Object attributeId) {
    	Attribute oldAttribute = findAttributeForId(attributeId );
    	Attribute newAttribute = newType.findAttributeForId(attributeId );
    	if ((newAttribute == null ) ||  ( oldAttribute == null)) {
    		return true;
    	}
		String newKey = newAttribute.getKey();
        if ( !newKey.equals( oldAttribute.getKey() )) {
			return true;
		}
		if ( !newAttribute.getType().equals( oldAttribute.getType())) {
			return true;
		}
		{
			String[] keys = newAttribute.getConstraintKeys();
			String[] oldKeys = oldAttribute.getConstraintKeys();
			if ( keys.length != oldKeys.length) {
				return true;
			}
			for ( int i=0;i< keys.length;i++) {
				if ( !keys[i].equals( oldKeys[i]) )
					return true;
				Object oldConstr = oldAttribute.getConstraint( keys[i]);
				Object newConstr = newAttribute.getConstraint( keys[i]);
				if ( oldConstr == null && newConstr == null)
					continue;
				if ( oldConstr == null || newConstr == null)
					return true;

				if ( !oldConstr.equals( newConstr))
					return true;
			}
		}
		return false;
	}

	
	static class DynamicTypeParseContext implements ParseContext {
		private DynamicTypeImpl type;

		DynamicTypeParseContext( DynamicType type)
		{
			this.type = (DynamicTypeImpl)type;
		}
		
		public Function resolveVariableFunction(String variableName) throws IllegalAnnotationException {
			Attribute attribute = type.getAttribute(variableName);
	        if (attribute != null) 
	        {
	        	return new AttributeFunction(attribute);
	        } 
	        else if (variableName.equals(type.getElementKey())) 
	        {
	        	return new TypeFunction(type);
	        }
	        return null;
		}
		
		class AttributeFunction extends ParsedText.Function
		{
			Object id;
			AttributeFunction(Attribute attribute )
			{
				super("attribute:"+attribute.getKey());
				id =((RefEntity<?>)attribute).getId() ;
				
			}
			
			protected String getName() {
			    Attribute attribute = findAttribute( type);
			    if  ( attribute != null)
			    {
			        return attribute.getKey();
			    }
                return name;
			}

			public Attribute eval(EvalContext context) {
				Classification classification = context.getClassification();
				DynamicTypeImpl type = (DynamicTypeImpl) classification.getType();
				return findAttribute(type);
			}

            public Attribute findAttribute(DynamicTypeImpl type) {
                Attribute attribute =  type.findAttributeForId( id );
				if ( attribute!= null) {
					return attribute;
	            }
				return null;
            }
			
			@Override
			public String getRepresentation( ParseContext context)
			{
				
		        Attribute attribute = type.findAttributeForId( id );
		        if ( attribute!= null) {
		        	return attribute.getKey();
		        }
		        return "";
			}
		}
		
		class TypeFunction extends ParsedText.Function
		{
			Object id;
			TypeFunction(DynamicType type) 
			{
				super("type:"+type.getElementKey());
				id = ((RefEntity<?>) type).getId() ;
			}
			
			public String eval(EvalContext context) 
			{
				DynamicTypeImpl type = (DynamicTypeImpl) context.getClassification().getType();
				return type.getName( context.getLocale());
			}
			
			@Override
			public String getRepresentation( ParseContext context)
			{
				if ( type.getId().equals( id ) ) {
					return type.getElementKey();
		        }
				return "";
			}
		}
	}
	
}



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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.rapla.components.util.ParseDateException;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.MultiLanguageName;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.ReferenceHandler;
import org.rapla.entities.storage.internal.SimpleEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaException;
import org.rapla.storage.LocalCache;

public class AttributeImpl extends SimpleEntity<Attribute> implements Attribute
{
    private MultiLanguageName name = new MultiLanguageName();
    private AttributeType type;
    private String key;
    private boolean bOptional = false;
    private HashMap<String,String> annotations = new LinkedHashMap<String,String>();
    private Object defaultValue =null;

    public final static AttributeType DEFAULT_TYPE = AttributeType.STRING;

    public AttributeImpl() {
        this.type = DEFAULT_TYPE;
    }

    public AttributeImpl(AttributeType type) {
        setType(type);
    }

    void setParent(DynamicType parent) {
        getReferenceHandler().put("parent", (RefEntity<?>)parent);
    }
    
    public DynamicType getDynamicType() {
        return (DynamicType)getReferenceHandler().get("parent");
    }

    final public RaplaType<Attribute> getRaplaType() {return TYPE;}

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) 
    {
        Object oldValue = defaultValue;
        if ( type.equals( AttributeType.CATEGORY))
        {
            oldValue = getReferenceHandler().get("default.category");
        }
        this.type = type;
        defaultValue = convertValue( oldValue);
    }

    public MultiLanguageName getName() {
        return name;
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        name.setReadOnly( enable );
    }

    public String getName(Locale locale) {
        return name.getName(locale.getLanguage());
    }

    public String getKey() {
        return key;
    }

    public void setConstraint(String key,Object constraint) {
        checkWritable();
        setContraintWithoutWritableCheck(key, constraint);
    }

	public void setContraintWithoutWritableCheck(String key,
			Object constraint) {
		if ( getConstraintClass( key ) == Category.class || getConstraintClass( key ) == DynamicType.class) {
        	ReferenceHandler referenceHandler = getReferenceHandler();
        	String refID = "constraint." + key;
        	if ( constraint == null)
        	{
        		referenceHandler.removeWithKey( refID);
        	} 
        	else if ( constraint instanceof RefEntity<?>)
        	{
        		referenceHandler.put(refID,(RefEntity<?>)constraint);
        	}
        	else if ( constraint instanceof SimpleIdentifier)
        	{
        		referenceHandler.putId(refID,(SimpleIdentifier)constraint);
        	}
        }
	}
    
    public void setDefaultValue(Object object)
    {
        defaultValue = object;
        if ( type.equals( AttributeType.CATEGORY))
        {
            getReferenceHandler().put("default.category",(RefEntity<?>)object);
        }
    }

    public Object getConstraint(String key) {
        if ( getConstraintClass( key ) == Category.class || getConstraintClass( key ) == DynamicType.class) {
            return getReferenceHandler().get("constraint." + key);
        }
        return null;
    }

    public Class<?> getConstraintClass(String key) {
        if (key.equals(ConstraintIds.KEY_ROOT_CATEGORY)) {
            return Category.class;
        }
        if (key.equals(ConstraintIds.KEY_DYNAMIC_TYPE)) {
            return DynamicType.class;
        }
        return String.class;
    }

    public String[] getConstraintKeys() {
        if (type.equals( AttributeType.CATEGORY)) {
            return new String[] {ConstraintIds.KEY_ROOT_CATEGORY};
        } 
        if (type.equals( AttributeType.ALLOCATABLE)) {
            return new String[] {ConstraintIds.KEY_DYNAMIC_TYPE};
        } else {
            return new String[0];
        }
    }

    public void setKey(String key) {
        checkWritable();
        this.key = key;
    }

    public  boolean isValid(Object obj) {
        return true;
    }

    public boolean isOptional() {
        return bOptional;
    }

    public void setOptional(boolean bOptional) {
        checkWritable();
        this.bOptional = bOptional;
    }

    public Object defaultValue() 
    {
        return defaultValue;
    }

    public boolean needsChange(Object value) {
        if (value == null)
            return false;

        if (type.equals( AttributeType.STRING )) {
            return !(value instanceof String);
        }
        if (type.equals( AttributeType.INT )) {
            return !(value instanceof Long);
        }
        if (type.equals( AttributeType.DATE )) {
            return !(value instanceof Date);
        }
        if (type.equals( AttributeType.BOOLEAN )) {
            return !(value instanceof Boolean);
        }
        if (type.equals( AttributeType.ALLOCATABLE )) {
            return !(value instanceof Allocatable);
        }
        if (type.equals( AttributeType.CATEGORY )) {
        	if (!(value instanceof Category))
                return true;

            Category temp = (Category) value;

            // look if the attribute category is a ancestor of the value category
            Category rootCategory = (Category) getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
            if ( rootCategory != null)
            {
            	boolean change =  !rootCategory.isAncestorOf( temp );
            	return change;
            }
            return false;
            
        }
        return false;
    }

    public Object convertValue(Object value) {
        if (type.equals( AttributeType.STRING )) {
            if (value == null)
                return null;
            if (value instanceof Date)
            {
                return new SerializableDateTimeFormat().formatDate( (Date) value);
            }
//            if (value instanceof Category)
//            {
//            	return ((Category) value).get
//            }
            return value.toString();
        }
        if (type.equals( AttributeType.DATE )) {
            if (value == null)
                return null;
            else if (value instanceof Date)
            	return value;
            
            try {
				return new SerializableDateTimeFormat().parseDate( value.toString(), false);
			} catch (ParseDateException e) {
				return null;
			}
        }
        if (type.equals( AttributeType.INT )) {
            if (value == null)
                return null;

            if (value instanceof Boolean)
                return ((Boolean) value).booleanValue() ? new Long(1) : new Long(0);
            String str = value.toString().trim().toLowerCase();
            try {
                return new Long(str);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (type.equals( AttributeType.BOOLEAN )) {
            if (value == null)
                return Boolean.FALSE;
            String str = value.toString().trim().toLowerCase();
            if (str.equals(""))
            {
                return Boolean.FALSE;
            }
            if (str.equals("0") || str.equals("false"))
                return Boolean.FALSE;
            else
                return Boolean.TRUE;
        }
        if (type.equals( AttributeType.ALLOCATABLE)) {
        	// We currently can't convert to allocatable type so we set it to null
        	return null;
        }
        if (type.equals( AttributeType.CATEGORY )) {
            if (value == null)
                return null;
            Category rootCategory = (Category) getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
            if (value instanceof Category) {
                Category temp = (Category) value;
                if  ( rootCategory != null  ) 
                { 
            	   if (rootCategory.isAncestorOf( temp ))
            	   {
            		   return value;
            	   }

            	   // if the category can't be found under the root then we check if we find a category path with the same keys 
            	   List<String> keyPathRootCategory = ((CategoryImpl)rootCategory).getKeyPath( null);
            	   List<String> keyPath = ((CategoryImpl)temp).getKeyPath( null);
            	   List<String> nonCommonPath = new ArrayList<String>();
            	   boolean differInKeys = false;
            	   // 
            	   for ( int i=0;i<keyPath.size();i++)
            	   {
            		   String key = keyPath.get(i);
            		   String rootCatKey = keyPathRootCategory.size() > i ? keyPathRootCategory.get( i ) : null;
            		   if ( rootCatKey == null || !key.equals(rootCatKey))
            		   {
            			   differInKeys = true;
            		   }
            		   if ( differInKeys)
            		   {
            			   nonCommonPath.add( key);
            		   }
            	   }
            	   
            	   Category parentCategory = rootCategory;
            	   Category newCategory = null;
            	   //we first check for the whole keypath  this covers root changes from b to c, when c contains the b substructure including b
            	   //     a  
            	   //    / \
            	   //  |b| |c|
            	   //  /   /
            	   // d   b
            	   //    /
            	   //   d
            	   for ( String key: nonCommonPath)
            	   {
            		   newCategory = parentCategory.getCategory( key);
            		   if ( newCategory == null)
            		   {
            			   break;
            		   }
            		   else
            		   {
            			   parentCategory = newCategory;
            		   }
            	   }
            	   //if we don't find a category we also check if a keypath that contains on less entry
            	   // covers root changes from b to c when c contains directly the b substructure but not b itself
            	   //     a  
            	   //    / \
            	   //  |b| |c|
            	   //  /   /
            	   // d   d
            	   //    
            	   if ( newCategory == null && nonCommonPath.size() > 1)
            	   {
            		   List<String> subList = nonCommonPath.subList(1, nonCommonPath.size());
            		   for ( String key: subList)
                	   {
                		   newCategory = parentCategory.getCategory( key);
                		   if ( newCategory == null)
                		   {
                			   break;
                		   }
                		   else
                		   {
                			   parentCategory = newCategory;
                		   }
                	   }
            	   }
            	   return newCategory;
               }
            }
            if ( rootCategory != null)
            {
	            Category category = rootCategory.getCategory(value.toString());
	            if ( category == null)
	            {
	            	return null;
	            }
	            return category;
            }
        }
        return null;
    }

    public String getAnnotation(String key) {
        return annotations.get(key);
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
        annotations.put(key,annotation);
    }

    public String[] getAnnotationKeys() {
        return annotations.keySet().toArray(RaplaObject.EMPTY_STRING_ARRAY);
    }


    static private void copy(AttributeImpl source,AttributeImpl dest) {
        dest.name = (MultiLanguageName) source.name.clone();
        @SuppressWarnings("unchecked")
		HashMap<String,String> annotationClone = (HashMap<String,String>) source.annotations.clone();
		dest.annotations = annotationClone;
        dest.type = source.getType();
        dest.setKey(source.getKey());
        dest.setOptional(source.isOptional());
        String[] constraintKeys = source.getConstraintKeys();
        for ( int i = 0;i < constraintKeys.length; i++) {
            String key = constraintKeys[ i ];
            dest.setConstraint( key, source.getConstraint(key));
        }
        dest.setDefaultValue( source.defaultValue());
    }

    @SuppressWarnings("unchecked")
	public void copy(Attribute obj) {
        super.copy((SimpleEntity<Attribute>)obj);
        copy((AttributeImpl) obj,this);
    }

    public Attribute deepClone() {
        AttributeImpl clone = new AttributeImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public String toString() {
        MultiLanguageName name = getName();
        if (name != null) {
            return name.toString()+ " ID='" + getId() + "'";
        }  else {
            return getKey()  + " " + getId();
        }
    }

    static public Object parseAttributeValue(Attribute attribute,String text) throws RaplaException {
        AttributeType type = attribute.getType();
        final String trim = text.trim();
        if (type.equals( AttributeType.STRING )) {
            return text;
        }
        else if (type.equals( AttributeType.ALLOCATABLE)) {
            String path = trim;
            if (path.length() == 0) {
                return null;
            }
        	if (LocalCache.isTextId(Allocatable.TYPE,path)) {
        		Comparable id = LocalCache.getId( Allocatable.TYPE, path);
        		return id ;
        	}
            return null;
        }
        else if (type.equals( AttributeType.CATEGORY )) {
            String path = trim;
            if (path.length() == 0) {
                return null;
            }
            if (LocalCache.isTextId(Category.TYPE,path) ) {
            	Comparable id = LocalCache.getId( Category.TYPE, path);
            	return id ;
            } else {
                CategoryImpl rootCategory = (CategoryImpl)attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
                 if (rootCategory == null) {
                   //System.out.println( attribute.getConstraintKeys());
                   throw new RaplaException("Can't find " + ConstraintIds.KEY_ROOT_CATEGORY + " for attribute " + attribute);
                }
                Category categoryFromPath = rootCategory.getCategoryFromPath(path);
				if ( categoryFromPath == null)
				{
					// TODO call convert that tries to convert from a string path
				}
                return categoryFromPath;
            }
        } else if (trim.length() == 0)
        {
            return null;
        }
        else if (type.equals(AttributeType.BOOLEAN)) {
            return trim.equalsIgnoreCase("true") || trim.equals("1")  ?
                Boolean.TRUE : Boolean.FALSE;
        } else if (type.equals( AttributeType.DATE )) {
            try {
				return new SerializableDateTimeFormat().parseDate( trim, false);
			} catch (ParseDateException e) {
				throw new RaplaException( e.getMessage(), e);
			}
        } else  if (type.equals( AttributeType.INT)) {
            try {
                return new Long( trim );
            } catch (NumberFormatException ex) {
                throw new RaplaException( ex.getMessage());
            }
        }
        
        throw new RaplaException("Unknown attribute type: " + type );
    }

    public static String attributeValueToString( Attribute attribute, Object value, boolean idOnly) throws EntityNotFoundException {
	    AttributeType type = attribute.getType();
	    if (type.equals( AttributeType.ALLOCATABLE ))
	    {
	    	return ((RefEntity<?>)value).getId().toString();
	    }
	    if (type.equals( AttributeType.CATEGORY ))
	    {
	        CategoryImpl rootCategory = (CategoryImpl) attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
	        if ( idOnly) {
	            return ((RefEntity<?>)value).getId().toString();
	        } else {
	           return  rootCategory.getPathForCategory((Category)value) ;
	        }
	    }
	    else if (type.equals( AttributeType.DATE ))
	    {
	        return new SerializableDateTimeFormat().formatDate( (Date)value ) ;
	    }
	    else
	    {
	        return value.toString() ;
	    }
    }

    static public class IntStrategy {
        String[] constraintKeys = new String[] {"min","max"};

        public String[] getConstraintKeys() {
            return constraintKeys;
        }

        public boolean needsChange(Object value) {
            return !(value instanceof Long);
        }

        public Object convertValue(Object value) {
            if (value == null)
                return null;

            if (value instanceof Boolean)
                return ((Boolean) value).booleanValue() ? new Long(1) : new Long(0);
            String str = value.toString().trim().toLowerCase();
            try {
                return new Long(str);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

}





/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.entry;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassAttribute extends AbstractServerAttribute
{
    /** Used for serialization */
    public static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ObjectClassAttribute.class );
    
    /** A speedup to get the ObjectClass attribute */
    private static transient AttributeType OBJECT_CLASS_AT;
    
    /** A mutex to manage synchronization*/
    private transient static Object MUTEX = new Object();


    // Sets dealing with objectClass operations
    private Set<ObjectClass> allObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> abstractObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> auxiliaryObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> structuralObjectClasses = new HashSet<ObjectClass>();

    private Set<AttributeType> mayList = new HashSet<AttributeType>();
    private Set<AttributeType> mustList = new HashSet<AttributeType>();
    
    /** The global registries */
    private transient Registries registries;


    /**
     * This method is used to initialize the OBJECT_CLASS_AT attributeType.
     * 
     * We want to do it only once, so it's a synchronized method. Note that
     * the alternative would be to call the lookup() every time, but this won't
     * be very efficient, as it will get the AT from a map, which is also
     * synchronized, so here, we have a very minimal cost.
     * 
     * We can't do it once as a static part in the body of this class, because
     * the access to the registries is mandatory to get back the AttributeType.
     */
    private void initObjectClassAT( Registries registries )
    {
        if ( OBJECT_CLASS_AT == null )
        {
            try
            {
                synchronized ( MUTEX )
                {
                    OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
                }
            }
            catch ( NamingException ne )
            {
                // do nothing...
            }
        }
    }


    /**
     * Creates a new ObjectClassAttribute with a null ID.
     * <p>
     * We will use the default name : 'objectClass'
     * 
     * @param registries The server registries to use
     */
    public ObjectClassAttribute( Registries registries )
    {
        this( registries, SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * Creates a new ObjectClassAttribute with ServerAttribute.
     * <p>
     * The ServerAttribute must have the ObjectClass attributeType
     * 
     * @param registries The server registries to use
     * @param serverAttribute The serverAttribute containing the objectClasses
     */
    public ObjectClassAttribute( Registries registries, ServerAttribute serverAttribute )
    {
        this( registries, SchemaConstants.OBJECT_CLASS_AT );

        if ( serverAttribute == null )
        {
            LOG.error( "We cannot create an ObjectClassAttribute without any serverAttribute" );
        }
        else
        {
            if ( !serverAttribute.getType().getOid().equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
            {
                LOG.error(  "The ServerAttribute does not represent an ObjectClass" );
            }
            else
            {
                // Iterate through the attribute values and store them in the ObjectClass,
                // if they are valid.
                for ( Iterator<ServerValue<?>> values = serverAttribute.getAll(); values.hasNext(); )
                {
                    ServerValue<?> value = values.next();
                    
                    if ( value instanceof ServerStringValue )
                    {
                        String objectClassName = ((ServerStringValue)value).get();
                        
                    
                        try
                        {
                            // Fond the objectClass and update the internal structures
                            ObjectClass objectClass =  registries.getObjectClassRegistry().lookup( objectClassName );

                            addObjectClass( objectClass );
                        }
                        catch ( NamingException ne )
                        {
                            // We didn't found the objectClass. Just ditch it
                            LOG.error(  "The '{}' objectclass does not exist or the associated schema is not loaded", objectClassName );
                        }
                    }
                }
            }
        }
    }


    /**
     * Creates a new instance of ObjectClassAttribute.
     *
     * @param upId The ObjectClass ID
     * @param registries The atRegistry to use to initialize this object
     * @throws NamingException If something went wrong
     */
    public ObjectClassAttribute( Registries registries, String upId )
    {
        this.registries = registries;
        
        initObjectClassAT( registries );
        
        attributeType = OBJECT_CLASS_AT;
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the values does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     */
    public ObjectClassAttribute( Registries registries, ServerValue<?>... values ) throws NamingException
    {
        this( registries, null, values );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     */
    public ObjectClassAttribute( Registries registries, String upId, ServerValue<?>... vals ) throws NamingException
    {
        this.registries = registries;
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );

        if ( vals == null )
        {
            values.add( new ServerStringValue( attributeType ) );
        }
        else
        {
            for ( ServerValue<?> val:vals )
            {
                if ( ! ( val instanceof ServerStringValue ) )
                {
                    String message = "Only String values supported for objectClass attribute";
                    LOG.error( message );
                    throw new UnsupportedOperationException( message );
                }
                else
                {
                    values.add( val );
                }
            }
        }

        setUpId( upId, attributeType );
    }


    public ObjectClassAttribute( Registries registries, String upId, String... vals ) throws NamingException
    {
        this.registries = registries;
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );
        
        if ( vals == null )
        {
            values.add( new ServerStringValue( attributeType ) );
        }
        else
        {
            for ( String val:vals )
            {
                values.add( new ServerStringValue( attributeType, val ) );
            }
        }

        setUpId( upId, attributeType );
    }

    
    // -----------------------------------------------------------------------


    private Set<ObjectClass> addAncestors( ObjectClass descendant, Set<ObjectClass> ancestors ) throws NamingException
    {
        if ( descendant == null )
        {
            return ancestors;
        }

        ObjectClass[] superClasses = descendant.getSuperClasses();

        if ( ( superClasses == null ) || ( superClasses.length == 0 ) )
        {
            return ancestors;
        }

        for ( ObjectClass ancestor : superClasses )
        {
            ancestors.add( ancestor );
            addAncestors( ancestor, ancestors );
        }

        return ancestors;
    }


    public boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException
    {
        if ( allObjectClasses.contains( objectClass ) )
        {
            return false;
        }

        // add the value to the set of values
        values.add( new ServerStringValue( attributeType, alias) );

        Set<ObjectClass> ancestors = addAncestors( objectClass, new HashSet<ObjectClass>() );
        ancestors.add( objectClass );
        
        // now create sets of the different kinds of objectClasses
        for ( ObjectClass oc : ancestors )
        {
            switch ( oc.getType() )
            {
                case STRUCTURAL :
                    structuralObjectClasses.add( oc );
                    break;
                    
                case AUXILIARY :
                    auxiliaryObjectClasses.add( oc );
                    break;
                    
                case ABSTRACT :
                    abstractObjectClasses.add( oc );
                    break;
                    
                default:
                    String message = "Unrecognized objectClass type value: " + oc.getType();
                    LOG.error( message );
                    throw new UnsupportedOperationException( message );
            }

            allObjectClasses.add( oc );
            
            // now go through all objectClassses to collect the must an may list attributes
            Collections.addAll( mayList, oc.getMayList() );
            Collections.addAll( mustList, oc.getMustList() );
        }

        return true;
    }


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        String name = objectClass.getName();

        if ( name == null )
        {
            name = objectClass.getOid();
        }

        return addObjectClass( objectClass, name );
    }


    public boolean hasObjectClass( ObjectClass objectClass )
    {
        return allObjectClasses.contains( objectClass );
    }


    public boolean hasObjectClass( String objectClass )
    {
        return allObjectClasses.contains( objectClass );
    }


    public Set<ObjectClass> getAbstractObjectClasses()
    {
        return Collections.unmodifiableSet( abstractObjectClasses );
    }


    public ObjectClass getStructuralObjectClass()
    {
        if ( structuralObjectClasses.isEmpty() )
        {
            return null;
        }
        return structuralObjectClasses.iterator().next();
    }


    public Set<ObjectClass> getStructuralObjectClasses()
    {
        return Collections.unmodifiableSet( structuralObjectClasses );
    }


    public Set<ObjectClass> getAuxiliaryObjectClasses()
    {
        return Collections.unmodifiableSet( auxiliaryObjectClasses );
    }


    public Set<ObjectClass> getAllObjectClasses()
    {
        return Collections.unmodifiableSet( allObjectClasses );
    }


    public Set<AttributeType> getMustList()
    {
        return Collections.unmodifiableSet( mustList );
    }


    public Set<AttributeType> getMayList()
    {
        return Collections.unmodifiableSet( mayList );
    }

    
    /**
     * Get the String value, if and only if the value is known to be a String,
     * otherwise a InvalidAttributeValueException will be thrown
     *
     * @return The value as a String
     * @throws InvalidAttributeValueException If the value is a byte[]
     */
    public String getString() throws InvalidAttributeValueException
    {
        ServerValue<?> value = get();
        
        if ( value instanceof ServerStringValue )
        {
            return (String)value.get();
        }
        else
        {
            String message = "The value is expected to be a String";
            LOG.error( message );
            throw new InvalidAttributeValueException( message );
        }
    }


    /**
     * Get the byte[] value, if and only if the value is known to be Binary,
     * otherwise a InvalidAttributeValueException will be thrown
     *
     * @return The value as a String
     * @throws InvalidAttributeValueException If the value is a String
     */
    public byte[] getBytes() throws InvalidAttributeValueException
    {
        String message = "The value for an objectClass is expected to be a String";
        LOG.error( message );
        throw new InvalidAttributeValueException( message );
    }


    public boolean add( byte[] val )
    {
        String message = "Binary values are not accepted by ObjectClassAttributes";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }


    public boolean add( String val ) throws InvalidAttributeIdentifierException, NamingException
    {
        ObjectClass objectClass = registries.getObjectClassRegistry().lookup( val );
        
        return addObjectClass( objectClass );
    }


    public boolean add( ServerStringValue val ) throws InvalidAttributeIdentifierException, NamingException
    {
        //boolean added = super.add( val );
        
        ObjectClass objectClass = registries.getObjectClassRegistry().lookup( val.get() );
        
        if ( objectClass == null )
        {
            return false;
        }
        else
        {
            return addObjectClass( objectClass, val.get() );
        }
        
        //return added;
    }


    public boolean contains( byte[] val )
    {
        String message = "There are no binary values in an ObjectClass attribute.";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }


    public boolean remove( byte[] val )
    {
        String message = "There are no binary values in an ObjectClass attribute.";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }
    
    
    /**
     * @see Object#toString() 
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "    ObjectClass : " );
        
        if ( ( values != null ) && ( values.size() != 0 ) )
        {
            boolean isFirst = true;
            
            for ( ServerValue<?> objectClass:values )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }
                
                sb.append( objectClass.get() );
            }
        }
        else
        {
            sb.append( "(null)" );
        }
        
        sb.append( '\n' );
        
        return sb.toString();
    }
}

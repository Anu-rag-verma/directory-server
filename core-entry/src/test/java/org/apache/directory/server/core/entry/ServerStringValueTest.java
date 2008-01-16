/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.entry;


import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.AbstractSyntax;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


/**
 * Tests that the ServerStringValue class works properly as expected.
 *
 * Some notes while conducting tests:
 *
 * <ul>
 *   <li>comparing values with different types - how does this behave</li>
 *   <li>exposing access to at from value or to a comparator?</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerStringValueTest
{
    static private S s;
    static private AT at;
    static private MR mr;
    
    /**
     * A local Syntax class for tests
     */
    static class AT extends AbstractAttributeType
    {
        public static final long serialVersionUID = 0L;
        AttributeType superior;
        Syntax syntax;
        MatchingRule equality;
        MatchingRule ordering;
        MatchingRule substr;

        protected AT( String oid )
        {
            super( oid );
        }

        public AttributeType getSuperior() throws NamingException
        {
            return superior;
        }


        public Syntax getSyntax() throws NamingException
        {
            return syntax;
        }


        public MatchingRule getEquality() throws NamingException
        {
            return equality;
        }


        public MatchingRule getOrdering() throws NamingException
        {
            return ordering;
        }


        public MatchingRule getSubstr() throws NamingException
        {
            return substr;
        }


        public void setSuperior( AttributeType superior )
        {
            this.superior = superior;
        }


        public void setSyntax( Syntax syntax )
        {
            this.syntax = syntax;
        }


        public void setEquality( MatchingRule equality )
        {
            this.equality = equality;
        }


        public void setOrdering( MatchingRule ordering )
        {
            this.ordering = ordering;
        }


        public void setSubstr( MatchingRule substr )
        {
            this.substr = substr;
        }
    }

    /**
     * A local MatchingRule class for tests
     */
    static class MR extends AbstractMatchingRule
    {
        public static final long serialVersionUID = 0L;
        private Syntax syntax;
        private Comparator comparator;
        private Normalizer normalizer;

        protected MR( String oid )
        {
            super( oid );
        }

        public Syntax getSyntax() throws NamingException
        {
            return syntax;
        }

        public Comparator getComparator() throws NamingException
        {
            return comparator;
        }


        public Normalizer getNormalizer() throws NamingException
        {
            return normalizer;
        }


        public void setSyntax( Syntax syntax )
        {
            this.syntax = syntax;
        }


        public void setComparator( Comparator<?> comparator )
        {
            this.comparator = comparator;
        }


        public void setNormalizer( Normalizer normalizer )
        {
            this.normalizer = normalizer;
        }
    }


    /**
     * A local Syntax class used for the tests
     */
    static class S extends AbstractSyntax
    {
        public static final long serialVersionUID = 0L;
        SyntaxChecker checker;

        public S( String oid, boolean humanReadible )
        {
            super( oid, humanReadible );
        }

        public void setSyntaxChecker( SyntaxChecker checker )
        {
            this.checker = checker;
        }

        public SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return checker;
        }
    }

    private AttributeType getCaseIgnoringAttributeNoNumbersType()
    {
        S s = new S( "1.1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker()
        {
            public String getSyntaxOid()
            {
                return "1.1.1.1";
            }
            public boolean isValidSyntax( Object value )
            {
                if ( !( value instanceof String ) )
                {
                    return false;
                }

                String strval = ( String ) value;
                
                for ( char c:strval.toCharArray() )
                {
                    if ( Character.isDigit( c ) )
                    {
                        return false;
                    }
                }
                return true;
            }

            public void assertSyntax( Object value ) throws NamingException
            {
                if ( ! isValidSyntax( value ) )
                {
                    throw new InvalidAttributeValueException();
                }
            }
        } );

        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                return ( o1 == null ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : o1.compareTo( o2 ) ) );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) 
                {
                    return 0;
                }
                else if ( val.equals( "MEDIUM" ) ) 
                {
                    return 1;
                }
                else if ( val.equals( "HIGH" ) ) 
                {
                    return 2;
                }
                
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;

            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof String )
                {
                    return ( ( String ) value ).toLowerCase();
                }

                throw new IllegalStateException( "expected string to normalize" );
            }
        };
        
        AT at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }


    private AttributeType getIA5StringAttributeType()
    {
        AT at = new AT( "1.1" );

        S s = new S( "1.1.1", true );

        s.setSyntaxChecker( new SyntaxChecker()
        {
            public String getSyntaxOid()
            {
                return "1.1.1";
            }
            public boolean isValidSyntax( Object value )
            {
                return ((String)value).length() < 5 ;
            }

            public void assertSyntax( Object value ) throws NamingException
            {
                if ( ! isValidSyntax( value ) )
                {
                    throw new InvalidAttributeValueException();
                }
            }
        } );

        final MR mr = new MR( "1.1.2" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                return ( ( o1 == null ) ? 
                    ( o2 == null ? 0 : -1 ) :
                    ( o2 == null ? 1 : o1.compareTo( o2 ) ) );
            }
        };
        
        mr.normalizer = new Normalizer()
        {
            public static final long serialVersionUID = 1L;
            
            public Object normalize( Object value ) throws NamingException
            {
                if ( value instanceof String )
                {
                    return ( ( String ) value ).toLowerCase();
                }

                throw new IllegalStateException( "expected string to normalize" );
            }
        };
        
        at.setEquality( mr );
        at.setSyntax( s );
        return at;
    }

    
    /**
     * Initialize an AttributeType and the associated MatchingRule 
     * and Syntax
     */
    @Before public void initAT()
    {
        s = new S( "1.1.1.1", false );
        s.setSyntaxChecker( new AcceptAllSyntaxChecker( "1.1.1.1" ) );
        mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new ByteArrayComparator();
        mr.normalizer = new DeepTrimToLowerNormalizer();
        at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setOrdering( mr );
        at.setSubstr( mr );
        at.setSyntax( s );
    }
    

    /**
     * Test the constructor with bad AttributeType
     */
    @Test public void testBadConstructor()
    {
        try
        {
            new ServerStringValue( null );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
        
        // create a AT without any syntax
        AttributeType at = new AT( "1.1.3.1" );
        
        try
        {
            new ServerStringValue( at );
            fail();
        }
        catch ( AssertionError ae )
        {
            // Expected...
        }
    }


    /**
     * Test the constructor with a null value
     */
    @Test public void testNullValue()
    {
        AttributeType at = getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, null );
        
        assertNull( value.get() );
        assertTrue( value.isNull() );
    }
    
    
    /**
     * Test the equals method
     */
    @Test public void testEquals()
    {
        AttributeType at = getIA5StringAttributeType();
        
        ServerStringValue value1 = new ServerStringValue( at, "test" );
        ServerStringValue value2 = new ServerStringValue( at, "test" );
        ServerStringValue value3 = new ServerStringValue( at, "TEST" );
        ServerStringValue value4 = new ServerStringValue( at, "tes" );
        ServerStringValue value5 = new ServerStringValue( at, null );
        
        assertTrue( value1.equals( value1 ) );
        assertTrue( value1.equals( value2 ) );
        assertTrue( value1.equals( value3 ) );
        assertFalse( value1.equals( value4 ) );
        assertFalse( value1.equals( value5 ) );
        assertFalse( value1.equals( "test" ) );
        assertFalse( value1.equals( null ) );
    }

    
    /**
     * Test the getNormalized method
     * TODO testNormalized.
     *
     */
    @Test public void testGetNormalized() throws NamingException
    {
        AttributeType at = getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, "TEST" );
        
        assertEquals( "test", value.getNormalized() );

        value = new ServerStringValue( at, null );
        
        assertNull( value.getNormalized() );
    }
    
    
    /**
     * Test the isValid method
     * 
     * The SyntaxChecker does not accept values longer than 5 chars.
     */
    @Test public void testIsValid() throws NamingException
    {
        AttributeType at = getIA5StringAttributeType();
        
        ServerStringValue value = new ServerStringValue( at, "test" );
        
        assertTrue( value.isValid() );

        value = new ServerStringValue( at, "testlong" );
        
        assertFalse( value.isValid() );
    }
    
    
    /**
     * Tests to make sure the hashCode method is working properly.
     * @throws Exception on errors
     */
    @Test public void testHashCodeValidEquals() throws Exception
    {
        AttributeType at = getCaseIgnoringAttributeNoNumbersType();
        ServerStringValue v0 = new ServerStringValue( at, "Alex" );
        ServerStringValue v1 = new ServerStringValue( at, "ALEX" );
        ServerStringValue v2 = new ServerStringValue( at, "alex" );
        assertEquals( v0.hashCode(), "alex".hashCode() );
        assertEquals( v1.hashCode(), "alex".hashCode() );
        assertEquals( v2.hashCode(), "alex".hashCode() );
        assertEquals( v0, v1 );
        assertEquals( v0, v2 );
        assertEquals( v1, v2 );
        assertTrue( v0.isValid() );
        assertTrue( v1.isValid() );
        assertTrue( v2.isValid() );

        ServerStringValue v3 = new ServerStringValue( at, "Timber" );
        assertFalse( v3.equals( v0 ) );
        assertFalse( v3.equals( v1 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v3.isValid() );

        ServerStringValue v4 = new ServerStringValue( at, "Timber123" );
        assertFalse( v4.isValid() );
    }


    /**
     * Presumes an attribute which constrains it's values to some constant
     * strings: LOW, MEDUIM, HIGH.  Normalization does nothing. MatchingRules
     * are exact case matching.
     *
     * @throws Exception on errors
     */
    @Test public void testConstrainedString() throws Exception
    {
        S s = new S( "1.1.1.1", true );
            
        s.setSyntaxChecker( new SyntaxChecker() {
            public String getSyntaxOid() { return "1.1.1.1"; }
            public boolean isValidSyntax( Object value )
            {
                if ( value instanceof String )
                {
                    String strval = ( String ) value;
                    return strval.equals( "HIGH" ) || strval.equals( "LOW" ) || strval.equals( "MEDIUM" );
                }
                return false;
            }
            public void assertSyntax( Object value ) throws NamingException
            { if ( ! isValidSyntax( value ) ) throw new InvalidAttributeValueException(); }
        });

        final MR mr = new MR( "1.1.2.1" );
        mr.syntax = s;
        mr.comparator = new Comparator<String>()
        {
            public int compare( String o1, String o2 )
            {
                if ( o1 == null )
                {
                    if ( o2 == null )
                    {
                        return 0;
                    }
                    else
                    {
                        return -1;
                    }
                }
                else if ( o2 == null )
                {
                    return 1;
                }

                int i1 = getValue( o1 );
                int i2 = getValue( o2 );

                if ( i1 == i2 ) return 0;
                if ( i1 > i2 ) return 1;
                if ( i1 < i2 ) return -1;

                throw new IllegalStateException( "should not get here at all" );
            }

            int getValue( String val )
            {
                if ( val.equals( "LOW" ) ) return 0;
                if ( val.equals( "MEDIUM" ) ) return 1;
                if ( val.equals( "HIGH" ) ) return 2;
                throw new IllegalArgumentException( "Not a valid value" );
            }
        };
        mr.normalizer = new NoOpNormalizer();
        AT at = new AT( "1.1.3.1" );
        at.setEquality( mr );
        at.setSyntax( s );

        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "HIGH" );
        assertEquals( value.get(), value.get() );
        assertTrue( value.isValid() );
        value = new ServerStringValue( at, "high" );
        assertFalse( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "LOW" );
        assertTrue( v0.isValid() );
        ServerStringValue v1 = new ServerStringValue( at, "LOW" );
        assertTrue( v1.isValid() );
        ServerStringValue v2 = new ServerStringValue( at, "MEDIUM" );
        assertTrue( v2.isValid() );
        ServerStringValue v3 = new ServerStringValue( at, "HIGH" );
        assertTrue( v3.isValid() );
        ServerStringValue v4 = new ServerStringValue( at );
        assertFalse( v4.isValid() );
        ServerStringValue v5 = new ServerStringValue( at );
        assertFalse( v5.isValid() );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertEquals( 0, v0.compareTo( v1 ) );

        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertEquals( 0, v4.compareTo( v5 ) );

        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );
        assertTrue( v2.compareTo( v3 ) < 0 );
        assertTrue( v3.compareTo( v2 ) > 0 );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        ArrayList<ServerValue<String>> list = new ArrayList<ServerValue<String>>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        Collections.sort( list );

        // null ones are at first 2 indices
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        // low ones are at the 3rd and 4th indices
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        // medium then high next
        assertTrue( "since v2 \"MEDIUM\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"HIGH\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }


    /**
     * Creates a string value with an attribute type that is of a syntax
     * which accepts anything.  Also there is no normalization since the
     * value is the same as the normalized value.  This makes the at technically
     * a binary value however it can be dealt with as a string so this test
     * is still OK.
     * @throws Exception on errors
     */
    @Test public void testAcceptAllNoNormalization() throws Exception
    {
        // check that normalization and syntax checks work as expected
        ServerStringValue value = new ServerStringValue( at, "hello" );
        assertEquals( value.get(), value.get() );
        assertTrue( value.isValid() );

        // create a bunch to best tested for equals and in containers
        ServerStringValue v0 = new ServerStringValue( at, "hello" );
        ServerStringValue v1 = new ServerStringValue( at, "hello" );
        ServerStringValue v2 = new ServerStringValue( at, "next0" );
        ServerStringValue v3 = new ServerStringValue( at, "next1" );
        ServerStringValue v4 = new ServerStringValue( at );
        ServerStringValue v5 = new ServerStringValue( at );

        // check equals
        assertTrue( v0.equals( v1 ) );
        assertTrue( v1.equals( v0 ) );
        assertTrue( v4.equals( v5 ) );
        assertTrue( v5.equals( v4 ) );
        assertFalse( v2.equals( v3 ) );
        assertFalse( v3.equals( v2 ) );

        // add all except v1 and v5 to a set
        HashSet<ServerStringValue> set = new HashSet<ServerStringValue>();
        set.add( v0 );
        set.add( v2 );
        set.add( v3 );
        set.add( v4 );

        // check contains method
        assertTrue( "since v1.equals( v0 ) and v0 was added then this should be true", set.contains( v1 ) );
        assertTrue( "since v4.equals( v5 ) and v4 was added then this should be true", set.contains( v5 ) );

        // check ordering based on the comparator
        ArrayList<ServerStringValue> list = new ArrayList<ServerStringValue>();
        list.add( v1 );
        list.add( v3 );
        list.add( v5 );
        list.add( v0 );
        list.add( v2 );
        list.add( v4 );

        Comparator<ServerStringValue> c = new Comparator<ServerStringValue>()
        {
            public int compare( ServerStringValue o1, ServerStringValue o2 )
            {
                byte[] b1 = new byte[0];
                byte[] b2 = new byte[0];

                try
                {
                    if ( o1 != null )
                    {
                        String n1 = o1.get();
                        if ( n1 != null )
                        {
                            b1 = n1.getBytes( "UTF-8" );
                        }
                    }

                    if ( o2 != null )
                    {
                        String n2 = o2.get();
                        if ( n2 != null )
                        {
                            b2 = o2.get().getBytes( "UTF-8" );
                        }
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }

                try
                {
                    return mr.getComparator().compare( b1, b2 );
                }
                catch ( Exception e )
                {
                    throw new IllegalStateException( "Normalization and comparison should succeed!", e );
                }
            }
        };

        Collections.sort( list, c );

        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 0 ).equals( v5 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v4 ) );
        assertTrue( "since v4 equals v5 and has no value either could be at index 0 & 1", list.get( 1 ).equals( v5 ) );

        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 2 ).equals( v1 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v0 ) );
        assertTrue( "since v0 equals v1 either could be at index 2 & 3", list.get( 3 ).equals( v1 ) );

        assertTrue( "since v2 \"next0\" should be at index 4", list.get( 4 ).equals( v2 ) );
        assertTrue( "since v3 \"next1\" should be at index 5", list.get( 5 ).equals( v3 ) );

        assertEquals( 6, list.size() );
    }

    
    /**
     * Test serialization of a StringValue which has a normalized value
     */
    @Test public void testNormalizedStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "  Test   Test  " );
        
        sv.normalize();
        String normalized = sv.getNormalized();
        
        assertEquals( "test test", normalized );
        assertEquals( "  Test   Test  ", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of a StringValue which does not have a normalized value
     */
    @Test public void testNoNormalizedStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "test" );
        
        sv.normalize();
        String normalized = sv.getNormalized();
        
        assertEquals( "test", normalized );
        assertEquals( "test", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of a null StringValue
     */
    @Test public void testNullStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at );
        
        sv.normalize();
        String normalized = sv.getNormalized();
        
        assertEquals( null, normalized );
        assertEquals( null, sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }


    /**
     * Test serialization of an empty StringValue
     */
    @Test public void testEmptyStringValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        // First check with a value which will be normalized
        ServerStringValue sv = new ServerStringValue( at, "" );
        
        sv.normalize();
        String normalized = sv.getNormalized();
        
        assertEquals( "", normalized );
        assertEquals( "", sv.get() );
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );
        
        sv.writeExternal( out );
        
        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );
        
        ServerStringValue sv2 = new ServerStringValue( at );
        sv2.readExternal( in );
        
        assertEquals( sv, sv2 );
   }
}
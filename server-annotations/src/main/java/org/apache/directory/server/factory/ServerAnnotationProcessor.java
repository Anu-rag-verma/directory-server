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
package org.apache.directory.server.factory;


import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.TrustManager;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.util.Network;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.annotations.CreateConsumer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmProvider;
import org.apache.directory.server.ldap.replication.SyncReplConfiguration;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;


/**
 * 
 * Annotation processor for creating LDAP and Kerberos servers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ServerAnnotationProcessor
{
    private ServerAnnotationProcessor()
    {
    }


    private static void createTransports( LdapServer ldapServer, CreateTransport[] transportBuilders )
    {
        if ( transportBuilders.length != 0 )
        {
            for ( CreateTransport transportBuilder : transportBuilders )
            {
                List< Transport > transports = createTransports( transportBuilder );
                
                for ( Transport t : transports )
                {
                    ldapServer.addTransports( t );
                }
            }
        }
        else
        {
            // Create default LDAP and LDAPS transports
            try 
            {
                int port = getFreePort();
                Transport ldap = new TcpTransport( port );
                ldapServer.addTransports( ldap );
            }
            catch ( IOException ioe )
            {
                // Don't know what to do here...
            }

            try 
            {
                int port = getFreePort();
                Transport ldaps = new TcpTransport( port );
                ldaps.setEnableSSL( true );
                ldapServer.addTransports( ldaps );
            }
            catch ( IOException ioe )
            {
                // Don't know what to do here...
            }
        }
    }


    /**
     * Just gives an instance of {@link LdapServer} without starting it.
     * For getting a running LdapServer instance see {@link #createLdapServer(CreateLdapServer, DirectoryService)}
     * @see #createLdapServer(CreateLdapServer, DirectoryService)
     * 
     * @param createLdapServer The LdapServer to create
     * @param directoryService the directory service
     * @return The created LdapServer
     */
    public static LdapServer instantiateLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService )
    {
        if ( createLdapServer != null )
        {
            LdapServer ldapServer = new LdapServer();

            ldapServer.setServiceName( createLdapServer.name() );

            // Read the transports
            createTransports( ldapServer, createLdapServer.transports() );

            // Associate the DS to this LdapServer
            ldapServer.setDirectoryService( directoryService );

            // Propagate the anonymous flag to the DS
            directoryService.setAllowAnonymousAccess( createLdapServer.allowAnonymousAccess() );

            ldapServer.setSaslHost( createLdapServer.saslHost() );

            ldapServer.setSaslPrincipal( createLdapServer.saslPrincipal() );

            if ( !Strings.isEmpty( createLdapServer.keyStore() ) )
            {
                ldapServer.setKeystoreFile( createLdapServer.keyStore() );
                ldapServer.setCertificatePassword( createLdapServer.certificatePassword() );
            }
            else
            {
                try
                {
                    // Create a temporary keystore, be sure to remove it when exiting the test
                    File keyStoreFile = CertificateUtil.createTempKeyStore( "testStore", "secret".toCharArray() );
                    ldapServer.setKeystoreFile( keyStoreFile.getAbsolutePath() );
                    ldapServer.setCertificatePassword( "secret" );
                }
                catch ( Exception e )
                {
                    
                }
            }

            for ( Class<?> extOpClass : createLdapServer.extendedOpHandlers() )
            {
                try
                {
                    ExtendedOperationHandler extOpHandler = ( ExtendedOperationHandler ) extOpClass.newInstance();
                    ldapServer.addExtendedOperationHandler( extOpHandler );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_41001_CANOT_ADD_EXTENDED_OPERATION, extOpClass.getName() ), e );
                }
            }

            for ( SaslMechanism saslMech : createLdapServer.saslMechanisms() )
            {
                try
                {
                    MechanismHandler handler = ( MechanismHandler ) saslMech.implClass().newInstance();
                    ldapServer.addSaslMechanismHandler( saslMech.name(), handler );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException(
                        I18n.err( I18n.ERR_41002_CANNOT_ADD_SASL_MLECHANISM, saslMech.name(), saslMech.implClass().getName() ), e );
                }
            }

            NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get(
                SupportedSaslMechanisms.NTLM );

            if ( ntlmHandler != null )
            {
                Class<?> ntlmProviderClass = createLdapServer.ntlmProvider();
                // default value is a invalid Object.class
                if ( ( ntlmProviderClass != null ) && ( ntlmProviderClass != Object.class ) )
                {
                    try
                    {
                        ntlmHandler.setNtlmProvider( ( NtlmProvider ) ntlmProviderClass.newInstance() );
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( I18n.err( I18n.ERR_41003_FAILED_TO_ADD_NTLM_PROVIDER ), e );
                    }
                }
            }

            List<String> realms = new ArrayList<>();
            for ( String s : createLdapServer.saslRealms() )
            {
                realms.add( s );
            }

            ldapServer.setSaslRealms( realms );

            if ( createLdapServer.trustManagers() != null && createLdapServer.trustManagers().length > 0 )
            {
                TrustManager[] trustManagers = new TrustManager[createLdapServer.trustManagers().length];
                for ( int i = 0; i < createLdapServer.trustManagers().length; i++ )
                {
                    try
                    {
                        trustManagers[i] = ( TrustManager ) createLdapServer.trustManagers()[i].newInstance();
                    }
                    catch ( InstantiationException | IllegalAccessException e )
                    {
                        throw new RuntimeException( I18n.err( I18n.ERR_41004_INVALID_TRUST_MANAGER_CLASS, createLdapServer.trustManagers()[i].getName() ), e );
                    }
                }
                ldapServer.setTrustManagers( trustManagers );
            }
            
            // Set the limits
            ldapServer.setMaxSizeLimit( createLdapServer.maxSizeLimit() );
            ldapServer.setMaxTimeLimit( createLdapServer.maxTimeLimit() );
            
            // Set the searchBaseDN
            ldapServer.setSearchBaseDn( createLdapServer.searchBaseDn() );

            return ldapServer;
        }
        else
        {
            return null;
        }
    }


    /**
     * Returns an LdapServer instance and starts it before returning the instance, infering
     * the configuration from the Stack trace
     *  
     * @param directoryService the directory service
     * @return a running LdapServer instance
     * @throws ClassNotFoundException If the CreateLdapServer class cannot be loaded
     */
    public static LdapServer getLdapServer( DirectoryService directoryService ) throws ClassNotFoundException
    {
        Object instance = AnnotationUtils.getInstance( CreateLdapServer.class );
        LdapServer ldapServer = null;

        if ( instance != null )
        {
            CreateLdapServer createLdapServer = ( CreateLdapServer ) instance;

            ldapServer = createLdapServer( createLdapServer, directoryService );
        }

        return ldapServer;
    }


    /**
     * Create a replication consumer
     */
    private static ReplicationConsumer createConsumer( CreateConsumer createConsumer )
    {
        ReplicationConsumer consumer = new ReplicationConsumerImpl();

        SyncReplConfiguration config = new SyncReplConfiguration();
        
        String remoteHost = createConsumer.remoteHost();
        
        if ( Strings.isEmpty( remoteHost ) )
        {
            remoteHost = Network.LOOPBACK_HOSTNAME;
        }
        
        config.setRemoteHost( remoteHost );
        config.setRemotePort( createConsumer.remotePort() );
        config.setReplUserDn( createConsumer.replUserDn() );
        config.setReplUserPassword( Strings.getBytesUtf8( createConsumer.replUserPassword() ) );
        config.setUseTls( createConsumer.useTls() );
        config.setBaseDn( createConsumer.baseDn() );
        config.setRefreshInterval( createConsumer.refreshInterval() );
        config.setStrictCertVerification( createConsumer.strictCertVerification() );

        consumer.setConfig( config );

        return consumer;
    }


    /**
     * creates an LdapServer and starts before returning the instance, infering
     * the configuration from the Stack trace
     *  
     * @return a running LdapServer instance
     * @throws ClassNotFoundException If the CreateConsumer class cannot be loaded
     */
    public static ReplicationConsumer createConsumer() throws ClassNotFoundException
    {
        Object instance = AnnotationUtils.getInstance( CreateConsumer.class );
        ReplicationConsumer consumer = null;

        if ( instance != null )
        {
            CreateConsumer createConsumer = ( CreateConsumer ) instance;

            consumer = createConsumer( createConsumer );
        }

        return consumer;
    }


    /**
     * creates an LdapServer and starts before returning the instance
     *  
     * @param createLdapServer the annotation containing the custom configuration
     * @param directoryService the directory service
     * @return a running LdapServer instance
     */
    public static LdapServer createLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService )
    {
        LdapServer ldapServer = instantiateLdapServer( createLdapServer, directoryService );

        if ( ldapServer == null )
        {
            return null;
        }

        // Launch the server
        try
        {
            ldapServer.start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return ldapServer;
    }


    /**
     * Create a new instance of LdapServer
     *
     * @param description A description for the created LdapServer
     * @param directoryService The associated DirectoryService
     * @return An LdapServer instance 
     */
    public static LdapServer createLdapServer( AnnotatedElement annotation, DirectoryService directoryService )
    {
        CreateLdapServer createLdapServer = annotation.getAnnotation( CreateLdapServer.class );

        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService );
    }


    @SuppressWarnings("unchecked")
    private static Annotation getAnnotation( Class annotationClass ) throws Exception
    {
        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // In Java5 the 0th stacktrace element is: java.lang.Thread.dumpThreads(Native Method)
        int index = stackTrace[0].getMethodName().equals( "dumpThreads" ) ? 4 : 3;

        // Get the enclosing class
        Class<?> classCaller = Class.forName( stackTrace[index].getClassName() );

        // Get the current method
        String methodCaller = stackTrace[index].getMethodName();

        // Check if we have any annotation associated with the method
        Method[] methods = classCaller.getMethods();

        for ( Method method : methods )
        {
            if ( methodCaller.equals( method.getName() ) )
            {
                Annotation annotation = method.getAnnotation( annotationClass );

                if ( annotation != null )
                {
                    return annotation;
                }
            }
        }

        // No : look at the class level
        return classCaller.getAnnotation( annotationClass );
    }

    private static List< Transport > createTransports( CreateTransport transportBuilder )
    {
        String protocol = transportBuilder.protocol();
        int port = transportBuilder.port();
        int nbThreads = transportBuilder.nbThreads();
        int backlog = transportBuilder.backlog();
        String address = transportBuilder.address();
        boolean clientAuth = transportBuilder.clientAuth();
        
        if ( Strings.isEmpty( address ) )
        {
            address = Network.LOOPBACK_HOSTNAME;
        }

        if ( port <= 0 )
        {
            try
            {
                port = getFreePort();
            }
            catch ( IOException ioe )
            {
                // Don't know what to do here...
            }
        }

        if ( protocol.equalsIgnoreCase( "TCP" ) || protocol.equalsIgnoreCase( "LDAP" ) )
        {
            Transport tcp = new TcpTransport( address, port, nbThreads, backlog );
            return Collections.singletonList( tcp );
        }
        else if ( protocol.equalsIgnoreCase( "LDAPS" ) )
        {
            Transport tcp = new TcpTransport( address, port, nbThreads, backlog );
            tcp.setEnableSSL( true );
            ( ( TcpTransport ) tcp ).setWantClientAuth( clientAuth );
            return Collections.singletonList( tcp );
        }
        else if ( protocol.equalsIgnoreCase( "UDP" ) )
        {
            Transport udp = new UdpTransport( address, port );
            return Collections.singletonList( udp );
        }
        else if ( protocol.equalsIgnoreCase( "KRB" ) || protocol.equalsIgnoreCase( "CPW" ) )
        {
            Transport tcp = new TcpTransport( address, port, nbThreads, backlog );
            List< Transport > transports = new ArrayList<>();
            transports.add( tcp );
            
            Transport udp = new UdpTransport( address, port );
            transports.add( udp );
            return transports;
        }
        
        throw new IllegalArgumentException( I18n.err( I18n.ERR_41000_UNSUPPORTED_PROTOCOL_TYPE, protocol ) );
    }


    private static int getFreePort() throws IOException
    {
        ServerSocket ss = new ServerSocket( 0 );
        int port = ss.getLocalPort();
        ss.close();
        
        return port;
    }

}

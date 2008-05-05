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
package org.apache.directory.server.ldap.handlers.bind.ntlm;


/**
 * An NTLM authentication service provider.  Multiple providers may be
 * utilized to conduct the NTLM negotiation over various protocols or by
 * calling native SSPI interfaces.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public interface NtlmProvider
{
    /**
     * Handles a Type 1 NTLM response from the client to generate an NTLM
     * Type 2 challenge message.
     *
     * @param type1reponse the Type 1 NTLM response from client
     * @return the NTLM Type 2 message with the challenge
     * @throws NtlmNegotiationException if there are communication, message
     * format or NTLM negotiation exceptions
     */
    byte[] generateChallenge( byte[] type1reponse ) throws NtlmNegotiationException;


    /**
     * Handles a Type 3 NTLM reponse from the client.
     *
     * @param type3response the Type 3 NTLM reponse from the client
     * @return the result of the successful authentication from the server
     * @throws NtlmNegotiationException if there are communication, message
     * format or NTLM negotiation exceptions
     * @throws NtlmAuthenticationException if authentication fails for the user
     */
    byte[] authenticate( byte[] type3response ) throws NtlmAuthenticationException, NtlmNegotiationException;
}

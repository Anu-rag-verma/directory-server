/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.authn;


import org.apache.directory.shared.ldap.codec.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.shared.ldap.exception.LdapException;


/**
 * A exception class defined for PasswordPolicy related errors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordPolicyException extends LdapException
{

    /** password policy error code */
    private PasswordPolicyErrorEnum errorCode;


    public PasswordPolicyException( Throwable cause )
    {
        super( cause );
    }


    public PasswordPolicyException( String message )
    {
        super( message );
    }


    public PasswordPolicyException( String message, PasswordPolicyErrorEnum errorCode )
    {
        super( message );
        this.errorCode = errorCode;
    }


    public PasswordPolicyException( PasswordPolicyErrorEnum errorCode )
    {
        this.errorCode = errorCode;
    }


    public PasswordPolicyErrorEnum getErrorCode()
    {
        return errorCode;
    }
}

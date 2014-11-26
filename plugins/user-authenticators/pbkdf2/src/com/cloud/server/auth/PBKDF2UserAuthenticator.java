// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.server.auth;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={UserAuthenticator.class})
public class PBKDF2UserAuthenticator extends DefaultUserAuthenticator {
    public static final Logger s_logger = Logger.getLogger(PBKDF2UserAuthenticator.class);
    private static final String s_defaultPassword = "000000000000000000000000000=";
    private static final String s_defaultSalt = "0000000000000000000000000000000=";
    @Inject
    private UserAccountDao _userAccountDao;
    private static final int s_saltlen = 32;
    private static final int s_rounds = 2000;
    private static final int s_keylen = 192; //cloudstack user table has varchar(255) for password

    /* (non-Javadoc)
     * @see com.cloud.server.auth.UserAuthenticator#authenticate(java.lang.String, java.lang.String, java.lang.Long, java.util.Map)
     */
    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password,
            Long domainId, Map<String, Object[]> requestParameters) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieving user: " + username);
        }
        boolean realUser = true;
        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            s_logger.debug("Unable to find user with " + username + " in domain " + domainId);
            realUser = false;
        }
        /* Fake Data */
        String realPassword = new String(s_defaultPassword);
        byte[] salt = new String(s_defaultSalt).getBytes();
        int rounds = s_rounds;
        try {
            if (realUser) {
                String storedPassword[] = user.getPassword().split(":");
                if (!(storedPassword.length == 3 && StringUtils.isNumeric(storedPassword[2]))) {
                    s_logger.warn("The stored password for " + username + " isn't in the right format for this authenticator");
                    realUser = false;
                } else {
                    realPassword = storedPassword[1];
                    salt = Base64.decode(storedPassword[0]);
                    rounds = Integer.parseInt(storedPassword[2]);
                }
            }
            String hashedPassword = encode(password, salt, rounds);
            /* constantTimeEquals comes first in boolean since we need to thwart timing attacks */
            boolean result = constantTimeEquals(realPassword, hashedPassword) && realUser;
            ActionOnFailedAuthentication action = null;
            if (!result && realUser) {
                action = ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT;
            }
            return new Pair<Boolean, ActionOnFailedAuthentication>(result, action);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (InvalidKeySpecException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    /* (non-Javadoc)
     * @see com.cloud.server.auth.UserAuthenticator#encode(java.lang.String)
     */
    @Override
    public String encode(String password) {
        // 1. Generate the salt
        SecureRandom randomGen;
        try {
            randomGen = SecureRandom.getInstance("SHA1PRNG");

            byte salt[] = new byte[s_saltlen];
            randomGen.nextBytes(salt);

            String saltString = new String(Base64.encode(salt));
            String hashString = encode(password, salt, s_rounds);

            // 3. concatenate the two and return
            return saltString + ":" + hashString + ":" + s_rounds;
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to hash password", e);
        } catch (InvalidKeySpecException e) {
            s_logger.error("Exception in EncryptUtil.createKey ", e);
            throw new CloudRuntimeException("Unable to hash password", e);
        }
    }

    public String encode(String password, byte[] salt, int rounds) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {
        // 2. Hash the password with the salt
        //MessageDigest md = MessageDigest.getInstance("SHA-256");
        //md.update(hashSource);
        //byte[] digest = md.digest();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, s_keylen);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey sKey = keyFactory.generateSecret(spec);
        Key key = new SecretKeySpec(sKey.getEncoded(), "AES");
        return new String(Base64.encode(key.getEncoded()));
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int result = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}

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

package src.com.cloud.server.auth.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.server.auth.PBKDF2UserAuthenticator;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticatorTest {

    @Mock
    UserAccount adminAccount;

    @Mock
    UserAccount adminAccount20Byte;

    @Mock
    UserAccountDao _userAccountDao;

    @InjectMocks
    PBKDF2UserAuthenticator authenticator;

	@Before
	public void setUp() throws Exception {
        try {
            authenticator.configure("PBKDF2", Collections.<String, Object> emptyMap());
        } catch (ConfigurationException e) {
            fail(e.toString());
        }

        when(_userAccountDao.getUserAccount("admin", 0L)).thenReturn(adminAccount);
        when(_userAccountDao.getUserAccount("fake", 0L)).thenReturn(null);
        //32 byte salt, and password="pkpkpk"
        when(adminAccount.getPassword()).thenReturn("dml2OjgR5/HCKaXgHkiKPsfWUamYfkL2mCSIp5/VDbM=:yJaY597KYCB6e5s0EwaPoDLnbKUeq69S:2000");
	}

	@Test
    public void testEncode() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException {

        String encodedPassword = authenticator.encode("pkpkpk");

		String storedPassword[] = encodedPassword.split(":");
        assertEquals("hash must consist of three components", storedPassword.length, 3);

        byte salt[] = Base64.decode(storedPassword[0]);
        String hashedPassword = authenticator.encode("pkpkpk", salt, Integer.parseInt(storedPassword[2]));

        assertEquals("compare hashes", storedPassword[1], hashedPassword);

	}

    @Test
    public void testAuthentication() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Map<String, Object[]> dummyMap = new HashMap<String, Object[]>();
        assertEquals("32 byte salt authenticated", true, authenticator.authenticate("admin", "pkpkpk", 0L, dummyMap).first());
        assertEquals("fake user not authenticated", false, authenticator.authenticate("fake", "fake", 0L, dummyMap).first());
        assertEquals("bad password not authenticated", false, authenticator.authenticate("admin", "fake", 0L, dummyMap).first());
    }
}

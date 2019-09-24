/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.model.PublicKey;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.data.AccountTestData;
import com.apollocurrency.aplwallet.apl.util.cache.InMemoryCacheManager;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountPublicKeyServiceTest {
    private PropertiesHolder propertiesHolder = mock(PropertiesHolder.class);
    private Blockchain blockchain = mock(BlockchainImpl.class);
    private PublicKeyTable publicKeyTable = mock(PublicKeyTable.class);
    private GenesisPublicKeyTable genesisPublicKeyTable = mock(GenesisPublicKeyTable.class);

    private AccountPublicKeyService accountPublicKeyService;
    private AccountTestData testData;
    private InMemoryCacheManager cacheManager = mock(InMemoryCacheManager.class);

    @BeforeEach
    void setUp() {
        testData = new AccountTestData();
        doReturn(false).when(propertiesHolder).getBooleanProperty("apl.enablePublicKeyCache");
        accountPublicKeyService = spy(new AccountPublicKeyServiceImpl(
                propertiesHolder,
                blockchain,
                publicKeyTable,
                genesisPublicKeyTable,
                cacheManager
        ));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getCount() {
        int publicCount = 200000;
        int genesisCount = 300000;
        doReturn(publicCount).when(publicKeyTable).getCount();
        doReturn(genesisCount).when(genesisPublicKeyTable).getCount();
        assertEquals(publicCount + genesisCount, accountPublicKeyService.getCount());
    }

    @Test
    void getPublicKey() {
        long accountId = 2728325718715804811L;

        assertNull(accountPublicKeyService.getPublicKey(accountId));

        PublicKey expectedPublicKey = new PublicKey(accountId,null,1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        assertNull(accountPublicKeyService.getPublicKey(accountId));

        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(),1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        assertEquals(expectedPublicKey.getPublicKey(), accountPublicKeyService.getPublicKey(accountId));
    }

    @Test
    void setOrVerify() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = new PublicKey(accountId,null,1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        //set new key
        assertTrue(accountPublicKeyService.setOrVerify(accountId, testData.PUBLIC_KEY_STR.getBytes()));

        //verify
        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(),1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        //true, the same keys
        assertTrue(accountPublicKeyService.setOrVerify(accountId, testData.PUBLIC_KEY_STR.getBytes()));
        //false, different keys
        assertFalse(accountPublicKeyService.setOrVerify(accountId, testData.PUBLIC_KEY_STR2.getBytes()));
    }

    @Test
    void testApply_newKey() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = new PublicKey(accountId,null,1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        //publickKey == null
        accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR.getBytes(), false);
        verify(publicKeyTable, times(1)).insert(any(PublicKey.class));
        verify(genesisPublicKeyTable, never()).insert(any(PublicKey.class));
        assertEquals(expectedPublicKey, testData.ACC_1.getPublicKey());
    }

    @Test
    void testApply() {
        long accountId = 2728325718715804811L;
        PublicKey expectedPublicKey = null;

        //check public keys
        expectedPublicKey = new PublicKey(accountId, testData.PUBLIC_KEY_STR.getBytes(),1000);
        doReturn(expectedPublicKey).when(genesisPublicKeyTable).get(any());
        //key mismatch
        assertThrows(IllegalStateException.class,() -> accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR2.getBytes(), false));
        //key match
        accountPublicKeyService.apply(testData.ACC_1, testData.PUBLIC_KEY_STR.getBytes(), false);
        verify(publicKeyTable, times(1)).insert(any(PublicKey.class));
        verify(genesisPublicKeyTable, never()).insert(any(PublicKey.class));
        assertEquals(expectedPublicKey, testData.ACC_1.getPublicKey());
    }

}
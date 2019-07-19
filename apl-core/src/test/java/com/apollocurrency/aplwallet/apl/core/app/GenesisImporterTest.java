package com.apollocurrency.aplwallet.apl.core.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.account.GenesisPublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.account.PublicKeyTable;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfigUpdater;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.BlockDaoImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DerivedDbTablesRegistryImpl;
import com.apollocurrency.aplwallet.apl.core.db.ShardDaoJdbcImpl;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.core.db.dao.BlockIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.dao.TransactionIndexDao;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextConfigImpl;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchEngine;
import com.apollocurrency.aplwallet.apl.core.db.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.BalancesPublicKeysTestData;
import com.apollocurrency.aplwallet.apl.data.DbTestData;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import com.apollocurrency.aplwallet.apl.extension.TemporaryFolderExtension;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import jnr.ffi.annotations.In;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DBIterator;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

@Slf4j
@EnableWeld
//@Execution(ExecutionMode.SAME_THREAD) //for better performance we will not recreate 3 datasources for each test method
class GenesisImporterTest {

//    private final Path blockchainTestDbPath = createPath("blockchainTestDbPath");
    @RegisterExtension
//    DbExtension extension = new DbExtension(createPath("genesisImport").toAbsolutePath());
    DbExtension extension = new DbExtension(DbTestData.getDbFileProperties(createPath("genesisImport").toAbsolutePath().toString()));
//    DbExtension extension = new DbExtension(Map.of("currency", List.of("code","name", "description"), "tagged_data", List.of("name","description","tags")));
    @RegisterExtension
    static TemporaryFolderExtension temporaryFolderExtension = new TemporaryFolderExtension();

    private BlockchainConfig blockchainConfig = mock(BlockchainConfig.class);
    private BlockchainConfigUpdater blockchainConfigUpdater = mock(BlockchainConfigUpdater.class);
    private HeightConfig config = Mockito.mock(HeightConfig.class);
    private Chain chain = Mockito.mock(Chain.class);
    private ConfigDirProvider configDirProvider = mock(ConfigDirProvider.class);
    private AplAppStatus aplAppStatus = mock(AplAppStatus.class);

    @WeldSetup
    public WeldInitiator weld  = WeldInitiator.from(
            AccountTable.class, FullTextConfigImpl.class, DerivedDbTablesRegistryImpl.class, PropertiesHolder.class,
            ShardDaoJdbcImpl.class,
            BlockIndexDao.class, TransactionDaoImpl.class, BlockchainImpl.class,
            JdbiHandleFactory.class, BlockDaoImpl.class, TransactionIndexDao.class, DaoConfig.class)
//            .addBeans(MockBean.of(mock(Blockchain.class), BlockchainImpl.class))
            .addBeans(MockBean.of(mock(EpochTime.class), EpochTime.class))
            .addBeans(MockBean.of(configDirProvider, ConfigDirProvider.class))
            .addBeans(MockBean.of(blockchainConfig, BlockchainConfig.class))
            .addBeans(MockBean.of(blockchainConfigUpdater, BlockchainConfigUpdater.class))
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getLuceneFullTextSearchEngine(), FullTextSearchEngine.class))
            .addBeans(MockBean.of(extension.getFtl(), FullTextSearchService.class))
            .addBeans(MockBean.of(aplAppStatus, AplAppStatus.class))
            .build();

    private GenesisImporter genesisImporter;
    @Inject
    PropertiesHolder propertiesHolder;
    @Inject
    Blockchain blockchain;
    @Inject
    AccountTable accountTable;
    PublicKeyTable publicKeyTable;
    BalancesPublicKeysTestData testData;

    private Path createPath(String fileName) {
        try {
            return temporaryFolderExtension.newFolder().toPath().resolve(fileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @BeforeEach
    void setUp() {
        doReturn(config).when(blockchainConfig).getCurrentConfig();
        doReturn(chain).when(blockchainConfig).getChain();
//        doReturn(UUID.fromString("a2e9b946-290b-48b6-9985-dc2e5a5860a1")).when(chain).getChainId();
        doReturn("data/genesisAccounts-testnet.json").when(chain).getGenesisLocation();
        doReturn("conf").when(configDirProvider).getConfigDirectoryName();
        doReturn(3000000000000000000L).when(config).getMaxBalanceATM();
//        blockchainConfig.getCurrentConfig().getMaxBalanceATM(); // 3000000000000000000
        genesisImporter = new GenesisImporter(blockchainConfig, configDirProvider,
                blockchainConfigUpdater, extension.getDatabaseManager(), aplAppStatus);
        GenesisPublicKeyTable.getInstance().init();
        publicKeyTable = new PublicKeyTable(blockchain);
        publicKeyTable.init();
        Account.init(extension.getDatabaseManager(), propertiesHolder, null,
                null, blockchain, null, publicKeyTable, accountTable, null);
        testData = new BalancesPublicKeysTestData();
    }

    @Test
    void newGenesisBlock() {
        Block block = genesisImporter.newGenesisBlock();
        assertNotNull(block);
        assertEquals(1739068987193023818L, block.getGeneratorId());
        assertEquals(4997592126877716673L, block.getId());
        assertEquals(0, block.getHeight());
        assertEquals("1259ec21d31a30898d7cd1609f80d9668b4778e3d97e941044b39f0c44d2e51b",
                Convert.toHexString( genesisImporter.getCreatorPublicKey() ));
        assertEquals("1259ec21d31a30898d7cd1609f80d9668b4778e3d97e941044b39f0c44d2e51b",
                Convert.toHexString( genesisImporter.getComputedDigest() ));
        assertEquals(1739068987193023818L, genesisImporter.CREATOR_ID);
        assertEquals(1515931200000L, genesisImporter.EPOCH_BEGINNING);
    }

    @Test
    void applyTrue() {
        genesisImporter.apply(true);
        int count = publicKeyTable.getCount();
        assertEquals(10, count);
        count = GenesisPublicKeyTable.getInstance().getCount();
        assertEquals(10, count);
        checkImportedPublicKeys(10);
    }

    @Test
    void applyFalse() {
        genesisImporter.apply(false);
        int count = publicKeyTable.getCount();
        assertEquals(10, count);
        count = GenesisPublicKeyTable.getInstance().getCount();
        assertEquals(19, count);
        Account genesisAccount = Account.getAccount(genesisImporter.CREATOR_ID);
        assertEquals(-43678392484062L , genesisAccount.getBalanceATM());
//        checkImportedPublicKeys(10);

    }

    private void checkImportedPublicKeys(int countExpected) {
        DbIterator<PublicKey> result = GenesisPublicKeyTable.getInstance().getAll(0, 10);
        int countActual = 0;
        while (result.hasNext()) {
            PublicKey publicKey = result.next();
            String toHexString = Convert.toHexString(publicKey.getPublicKey());
            log.trace("publicKeySet contains key = {} = {}", toHexString, testData.publicKeySet.contains(toHexString));
            assertTrue(testData.publicKeySet.contains( Convert.toHexString(publicKey.getPublicKey())),
                    "ERROR, publicKeySet doesn't contain key = "
                            + Convert.toHexString(publicKey.getPublicKey()) );
            countActual++;
        }
        assertEquals(countExpected, countActual);
    }


    @Test
    void loadGenesisAccounts() {
        List<Map.Entry<String, Long>> result = genesisImporter.loadGenesisAccounts();
        assertNotNull(result);
        assertEquals(9, result.size()); // genesis is skipped
    }
}
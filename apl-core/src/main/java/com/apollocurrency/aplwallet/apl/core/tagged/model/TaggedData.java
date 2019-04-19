/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.model;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedPersistentDbTable;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedPrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedValuesDbTable;
import com.apollocurrency.aplwallet.apl.util.Search;
import org.slf4j.Logger;

public class TaggedData {
/*
    private static final Logger LOG = getLogger(TaggedData.class);

    private static final LongKeyFactory<TaggedData> taggedDataKeyFactory = new LongKeyFactory<TaggedData>("id") {
        @Override
        public DbKey newKey(TaggedData taggedData) {
            return taggedData.dbKey;
        }

    };
    private static LongKeyFactory<UnconfirmedTransaction> keyFactory = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();

    private static BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();
    private static DatabaseManager databaseManager;

    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }
*/

/*
    private static final VersionedPrunableDbTable<TaggedData> taggedDataTable = new VersionedPrunableDbTable<TaggedData>(
            "tagged_data", taggedDataKeyFactory, "name,description,tags") {

        @Override
        public TaggedData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new TaggedData(rs, dbKey);
        }

        @Override
        public void save(Connection con, TaggedData taggedData) throws SQLException {
            taggedData.save(con);
        }

        @Override
        protected String defaultSort() {
            return " ORDER BY block_timestamp DESC, height DESC, db_id DESC ";
        }

        @Override
        protected void prune() {
            if (blockchainConfig.isEnablePruning()) {
                TransactionalDataSource dataSource = lookupDataSource();
                try (Connection con = dataSource.getConnection();
                     PreparedStatement pstmtSelect = con.prepareStatement("SELECT parsed_tags "
                             + "FROM tagged_data WHERE transaction_timestamp < ? AND latest = TRUE ")) {
                    int expiration = timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime();
                    pstmtSelect.setInt(1, expiration);
                    Map<String,Integer> expiredTags = new HashMap<>();
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        while (rs.next()) {
                            Object[] array = (Object[])rs.getArray("parsed_tags").getArray();
                            for (Object tag : array) {
                                Integer count = expiredTags.get(tag);
                                expiredTags.put((String)tag, count != null ? count + 1 : 1);
                            }
                        }
                    }
//                    Tag.delete(expiredTags); // TODO: YL review and change
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
            }
            super.prune();
        }

    };
*/

/*
    private static final LongKeyFactory<TaggedDataTimestamp> timestampKeyFactory = new LongKeyFactory<TaggedDataTimestamp>("id") {
        @Override
        public DbKey newKey(TaggedDataTimestamp timestamp) {
            return timestamp.dbKey;
        }

    };
    private static final VersionedEntityDbTable<TaggedDataTimestamp> timestampTable = new VersionedEntityDbTable<TaggedDataTimestamp>(
            "tagged_data_timestamp", timestampKeyFactory) {

        @Override
        public TaggedDataTimestamp load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new TaggedDataTimestamp(rs, dbKey);
        }

        @Override
        public void save(Connection con, TaggedDataTimestamp timestamp) throws SQLException {
//            timestamp.save(con); // TODO: YL review and change
        }

    };
*/

/*
    public static final class Tag {

        private static final StringKeyFactory<Tag> tagDbKeyFactory = new StringKeyFactory<Tag>("tag") {
            @Override
            public DbKey newKey(Tag tag) {
                return tag.dbKey;
            }
        };

        private static final VersionedPersistentDbTable<Tag> tagTable = new VersionedPersistentDbTable<Tag>("data_tag", tagDbKeyFactory) {

            @Override
            public Tag load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
                return new Tag(rs, dbKey);
            }

            @Override
            public void save(Connection con, Tag tag) throws SQLException {
                tag.save(con);
            }

            @Override
            public String defaultSort() {
                return " ORDER BY tag_count DESC, tag ASC ";
            }

        };

        public static int getDataTagCount() {
            return tagTable.getCount();
        }

        public static DbIterator<Tag> getAllTags(int from, int to) {
            return tagTable.getAll(from, to);
        }

        public static DbIterator<Tag> getTagsLike(String prefix, int from, int to) {
            DbClause dbClause = new DbClause.LikeClause("tag", prefix);
            return tagTable.getManyBy(dbClause, from, to, " ORDER BY tag ");
        }

        private static void init() {}

        private static void add(TaggedData taggedData) {
            for (String tagValue : taggedData.getParsedTags()) {
                Tag tag = tagTable.get(tagDbKeyFactory.newKey(tagValue));
                if (tag == null) {
                    tag = new Tag(tagValue, blockchain.getHeight());
                }
                tag.count += 1;
                tagTable.insert(tag);
            }
        }

        private static void add(TaggedData taggedData, int height) {
            try (Connection con = lookupDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("UPDATE data_tag SET tag_count = tag_count + 1 WHERE tag = ? AND height >= ?")) {
                for (String tagValue : taggedData.getParsedTags()) {
                    pstmt.setString(1, tagValue);
                    pstmt.setInt(2, height);
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        Tag tag = new Tag(tagValue, height);
                        tag.count += 1;
                        tagTable.insert(tag);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        private static void delete(Map<String,Integer> expiredTags) {
            try (Connection con = lookupDataSource().getConnection();
                 PreparedStatement pstmt = con.prepareStatement("UPDATE data_tag SET tag_count = tag_count - ? WHERE tag = ?");
                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM data_tag WHERE tag_count <= 0")) {
                for (Map.Entry<String,Integer> entry : expiredTags.entrySet()) {
                    pstmt.setInt(1, entry.getValue());
                    pstmt.setString(2, entry.getKey());
                    pstmt.executeUpdate();
                    LOG.debug("Reduced tag count for " + entry.getKey() + " by " + entry.getValue());
                }
                int deleted = pstmtDelete.executeUpdate();
                if (deleted > 0) {
                    LOG.debug("Deleted " + deleted + " tags");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        private final String tag;
        private final DbKey dbKey;
        private final int height;
        private int count;

        private Tag(String tag, int height) {
            this.tag = tag;
            this.dbKey = tagDbKeyFactory.newKey(this.tag);
            this.height = height;
        }

        private Tag(ResultSet rs, DbKey dbKey) throws SQLException {
            this.tag = rs.getString("tag");
            this.dbKey = dbKey;
            this.count = rs.getInt("tag_count");
            this.height = rs.getInt("height");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO data_tag (tag, tag_count, height, latest) "
                    + "KEY (tag, height) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setString(++i, this.tag);
                pstmt.setInt(++i, this.count);
                pstmt.setInt(++i, this.height);
                pstmt.executeUpdate();
            }
        }

        public String getTag() {
            return tag;
        }

        public int getCount() {
            return count;
        }

    }
*/

/*
    private static final LongKeyFactory<Long> extendDbKeyFactory = new LongKeyFactory<Long>("id") {
        @Override
        public DbKey newKey(Long taggedDataId) {
            return newKey(taggedDataId.longValue());
        }
    };

    private static final VersionedValuesDbTable<Long> extendTable = new VersionedValuesDbTable<Long>("tagged_data_extend", extendDbKeyFactory) {
        @Override
        public Long load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return rs.getLong("extend_id");
        }

        // TODO: YL WARNING code will be refactored and changed
        @Override
        public void save(Connection con, Long taggedDataId, Long extendId) throws SQLException {
            LOG.warn("TODO: YL WARNING code will be refactored and changed !!");
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO tagged_data_extend (id, extend_id, "
                    + "height, latest) VALUES (?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, taggedDataId);
                pstmt.setLong(++i, extendId);
//                // TODO: YL can it be replace by 'sql join' here ???
                pstmt.setInt(++i, blockchain.getHeight());
                pstmt.executeUpdate();
            }
        }
    };

    public static List<Long> getExtendTransactionIds(long taggedDataId) {
        return extendTable.get(extendDbKeyFactory.newKey(taggedDataId));
    }
*/

/*
    public static int getCount() {
        return taggedDataTable.getCount();
    }

    public static DbIterator<TaggedData> getAll(int from, int to) {
        return taggedDataTable.getAll(from, to);
    }

    public static TaggedData getData(long transactionId) {
        return taggedDataTable.get(taggedDataKeyFactory.newKey(transactionId));

    public static DbIterator<TaggedData> getData(String channel, long accountId, int from, int to) {
        if (channel == null && accountId == 0) {
            throw new IllegalArgumentException("Either channel, or accountId, or both, must be specified");
        }
        return taggedDataTable.getManyBy(getDbClause(channel, accountId), from, to);
    }

    public static DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to) {
        return taggedDataTable.search(query, getDbClause(channel, accountId), from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }

    private static DbClause getDbClause(String channel, long accountId) {
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        if (channel != null) {
            dbClause = new DbClause.StringClause("channel", channel);
        }
        if (accountId != 0) {
            DbClause accountClause = new DbClause.LongClause("account_id", accountId);
            dbClause = dbClause != DbClause.EMPTY_CLAUSE ? dbClause.and(accountClause) : accountClause;
        }
        return dbClause;
    }
*/

/*
    static void init() {
        DataTag.init();
    }
*/

    private final long id;
    private DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final String tags;
    private final String[] parsedTags;
    private final byte[] data;
    private final String type;
    private final String channel;
    private final boolean isText;
    private final String filename;
    private int transactionTimestamp;
    private int blockTimestamp;
    private int height;

    public TaggedData(Transaction transaction, TaggedDataAttachment attachment, int blockTimestamp, int height) {
        this.id = transaction.getId();
//        this.dbKey = taggedDataKeyFactory.newKey(this.id);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.tags = attachment.getTags();
        this.parsedTags = Search.parseTags(tags, 3, 20, 5);
        this.data = attachment.getData();
        this.type = attachment.getType();
        this.channel = attachment.getChannel();
        this.isText = attachment.isText();
        this.filename = attachment.getFilename();
        this.blockTimestamp = blockTimestamp;
        this.transactionTimestamp = transaction.getTimestamp();
        this.height = height;
    }

    public TaggedData(ResultSet rs, DbKey dbKey) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKey;
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tags = rs.getString("tags");
        this.parsedTags = DbUtils.getArray(rs, "parsed_tags", String[].class);
        this.data = rs.getBytes("data");
        this.type = rs.getString("type");
        this.channel = rs.getString("channel");
        this.isText = rs.getBoolean("is_text");
        this.filename = rs.getString("filename");
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.transactionTimestamp = rs.getInt("transaction_timestamp");
        this.height = rs.getInt("height");
    }

    public TaggedData(long id, DbKey dbKey, long accountId, String name, String description, String tags, String[] parsedTags, byte[] data, String type, String channel, boolean isText, String filename) {
        this.id = id;
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.parsedTags = parsedTags;
        this.data = data;
        this.type = type;
        this.channel = channel;
        this.isText = isText;
        this.filename = filename;
    }

/*
    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO tagged_data (id, account_id, name, description, tags, parsed_tags, "
                + "type, channel, data, is_text, filename, block_timestamp, transaction_timestamp, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setString(++i, this.tags);
            DbUtils.setArray(pstmt, ++i, this.parsedTags);
            pstmt.setString(++i, this.type);
            pstmt.setString(++i, this.channel);
            pstmt.setBytes(++i, this.data);
            pstmt.setBoolean(++i, this.isText);
            pstmt.setString(++i, this.filename);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.setInt(++i, this.transactionTimestamp);
            pstmt.setInt(++i, height);
            pstmt.executeUpdate();
        }
    }
*/

    public long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String[] getParsedTags() {
        return parsedTags;
    }

    public byte[] getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public boolean isText() {
        return isText;
    }

    public String getFilename() {
        return filename;
    }

    public int getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public DbKey getDbKey() {
        return dbKey;
    }

    public void setDbKey(DbKey dbKey) {
        this.dbKey = dbKey;
    }

    public void setTransactionTimestamp(int transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public void setBlockTimestamp(int blockTimestamp) {
        this.blockTimestamp = blockTimestamp;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

/*
    public static void add(TransactionImpl transaction, TaggedDataUploadAttachment attachment) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            TaggedData taggedData = taggedDataTable.get(dbKey);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchain.getHeight());
                taggedDataTable.insert(taggedData);
//                DataTag.add(taggedData); // TODO: YL review and change
            }
        }
        TaggedDataTimestamp timestamp = new TaggedDataTimestamp(transaction.getId(), transaction.getTimestamp());
        timestampTable.insert(timestamp);
    }
*/

/*    public static void extend(Transaction transaction, TaggedDataExtendAttachment attachment) {
        long taggedDataId = attachment.getTaggedDataId();
        DbKey dbKey = taggedDataKeyFactory.newKey(taggedDataId);
        TaggedDataTimestamp timestamp = timestampTable.get(dbKey);
        if (transaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp.getTimestamp()) {
            timestamp.setTimestamp(transaction.getTimestamp() );
        } else {
            timestamp.setTimestamp(timestamp.getTimestamp()
                    + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp.getTimestamp()));
        }
        timestampTable.insert(timestamp);
        // TODO: YL review
//        List<Long> extendTransactionIds = extendTable.get(dbKey);
//        extendTransactionIds.add(transaction.getId());
//        extendTable.insert(taggedDataId, extendTransactionIds);
        if (timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() < timestamp.getTimestamp()) {
            TaggedData taggedData = taggedDataTable.get(dbKey);
            if (taggedData == null && attachment.getData() != null) {
                Transaction uploadTransaction = blockchain.getTransaction(taggedDataId);
                taggedData = new TaggedData(uploadTransaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchain.getHeight());
//                DataTag.add(taggedData); // TODO: YL review and change
            }
            if (taggedData != null) {
                taggedData.transactionTimestamp = timestamp.getTimestamp();
                taggedData.blockTimestamp = blockchain.getLastBlockTimestamp();
                taggedData.height = blockchain.getHeight();
                taggedDataTable.insert(taggedData);
            }
        }
    }*/

/*
    public static void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height) {
        TaggedData taggedData = new TaggedData(transaction, attachment, blockTimestamp, height);
        taggedDataTable.insert(taggedData);
//        DataTag.add(taggedData, height); // TODO: YL review and change
        int timestamp = transaction.getTimestamp();
        for (long extendTransactionId : TaggedData.getExtendTransactionIds(transaction.getId())) {
            Transaction extendTransaction = blockchain.getTransaction(extendTransactionId);
            if (extendTransaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp) {
                timestamp = extendTransaction.getTimestamp();
            } else {
                timestamp = timestamp + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp);
            }
            taggedData.transactionTimestamp = timestamp;
            taggedData.blockTimestamp = extendTransaction.getBlockTimestamp();
            taggedData.height = extendTransaction.getHeight();
            taggedDataTable.insert(taggedData);
        }
    }

    public static boolean isPruned(long transactionId) {
        try (Connection con = lookupDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM tagged_data WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
*/

}

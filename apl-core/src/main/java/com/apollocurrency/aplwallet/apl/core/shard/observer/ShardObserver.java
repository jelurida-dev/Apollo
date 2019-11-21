/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shard.observer;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.Async;
import com.apollocurrency.aplwallet.apl.core.app.observer.events.Sync;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import com.apollocurrency.aplwallet.apl.util.ModWatcher;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Singleton
public class ShardObserver {
    private static final Logger log = LoggerFactory.getLogger(ShardObserver.class);

    private final BlockchainConfig blockchainConfig;
    private final ShardService shardService;
    private PropertiesHolder propertiesHolder;

    @Inject
    public ShardObserver(BlockchainConfig blockchainConfig, ShardService shardService, PropertiesHolder propertiesHolder) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig, "blockchainConfig is NULL");
        this.shardService = Objects.requireNonNull(shardService, "shardService is NULL");
        this.propertiesHolder = Objects.requireNonNull(propertiesHolder, "propertiesHolder is NULL");
    }

//no matter how we get signal sync or async, do it async
    public void onTrimDoneAsync(@Observes @Async TrimData trimData) {
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());        
    }

    public void onTrimDone(@Observes @Sync TrimData trimData) {
//do it async anyway because we have to exit from trim and unlock it       
        tryCreateShardAsync(trimData.getTrimHeight(), trimData.getBlockchainHeight());
    }
    private boolean isShardingEnabled(HeightConfig currentConfig){
        boolean isShardingOff = propertiesHolder.getBooleanProperty("apl.noshardcreate", false);
        boolean shardingEnabled = currentConfig.isShardingEnabled();
        log.debug("Is sharding enabled ? : '{}' && '{}'", shardingEnabled, !isShardingOff);
        return shardingEnabled && !isShardingOff;
    }
    
    private boolean isShardAlreadyCreated(){
        //TODO: check
        return false;
    }
    
    private boolean isTimeForShard(int lastTrimBlockHeight, int blockchainHeight, HeightConfig currentConfig) {
        int shardingFrequency = 1;
        if (!blockchainConfig.isJustUpdated()) {
            // config didn't change from previous trim scheduling
            shardingFrequency = currentConfig.getShardingFrequency();
        } else {
            // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated()' usage and checking
            // config has changed from previous trim scheduling, try to get previous 'shard frequency' value
            shardingFrequency = blockchainConfig.getPreviousConfig().isPresent()
                    && blockchainConfig.getPreviousConfig().get().isShardingEnabled()
                    ? blockchainConfig.getPreviousConfig().get().getShardingFrequency() // previous config
                    : currentConfig.getShardingFrequency(); // fall back
        }
        log.debug("Check shard conditions: ? [{}],  lastTrimBlockHeight = {}, blockchainHeight = {}"
                + ", shardingFrequency = {} ({})",
                shardingFrequency != 0
                        ? lastTrimBlockHeight % shardingFrequency == 0 : "zeroDivision",
                lastTrimBlockHeight, blockchainHeight,
                shardingFrequency, blockchainConfig.isJustUpdated());
        
        //Q. how much blocks we ould be late? 1/4 of frequiency is OK?
        //Q. Do we count on some other parameters?
        long howLateWeCanBe = shardingFrequency / 4;
        ModWatcher watch = new ModWatcher(shardingFrequency, howLateWeCanBe);
        boolean res = lastTrimBlockHeight != 0 && !watch.isTooLate(lastTrimBlockHeight);
        res = res && !isShardAlreadyCreated();
        return res;
    }
    
    public CompletableFuture<MigrateState> tryCreateShardAsync(int lastTrimBlockHeight, int blockchainHeight) {
        CompletableFuture<MigrateState> completableFuture = null;
        HeightConfig currentConfig = blockchainConfig.getCurrentConfig();
        
        if (isShardingEnabled(currentConfig)) {

            if (isTimeForShard(lastTrimBlockHeight, blockchainHeight, currentConfig)) {
                completableFuture = shardService.tryCreateShardAsync(lastTrimBlockHeight, blockchainHeight);
            } else {
                log.debug("No attempt to create new shard at height '{}' (because lastTrimHeight={}), ({})",
                        blockchainHeight, lastTrimBlockHeight, blockchainConfig.isJustUpdated());
            }
            // TODO: YL after separating 'shard' and 'trim' logic, we can remove 'isJustUpdated() + resetJustUpdated()' usage
            if (blockchainConfig.isJustUpdated()) {
                blockchainConfig.resetJustUpdated(); // reset flag
            }
        }else{
            log.debug("Sharding is disabled by config");
        }
        return completableFuture;
    }
}

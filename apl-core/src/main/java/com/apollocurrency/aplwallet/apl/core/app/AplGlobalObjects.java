/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

//TODO: This class is mistake in DNA and must be removed completelly

/**
 * This class required for global accessed objects initialization without static initialization in Apl class
 *
 */
@ApplicationScoped
public class AplGlobalObjects {
    private static final Logger LOG = getLogger(AplGlobalObjects.class);
    private static final Map<String, GlobalObject<?>> OBJECTS = new ConcurrentHashMap<>();
    private static final String DEFAULT_INIT_ERROR = "%s was not initialized before accessing";
    private static final String DEFAULT_CHAIN_CONFIG_NAME = "BlockchainConfig";
    private static final String DEFAULT_BLOCK_DB_NAME = "BlockDb";
    private static final String GET_EXEPTION_TEMPLATE = "Unable to get %s. %s is not an instance of %s";

    public AplGlobalObjects() { } // for weld


    private static<T> void save(String name, GlobalObject<T> object) {
        OBJECTS.put(name, object);
        LOG.info("Saved new {} object as instance of {}", name, object.getObj().getClass());
    }

    public static<T> void set(GlobalObject<T> globalObject) {
        if (globalObject == null || !globalObject.isValid()) {
            throw new IllegalArgumentException("Global object is not valid!");
        }
        save(globalObject.getName(), globalObject);
    }


    public static void createBlockchainConfig(Chain chain, PropertiesHolder loader, boolean doInit) {
        BlockchainConfig blockchainConfig = new BlockchainConfig(chain, loader);
        if (doInit) {
            blockchainConfig.init();
        }
        OBJECTS.put(DEFAULT_CHAIN_CONFIG_NAME, new GlobalObject<>(blockchainConfig, DEFAULT_CHAIN_CONFIG_NAME));
    }
    
    public static void createBlockchainConfig(Chain chain, PropertiesHolder loader) {
        createBlockchainConfig(chain, loader, true);
    }

    private static <T> T get(Class<T> clazz, String name) {
        GlobalObject o = OBJECTS.get(name);
        validateInitialization(o, name);
        Object realObject = o.getObj();
        if (clazz.isInstance(realObject)) {
            return clazz.cast(realObject);
        } else {
            throw new RuntimeException(String.format(GET_EXEPTION_TEMPLATE, name, realObject.getClass(), clazz));
        }
    }


    private static void validateInitialization(Object object, String component) {
        if (object == null) {
            throw new RuntimeException(String.format(DEFAULT_INIT_ERROR, component));
        }
    }

    public static BlockchainConfig getChainConfig() {
        return get(BlockchainConfig.class, DEFAULT_CHAIN_CONFIG_NAME);
    }
}

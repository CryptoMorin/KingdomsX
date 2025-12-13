package org.kingdoms.utils.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class AllKnowingCacheLoader<K, T> implements CacheLoader<K, T> {
    protected final boolean cacheExistence;
    private final Cache<K, Boolean> doesntExist;

    protected AllKnowingCacheLoader(boolean cacheExistence, Cache<K, Boolean> doesntExist) {
        this.cacheExistence = cacheExistence;
        this.doesntExist = doesntExist;
    }

    protected boolean shouldCacheExistence() {
        return cacheExistence;
    }

    protected Cache<K, Boolean> doesntExistCache() {
        return doesntExist;
    }
}
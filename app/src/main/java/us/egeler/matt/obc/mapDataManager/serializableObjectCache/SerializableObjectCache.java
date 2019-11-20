package us.egeler.matt.obc.mapDataManager.serializableObjectCache;

import java.util.HashMap;

public abstract class SerializableObjectCache<ID,ELEMENT> {
    // TODO support caching on disk -- all map elements are serializable
    // when caching on disk, make sure we use a folder specific to this instance of the class
    //
    // Also: ensure that multiple cache targets can be used at once

    public static final int CACHE_TARGET_DISK = 1;
    public static final int CACHE_TARGET_MEMORY = 2;
    // TODO maybe support "smart" cache that chooses between disk and memory based on data used and user-specified limits

    private int initializedCaches = 0;

    private HashMap<ID, ELEMENT> memoryCache = null;

    protected abstract ID getUniqueIdentifierForElement(ELEMENT e);

    public void initCaches(int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                throw new IllegalStateException("Attempting to initialize memory cache, but it is already initialized.");
            } else {
                memoryCache = new HashMap<>();
                initializedCaches = (initializedCaches | CACHE_TARGET_MEMORY);
            }
        }
    }

    public void destroyCaches(int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if (memoryCache != null) {
                memoryCache.clear();
            }

            memoryCache = null;
        }
    }

    // returns the ID of the element cached
    // null if nothing cached
    public ID cacheElement(ELEMENT e, int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                return cacheElementMemory(e);
            } else {
                throw new IllegalStateException("Attempting to cache element in memory, but memory cache is not initialized.");
            }
        }

        return null;
    }

    public void removeElement(ELEMENT e, int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                removeElementMemory(e);
            } else {
                throw new IllegalStateException("Attempting to remove element from memory cache, but memory cache is not initialized.");
            }
        }
    }

    // returns true if element exists within ANY cache target, otherwise false
    public boolean elementExists(ID elementId, int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                return elementExistsMemory(elementId);
            }
        } else {
            throw new IllegalStateException("Attempting to remove element from memory cache, but memory cache is not initialized.");
        }

        return false;
    }

    // if multiple cacheTargets are specified, will return the first one found checking in the following order: memory, disk
    // returns null if nothing found
    public ELEMENT getElement(ID elementId, int cacheTargets) {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                ELEMENT r = getElementMemory(elementId);
                if (r != null) {
                    return r;
                }
            } else {
                throw new IllegalStateException("Attempting to remove element from memory cache, but memory cache is not initialized.");
            }
        }

        return null;
    }

    private ID cacheElementMemory(ELEMENT e) {
        ID id = getUniqueIdentifierForElement(e);

        memoryCache.put(id, e);
        return id;
    }

    private void removeElementMemory(ELEMENT e) {
        memoryCache.remove(getUniqueIdentifierForElement(e));
    }

    private ELEMENT getElementMemory(ID elementId) {
        return memoryCache.get(elementId);
    }

    private boolean elementExistsMemory(ID elementId) {
        return memoryCache.containsKey(elementId);
    }
}

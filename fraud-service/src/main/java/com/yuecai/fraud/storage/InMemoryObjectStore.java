package com.yuecai.fraud.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Tests and single-process local runs. Nothing survives a restart. */
public class InMemoryObjectStore implements ObjectStore {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] content, String contentType) {
        objects.put(key, content.clone());
    }

    @Override
    public byte[] get(String key) {
        byte[] content = objects.get(key);
        if (content == null) {
            throw new ObjectNotFoundException(key);
        }
        return content.clone();
    }

    @Override
    public boolean exists(String key) {
        return objects.containsKey(key);
    }

    @Override
    public Optional<URI> presignedGetUrl(String key, Duration ttl) {
        return Optional.empty();
    }
}

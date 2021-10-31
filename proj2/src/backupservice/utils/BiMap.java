package backupservice.utils;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class BiMap<K, V> implements Serializable {

    ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    ConcurrentHashMap<V, K> inverseMap = new ConcurrentHashMap<>();

    public void put(K k, V v) {
        map.put(k, v);
        inverseMap.put(v, k);
    }

    public V get(K k) {
        return map.get(k);
    }

    public K getKey(V v) {
        return inverseMap.get(v);
    }

    public void remove(K k) {
        V v = map.remove(k);
        inverseMap.remove(v);
    }
}

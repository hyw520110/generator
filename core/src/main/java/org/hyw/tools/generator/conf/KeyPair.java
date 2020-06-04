package org.hyw.tools.generator.conf;

public class KeyPair<K, V> {
    private K key;
    private V value;

    public KeyPair() {
    }

    public KeyPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V val) {
        this.value = val;
    }
}
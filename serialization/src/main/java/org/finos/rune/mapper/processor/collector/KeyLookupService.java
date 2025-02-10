package org.finos.rune.mapper.processor.collector;

import org.finos.rune.mapper.processor.KeyRecord;

import java.util.Map;
import java.util.Objects;

public class KeyLookupService {
    private final Map<KeyRecord, Object> globalKeyToValueObjectMap;
    private final Map<KeyRecord, Object> externalKeyToValueObjectMap;
    private final Map<KeyRecord, Object> addressToValueObjectMap;

    public KeyLookupService(Map<KeyRecord, Object> globalKeyToValueObjectMap,
                            Map<KeyRecord, Object> externalKeyToValueObjectMap,
                            Map<KeyRecord, Object> addressToValueObjectMap) {
        this.globalKeyToValueObjectMap = globalKeyToValueObjectMap;
        this.externalKeyToValueObjectMap = externalKeyToValueObjectMap;
        this.addressToValueObjectMap = addressToValueObjectMap;
    }

    public Object getReferencedObject(KeyType keyType, Class<?> keyOnType, String keyReferenceValue) {
        switch (keyType) {
            case GLOBAL_KEY:
                return globalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            case EXTERNAL_KEY:
                return externalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            case ADDRESS:
                return addressToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            default:
                throw new IllegalArgumentException("Unknown key type: " + keyType);
        }
    }

    public boolean higherPrecedenceKeyExists(KeyType keyType, Class<?> keyOnType, String keyReferenceValue) {
        switch (keyType) {
            case GLOBAL_KEY:
                Object globalKeyObject = globalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
                return Objects.equals(globalKeyObject, addressToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue))) ||
                        Objects.equals(globalKeyObject, externalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue)));
            case EXTERNAL_KEY:
                Object externalKeyObject = externalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
                return Objects.equals(externalKeyObject, addressToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue)));
            case ADDRESS:
                Object addressObject = addressToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
                return Objects.nonNull(addressObject);
            default:
                throw new IllegalArgumentException("Unknown key type: " + keyType);
        }
    }

    public enum KeyType {
        GLOBAL_KEY, EXTERNAL_KEY, ADDRESS
    }
}

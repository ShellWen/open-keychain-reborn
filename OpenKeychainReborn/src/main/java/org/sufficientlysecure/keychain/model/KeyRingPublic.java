package org.sufficientlysecure.keychain.model;


import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeyRingsPublicModel;


@AutoValue
public abstract class KeyRingPublic implements KeyRingsPublicModel {
    @SuppressWarnings("mutable")
    @Nullable
    public abstract byte[] key_ring_data();

    public static final Factory<KeyRingPublic> FACTORY = new Factory<>(AutoValue_KeyRingPublic::new);

    public static final Mapper<KeyRingPublic> MAPPER = new Mapper<>(FACTORY);

    public static KeyRingPublic create(long masterKeyId, byte[] keyRingData) {
        return new AutoValue_KeyRingPublic(masterKeyId, keyRingData);
    }

    public static InsertKeyRingPublic createInsertStatement(SupportSQLiteDatabase db) {
        return new InsertKeyRingPublic(db);
    }

    public void bindTo(InsertKeyRingPublic statement) {
        statement.bind(master_key_id(), key_ring_data());
    }
}

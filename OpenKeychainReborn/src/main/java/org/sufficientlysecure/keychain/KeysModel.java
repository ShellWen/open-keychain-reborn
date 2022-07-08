package org.sufficientlysecure.keychain;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import com.squareup.sqldelight.prerelease.internal.QuestionMarks;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

public interface KeysModel {
  @Deprecated
  String VALIDMASTERKEYS_VIEW_NAME = "validMasterKeys";

  @Deprecated
  String VALIDKEYS_VIEW_NAME = "validKeys";

  @Deprecated
  String UNIFIEDKEYVIEW_VIEW_NAME = "unifiedKeyView";

  @Deprecated
  String TABLE_NAME = "keys";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String RANK = "rank";

  @Deprecated
  String KEY_ID = "key_id";

  @Deprecated
  String KEY_SIZE = "key_size";

  @Deprecated
  String KEY_CURVE_OID = "key_curve_oid";

  @Deprecated
  String ALGORITHM = "algorithm";

  @Deprecated
  String CAN_CERTIFY = "can_certify";

  @Deprecated
  String CAN_SIGN = "can_sign";

  @Deprecated
  String CAN_ENCRYPT = "can_encrypt";

  @Deprecated
  String CAN_AUTHENTICATE = "can_authenticate";

  @Deprecated
  String IS_REVOKED = "is_revoked";

  @Deprecated
  String HAS_SECRET = "has_secret";

  @Deprecated
  String IS_SECURE = "is_secure";

  @Deprecated
  String CREATION = "creation";

  @Deprecated
  String EXPIRY = "expiry";

  @Deprecated
  String VALIDFROM = "validFrom";

  @Deprecated
  String FINGERPRINT = "fingerprint";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS keys (\r\n"
      + "    master_key_id INTEGER NOT NULL,\r\n"
      + "    rank INTEGER NOT NULL,\r\n"
      + "    key_id INTEGER NOT NULL,\r\n"
      + "    key_size INTEGER,\r\n"
      + "    key_curve_oid TEXT,\r\n"
      + "    algorithm INTEGER NOT NULL,\r\n"
      + "    can_certify INTEGER NOT NULL,\r\n"
      + "    can_sign INTEGER NOT NULL,\r\n"
      + "    can_encrypt INTEGER NOT NULL,\r\n"
      + "    can_authenticate INTEGER NOT NULL,\r\n"
      + "    is_revoked INTEGER NOT NULL,\r\n"
      + "    has_secret INTEGER NOT NULL DEFAULT 0,\r\n"
      + "    is_secure INTEGER NOT NULL,\r\n"
      + "    creation INTEGER NOT NULL,\r\n"
      + "    expiry INTEGER,\r\n"
      + "    validFrom INTEGER NOT NULL,\r\n"
      + "    fingerprint BLOB NOT NULL,\r\n"
      + "    PRIMARY KEY(master_key_id, rank),\r\n"
      + "    FOREIGN KEY(master_key_id) REFERENCES\r\n"
      + "    keyrings_public(master_key_id) ON DELETE CASCADE\r\n"
      + ")";

  String VALIDMASTERKEYSVIEW = ""
      + "CREATE VIEW validMasterKeys AS\r\n"
      + "SELECT *\r\n"
      + "    FROM validKeys\r\n"
      + "    WHERE rank = 0";

  String VALIDKEYSVIEW = ""
      + "CREATE VIEW validKeys AS\r\n"
      + "SELECT master_key_id, rank, key_id, key_size, key_curve_oid, algorithm, can_certify, can_sign, can_encrypt, can_authenticate, is_revoked, has_secret, is_secure, creation, expiry, fingerprint\r\n"
      + "    FROM keys\r\n"
      + "    WHERE is_revoked = 0 AND is_secure = 1 AND (expiry IS NULL OR expiry >= strftime('%s', 'now')) AND validFrom <= strftime('%s', 'now')";

  String UNIFIEDKEYVIEW = ""
      + "CREATE VIEW unifiedKeyView AS\r\n"
      + "    SELECT keys.master_key_id, MIN(user_packets.rank), user_packets.user_id, user_packets.name, user_packets.email, user_packets.comment, keys.creation, keys.expiry, keys.is_revoked, keys.is_secure, keys.can_certify, certs.verified,\r\n"
      + "        (EXISTS (SELECT * FROM user_packets AS dups WHERE dups.master_key_id != keys.master_key_id AND dups.rank = 0 AND dups.name = user_packets.name COLLATE NOCASE AND dups.email = user_packets.email COLLATE NOCASE )) AS has_duplicate_int,\r\n"
      + "        (EXISTS (SELECT * FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.has_secret != 0 )) AS has_any_secret_int,\r\n"
      + "        (SELECT key_id FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_encrypt != 0 LIMIT 1) AS has_encrypt_key_int,\r\n"
      + "        (SELECT key_id FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_sign != 0 LIMIT 1) AS has_sign_key_int,\r\n"
      + "        (SELECT key_id FROM keys AS k WHERE k.master_key_id = keys.master_key_id AND k.can_authenticate != 0 LIMIT 1) AS has_auth_key_int,\r\n"
      + "        GROUP_CONCAT(DISTINCT aTI.package_name) AS autocrypt_package_names_csv,\r\n"
      + "        GROUP_CONCAT(user_packets.user_id, '|||') AS user_id_list,\r\n"
      + "        keys.fingerprint\r\n"
      + "    FROM keys\r\n"
      + "         INNER JOIN user_packets ON ( keys.master_key_id = user_packets.master_key_id AND user_packets.type IS NULL AND (user_packets.rank = 0 OR user_packets.is_revoked = 0))\r\n"
      + "         LEFT JOIN certs ON ( keys.master_key_id = certs.master_key_id AND certs.verified = 1 )\r\n"
      + "         LEFT JOIN autocrypt_peers AS aTI ON ( aTI.master_key_id = keys.master_key_id )\r\n"
      + "    WHERE keys.rank = 0\r\n"
      + "    GROUP BY keys.master_key_id";

  long master_key_id();

  long rank();

  long key_id();

  @Nullable
  Integer key_size();

  @Nullable
  String key_curve_oid();

  int algorithm();

  boolean can_certify();

  boolean can_sign();

  boolean can_encrypt();

  boolean can_authenticate();

  boolean is_revoked();

  @NonNull
  CanonicalizedSecretKey.SecretKeyType has_secret();

  boolean is_secure();

  long creation();

  @Nullable
  Long expiry();

  long validFrom();

  @NonNull
  byte[] fingerprint();

  interface SelectSubkeysByMasterKeyIdModel {
    long master_key_id();

    long rank();

    long key_id();

    @Nullable
    Integer key_size();

    @Nullable
    String key_curve_oid();

    int algorithm();

    boolean can_certify();

    boolean can_sign();

    boolean can_encrypt();

    boolean can_authenticate();

    boolean is_revoked();

    @NonNull
    CanonicalizedSecretKey.SecretKeyType has_secret();

    boolean is_secure();

    long creation();

    @Nullable
    Long expiry();

    long validFrom();

    @NonNull
    byte[] fingerprint();
  }

  interface SelectSubkeysByMasterKeyIdCreator<T extends SelectSubkeysByMasterKeyIdModel> {
    T create(long master_key_id, long rank, long key_id, @Nullable Integer key_size,
        @Nullable String key_curve_oid, int algorithm, boolean can_certify, boolean can_sign,
        boolean can_encrypt, boolean can_authenticate, boolean is_revoked,
        @NonNull CanonicalizedSecretKey.SecretKeyType has_secret, boolean is_secure, long creation,
        @Nullable Long expiry, long validFrom, @NonNull byte[] fingerprint);
  }

  final class SelectSubkeysByMasterKeyIdMapper<T extends SelectSubkeysByMasterKeyIdModel, T1 extends KeysModel> implements RowMapper<T> {
    private final SelectSubkeysByMasterKeyIdCreator<T> creator;

    private final Factory<T1> keysModelFactory;

    public SelectSubkeysByMasterKeyIdMapper(SelectSubkeysByMasterKeyIdCreator<T> creator,
        @NonNull Factory<T1> keysModelFactory) {
      this.creator = creator;
      this.keysModelFactory = keysModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getLong(2),
          cursor.isNull(3) ? null : cursor.getInt(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.getInt(5),
          cursor.getInt(6) == 1,
          cursor.getInt(7) == 1,
          cursor.getInt(8) == 1,
          cursor.getInt(9) == 1,
          cursor.getInt(10) == 1,
          keysModelFactory.has_secretAdapter.decode(cursor.getLong(11)),
          cursor.getInt(12) == 1,
          cursor.getLong(13),
          cursor.isNull(14) ? null : cursor.getLong(14),
          cursor.getLong(15),
          cursor.getBlob(16)
      );
    }
  }

  interface ValidMasterKeysModel<V1 extends ValidKeysModel> {
    @NonNull
    V1 validKeys();
  }

  interface ValidMasterKeysCreator<V1 extends ValidKeysModel, T extends ValidMasterKeysModel<V1>> {
    T create(@NonNull V1 validKeys);
  }

  interface ValidKeysModel {
    long master_key_id();

    long rank();

    long key_id();

    @Nullable
    Integer key_size();

    @Nullable
    String key_curve_oid();

    int algorithm();

    boolean can_certify();

    boolean can_sign();

    boolean can_encrypt();

    boolean can_authenticate();

    boolean is_revoked();

    @NonNull
    CanonicalizedSecretKey.SecretKeyType has_secret();

    boolean is_secure();

    long creation();

    @Nullable
    Long expiry();

    @NonNull
    byte[] fingerprint();
  }

  interface ValidKeysCreator<T extends ValidKeysModel> {
    T create(long master_key_id, long rank, long key_id, @Nullable Integer key_size,
        @Nullable String key_curve_oid, int algorithm, boolean can_certify, boolean can_sign,
        boolean can_encrypt, boolean can_authenticate, boolean is_revoked,
        @NonNull CanonicalizedSecretKey.SecretKeyType has_secret, boolean is_secure, long creation,
        @Nullable Long expiry, @NonNull byte[] fingerprint);
  }

  interface UnifiedKeyViewModel {
    long master_key_id();

    long MIN_user_packets_rank();

    @Nullable
    String user_id();

    @Nullable
    String name();

    @Nullable
    String email();

    @Nullable
    String comment();

    long creation();

    @Nullable
    Long expiry();

    boolean is_revoked();

    boolean is_secure();

    boolean can_certify();

    @Nullable
    CanonicalizedKeyRing.VerificationStatus verified();

    long has_duplicate_int();

    long has_any_secret_int();

    long has_encrypt_key_int();

    long has_sign_key_int();

    long has_auth_key_int();

    @Nullable
    String autocrypt_package_names_csv();

    @Nullable
    String user_id_list();

    @NonNull
    byte[] fingerprint();
  }

  interface UnifiedKeyViewCreator<T extends UnifiedKeyViewModel> {
    T create(long master_key_id, long MIN_user_packets_rank, @Nullable String user_id,
        @Nullable String name, @Nullable String email, @Nullable String comment, long creation,
        @Nullable Long expiry, boolean is_revoked, boolean is_secure, boolean can_certify,
        @Nullable CanonicalizedKeyRing.VerificationStatus verified, long has_duplicate_int,
        long has_any_secret_int, long has_encrypt_key_int, long has_sign_key_int,
        long has_auth_key_int, @Nullable String autocrypt_package_names_csv,
        @Nullable String user_id_list, @NonNull byte[] fingerprint);
  }

  final class UnifiedKeyViewMapper<T extends UnifiedKeyViewModel, T1 extends CertsModel> implements RowMapper<T> {
    private final UnifiedKeyViewCreator<T> creator;

    private final CertsModel.Factory<T1> certsModelFactory;

    public UnifiedKeyViewMapper(UnifiedKeyViewCreator<T> creator,
        @NonNull CertsModel.Factory<T1> certsModelFactory) {
      this.creator = creator;
      this.certsModelFactory = certsModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.getLong(6),
          cursor.isNull(7) ? null : cursor.getLong(7),
          cursor.getInt(8) == 1,
          cursor.getInt(9) == 1,
          cursor.getInt(10) == 1,
          cursor.isNull(11) ? null : certsModelFactory.verifiedAdapter.decode(cursor.getLong(11)),
          cursor.getLong(12),
          cursor.getLong(13),
          cursor.getLong(14),
          cursor.getLong(15),
          cursor.getLong(16),
          cursor.isNull(17) ? null : cursor.getString(17),
          cursor.isNull(18) ? null : cursor.getString(18),
          cursor.getBlob(19)
      );
    }
  }

  interface Creator<T extends KeysModel> {
    T create(long master_key_id, long rank, long key_id, @Nullable Integer key_size,
        @Nullable String key_curve_oid, int algorithm, boolean can_certify, boolean can_sign,
        boolean can_encrypt, boolean can_authenticate, boolean is_revoked,
        @NonNull CanonicalizedSecretKey.SecretKeyType has_secret, boolean is_secure, long creation,
        @Nullable Long expiry, long validFrom, @NonNull byte[] fingerprint);
  }

  final class Mapper<T extends KeysModel> implements RowMapper<T> {
    private final Factory<T> keysModelFactory;

    public Mapper(@NonNull Factory<T> keysModelFactory) {
      this.keysModelFactory = keysModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return keysModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getLong(2),
          cursor.isNull(3) ? null : cursor.getInt(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.getInt(5),
          cursor.getInt(6) == 1,
          cursor.getInt(7) == 1,
          cursor.getInt(8) == 1,
          cursor.getInt(9) == 1,
          cursor.getInt(10) == 1,
          keysModelFactory.has_secretAdapter.decode(cursor.getLong(11)),
          cursor.getInt(12) == 1,
          cursor.getLong(13),
          cursor.isNull(14) ? null : cursor.getLong(14),
          cursor.getLong(15),
          cursor.getBlob(16)
      );
    }
  }

  final class Factory<T extends KeysModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<CanonicalizedSecretKey.SecretKeyType, Long> has_secretAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<CanonicalizedSecretKey.SecretKeyType, Long> has_secretAdapter) {
      this.creator = creator;
      this.has_secretAdapter = has_secretAdapter;
    }

    @NonNull
    public SqlDelightQuery selectAllUnifiedKeyInfo() {
      return new SqlDelightQuery(""
          + "SELECT * FROM unifiedKeyView\r\n"
          + "    ORDER BY has_any_secret_int DESC, IFNULL(name, email) COLLATE NOCASE ASC, creation DESC",
          new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));
    }

    @NonNull
    public SqlDelightQuery selectUnifiedKeyInfoByMasterKeyId(long master_key_id) {
      return new SelectUnifiedKeyInfoByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectUnifiedKeyInfoByMasterKeyIds(long[] master_key_id) {
      return new SelectUnifiedKeyInfoByMasterKeyIdsQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectUnifiedKeyInfoSearchMailAddress(@Nullable String email) {
      return new SelectUnifiedKeyInfoSearchMailAddressQuery(email);
    }

    @NonNull
    public SqlDelightQuery selectAllUnifiedKeyInfoWithSecret() {
      return new SqlDelightQuery(""
          + "SELECT * FROM unifiedKeyView\r\n"
          + "    WHERE has_any_secret_int = 1\r\n"
          + "   ORDER BY creation DESC",
          new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));
    }

    @NonNull
    public SqlDelightQuery selectAllUnifiedKeyInfoWithAuthKeySecret() {
      return new SqlDelightQuery(""
          + "SELECT * FROM unifiedKeyView\r\n"
          + "    WHERE has_any_secret_int = 1 AND has_auth_key_int IS NOT NULL\r\n"
          + "   ORDER BY creation DESC",
          new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));
    }

    @NonNull
    public SqlDelightQuery selectMasterKeyIdBySubkey(long key_id) {
      return new SelectMasterKeyIdBySubkeyQuery(key_id);
    }

    @NonNull
    public SqlDelightQuery selectSubkeysByMasterKeyId(long master_key_id) {
      return new SelectSubkeysByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectSecretKeyType(long key_id) {
      return new SelectSecretKeyTypeQuery(key_id);
    }

    @NonNull
    public SqlDelightQuery selectFingerprintByKeyId(long key_id) {
      return new SelectFingerprintByKeyIdQuery(key_id);
    }

    @NonNull
    public SqlDelightQuery selectEffectiveEncryptionKeyIdsByMasterKeyId(long master_key_id) {
      return new SelectEffectiveEncryptionKeyIdsByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectEffectiveSignKeyIdByMasterKeyId(long master_key_id) {
      return new SelectEffectiveSignKeyIdByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectEffectiveAuthKeyIdByMasterKeyId(long master_key_id) {
      return new SelectEffectiveAuthKeyIdByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectAllUnifiedKeyInfoMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectUnifiedKeyInfoByMasterKeyIdMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectUnifiedKeyInfoByMasterKeyIdsMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectUnifiedKeyInfoSearchMailAddressMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectAllUnifiedKeyInfoWithSecretMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    @NonNull
    public <R extends UnifiedKeyViewModel, T1 extends CertsModel> UnifiedKeyViewMapper<R, T1> selectAllUnifiedKeyInfoWithAuthKeySecretMapper(
        UnifiedKeyViewCreator<R> creator, CertsModel.Factory<T1> certsModelFactory) {
      return new UnifiedKeyViewMapper<R, T1>(creator, certsModelFactory);
    }

    public RowMapper<Long> selectMasterKeyIdBySubkeyMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    @NonNull
    public <R extends SelectSubkeysByMasterKeyIdModel> SelectSubkeysByMasterKeyIdMapper<R, T> selectSubkeysByMasterKeyIdMapper(
        SelectSubkeysByMasterKeyIdCreator<R> creator) {
      return new SelectSubkeysByMasterKeyIdMapper<R, T>(creator, this);
    }

    public RowMapper<CanonicalizedSecretKey.SecretKeyType> selectSecretKeyTypeMapper() {
      return new RowMapper<CanonicalizedSecretKey.SecretKeyType>() {
        @Override
        public CanonicalizedSecretKey.SecretKeyType map(Cursor cursor) {
          return has_secretAdapter.decode(cursor.getLong(0));
        }
      };
    }

    public RowMapper<byte[]> selectFingerprintByKeyIdMapper() {
      return new RowMapper<byte[]>() {
        @Override
        public byte[] map(Cursor cursor) {
          return cursor.getBlob(0);
        }
      };
    }

    public RowMapper<Long> selectEffectiveEncryptionKeyIdsByMasterKeyIdMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> selectEffectiveSignKeyIdByMasterKeyIdMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> selectEffectiveAuthKeyIdByMasterKeyIdMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    private final class SelectUnifiedKeyInfoByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectUnifiedKeyInfoByMasterKeyIdQuery(long master_key_id) {
        super("SELECT * FROM unifiedKeyView\r\n"
            + "   WHERE master_key_id = ?1",
            new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }

    private final class SelectUnifiedKeyInfoByMasterKeyIdsQuery extends SqlDelightQuery {
      private final long[] master_key_id;

      SelectUnifiedKeyInfoByMasterKeyIdsQuery(long[] master_key_id) {
        super("SELECT * FROM unifiedKeyView\r\n"
            + "   WHERE master_key_id IN " + QuestionMarks.ofSize(master_key_id.length),
            new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        for (long item : master_key_id) {
          program.bindLong(nextIndex++, item);
        }
      }
    }

    private final class SelectUnifiedKeyInfoSearchMailAddressQuery extends SqlDelightQuery {
      @Nullable
      private final String email;

      SelectUnifiedKeyInfoSearchMailAddressQuery(@Nullable String email) {
        super("SELECT * FROM unifiedKeyView\r\n"
            + "   WHERE email LIKE ?1\r\n"
            + "   ORDER BY creation DESC",
            new TableSet("keys", "user_packets", "certs", "autocrypt_peers"));

        this.email = email;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        String email = this.email;
        if (email != null) {
          program.bindString(1, email);
        } else {
          program.bindNull(1);
        }
      }
    }

    private final class SelectMasterKeyIdBySubkeyQuery extends SqlDelightQuery {
      private final long key_id;

      SelectMasterKeyIdBySubkeyQuery(long key_id) {
        super("SELECT master_key_id\r\n"
            + "    FROM keys\r\n"
            + "    WHERE key_id = ?1",
            new TableSet("keys"));

        this.key_id = key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, key_id);
      }
    }

    private final class SelectSubkeysByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectSubkeysByMasterKeyIdQuery(long master_key_id) {
        super("SELECT master_key_id, rank, key_id, key_size, key_curve_oid, algorithm, can_certify, can_sign, can_encrypt, can_authenticate, is_revoked, has_secret, is_secure, creation, expiry, validFrom, fingerprint\r\n"
            + "    FROM keys\r\n"
            + "    WHERE master_key_id = ?1\r\n"
            + "    ORDER BY rank ASC",
            new TableSet("keys"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }

    private final class SelectSecretKeyTypeQuery extends SqlDelightQuery {
      private final long key_id;

      SelectSecretKeyTypeQuery(long key_id) {
        super("SELECT has_secret\r\n"
            + "    FROM keys\r\n"
            + "    WHERE key_id = ?1",
            new TableSet("keys"));

        this.key_id = key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, key_id);
      }
    }

    private final class SelectFingerprintByKeyIdQuery extends SqlDelightQuery {
      private final long key_id;

      SelectFingerprintByKeyIdQuery(long key_id) {
        super("SELECT fingerprint\r\n"
            + "    FROM keys\r\n"
            + "    WHERE key_id = ?1",
            new TableSet("keys"));

        this.key_id = key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, key_id);
      }
    }

    private final class SelectEffectiveEncryptionKeyIdsByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectEffectiveEncryptionKeyIdsByMasterKeyIdQuery(long master_key_id) {
        super("SELECT key_id\r\n"
            + "    FROM validKeys\r\n"
            + "    WHERE can_encrypt = 1 AND master_key_id = ?1",
            new TableSet("keys"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }

    private final class SelectEffectiveSignKeyIdByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectEffectiveSignKeyIdByMasterKeyIdQuery(long master_key_id) {
        super("SELECT key_id\r\n"
            + "    FROM validKeys\r\n"
            + "    WHERE has_secret > 1 AND can_sign = 1 AND master_key_id = ?1",
            new TableSet("keys"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }

    private final class SelectEffectiveAuthKeyIdByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectEffectiveAuthKeyIdByMasterKeyIdQuery(long master_key_id) {
        super("SELECT key_id\r\n"
            + "    FROM validKeys\r\n"
            + "    WHERE can_authenticate = 1 AND master_key_id = ?1\r\n"
            + "    ORDER BY has_secret > 1 DESC, creation DESC\r\n"
            + "    LIMIT 1",
            new TableSet("keys"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }
  }

  final class InsertKey extends SqlDelightStatement {
    private final Factory<? extends KeysModel> keysModelFactory;

    public InsertKey(@NonNull SupportSQLiteDatabase database,
        Factory<? extends KeysModel> keysModelFactory) {
      super("keys", database.compileStatement(""
              + "INSERT INTO keys (\r\n"
              + "        master_key_id, rank, key_id, key_size, key_curve_oid, algorithm, fingerprint,\r\n"
              + "        can_certify, can_sign, can_encrypt, can_authenticate,\r\n"
              + "        is_revoked, has_secret, is_secure, creation, expiry, validFrom\r\n"
              + "    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
      this.keysModelFactory = keysModelFactory;
    }

    public void bind(long master_key_id, long rank, long key_id, @Nullable Integer key_size,
        @Nullable String key_curve_oid, int algorithm, @NonNull byte[] fingerprint,
        boolean can_certify, boolean can_sign, boolean can_encrypt, boolean can_authenticate,
        boolean is_revoked, @NonNull CanonicalizedSecretKey.SecretKeyType has_secret,
        boolean is_secure, long creation, @Nullable Long expiry, long validFrom) {
      bindLong(1, master_key_id);
      bindLong(2, rank);
      bindLong(3, key_id);
      if (key_size == null) {
        bindNull(4);
      } else {
        bindLong(4, key_size);
      }
      if (key_curve_oid == null) {
        bindNull(5);
      } else {
        bindString(5, key_curve_oid);
      }
      bindLong(6, algorithm);
      bindBlob(7, fingerprint);
      bindLong(8, can_certify ? 1 : 0);
      bindLong(9, can_sign ? 1 : 0);
      bindLong(10, can_encrypt ? 1 : 0);
      bindLong(11, can_authenticate ? 1 : 0);
      bindLong(12, is_revoked ? 1 : 0);
      bindLong(13, keysModelFactory.has_secretAdapter.encode(has_secret));
      bindLong(14, is_secure ? 1 : 0);
      bindLong(15, creation);
      if (expiry == null) {
        bindNull(16);
      } else {
        bindLong(16, expiry);
      }
      bindLong(17, validFrom);
    }
  }

  final class UpdateHasSecretByMasterKeyId extends SqlDelightStatement {
    private final Factory<? extends KeysModel> keysModelFactory;

    public UpdateHasSecretByMasterKeyId(@NonNull SupportSQLiteDatabase database,
        Factory<? extends KeysModel> keysModelFactory) {
      super("keys", database.compileStatement(""
              + "UPDATE keys\r\n"
              + "    SET has_secret = ?2\r\n"
              + "    WHERE master_key_id = ?1"));
      this.keysModelFactory = keysModelFactory;
    }

    public void bind(long master_key_id, @NonNull CanonicalizedSecretKey.SecretKeyType has_secret) {
      bindLong(1, master_key_id);
      bindLong(2, keysModelFactory.has_secretAdapter.encode(has_secret));
    }
  }

  final class UpdateHasSecretByKeyId extends SqlDelightStatement {
    private final Factory<? extends KeysModel> keysModelFactory;

    public UpdateHasSecretByKeyId(@NonNull SupportSQLiteDatabase database,
        Factory<? extends KeysModel> keysModelFactory) {
      super("keys", database.compileStatement(""
              + "UPDATE keys\r\n"
              + "    SET has_secret = ?2\r\n"
              + "    WHERE key_id = ?1"));
      this.keysModelFactory = keysModelFactory;
    }

    public void bind(long key_id, @NonNull CanonicalizedSecretKey.SecretKeyType has_secret) {
      bindLong(1, key_id);
      bindLong(2, keysModelFactory.has_secretAdapter.encode(has_secret));
    }
  }
}

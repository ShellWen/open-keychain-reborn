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
import java.lang.Boolean;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import org.sufficientlysecure.keychain.model.AutocryptPeer;

public interface AutocryptPeersModel {
  @Deprecated
  String TABLE_NAME = "autocrypt_peers";

  @Deprecated
  String PACKAGE_NAME = "package_name";

  @Deprecated
  String IDENTIFIER = "identifier";

  @Deprecated
  String LAST_SEEN = "last_seen";

  @Deprecated
  String LAST_SEEN_KEY = "last_seen_key";

  @Deprecated
  String IS_MUTUAL = "is_mutual";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String GOSSIP_MASTER_KEY_ID = "gossip_master_key_id";

  @Deprecated
  String GOSSIP_LAST_SEEN_KEY = "gossip_last_seen_key";

  @Deprecated
  String GOSSIP_ORIGIN = "gossip_origin";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS autocrypt_peers (\r\n"
      + "    package_name TEXT NOT NULL,\r\n"
      + "    identifier TEXT NOT NULL,\r\n"
      + "    last_seen INTEGER NULL,\r\n"
      + "    last_seen_key INTEGER NULL,\r\n"
      + "    is_mutual INTEGER NOT NULL DEFAULT 0,\r\n"
      + "    master_key_id INTEGER NULL,\r\n"
      + "    gossip_master_key_id INTEGER NULL,\r\n"
      + "    gossip_last_seen_key INTEGER NULL,\r\n"
      + "    gossip_origin INTEGER NULL,\r\n"
      + "    PRIMARY KEY(package_name, identifier),\r\n"
      + "    FOREIGN KEY(package_name) REFERENCES api_apps (package_name) ON DELETE CASCADE\r\n"
      + ")";

  @NonNull
  String package_name();

  @NonNull
  String identifier();

  @Nullable
  Date last_seen();

  @Nullable
  Date last_seen_key();

  boolean is_mutual();

  @Nullable
  Long master_key_id();

  @Nullable
  Long gossip_master_key_id();

  @Nullable
  Date gossip_last_seen_key();

  @Nullable
  AutocryptPeer.GossipOrigin gossip_origin();

  interface SelectAutocryptKeyStatusModel<T1 extends AutocryptPeersModel> {
    @NonNull
    T1 autocryptPeer();

    long key_is_expired_int();

    long gossip_key_is_expired_int();

    @Nullable
    Boolean key_is_revoked_int();

    @Nullable
    Boolean gossip_key_is_revoked_int();

    long key_is_verified_int();

    long gossip_key_is_verified_int();
  }

  interface SelectAutocryptKeyStatusCreator<T1 extends AutocryptPeersModel, T extends SelectAutocryptKeyStatusModel<T1>> {
    T create(@NonNull T1 autocryptPeer, long key_is_expired_int, long gossip_key_is_expired_int,
        @Nullable Boolean key_is_revoked_int, @Nullable Boolean gossip_key_is_revoked_int,
        long key_is_verified_int, long gossip_key_is_verified_int);
  }

  final class SelectAutocryptKeyStatusMapper<T1 extends AutocryptPeersModel, T extends SelectAutocryptKeyStatusModel<T1>> implements RowMapper<T> {
    private final SelectAutocryptKeyStatusCreator<T1, T> creator;

    private final Factory<T1> autocryptPeersModelFactory;

    public SelectAutocryptKeyStatusMapper(SelectAutocryptKeyStatusCreator<T1, T> creator,
        @NonNull Factory<T1> autocryptPeersModelFactory) {
      this.creator = creator;
      this.autocryptPeersModelFactory = autocryptPeersModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          autocryptPeersModelFactory.creator.create(
              cursor.getString(0),
              cursor.getString(1),
              cursor.isNull(2) ? null : autocryptPeersModelFactory.last_seenAdapter.decode(cursor.getLong(2)),
              cursor.isNull(3) ? null : autocryptPeersModelFactory.last_seen_keyAdapter.decode(cursor.getLong(3)),
              cursor.getInt(4) == 1,
              cursor.isNull(5) ? null : cursor.getLong(5),
              cursor.isNull(6) ? null : cursor.getLong(6),
              cursor.isNull(7) ? null : autocryptPeersModelFactory.gossip_last_seen_keyAdapter.decode(cursor.getLong(7)),
              cursor.isNull(8) ? null : autocryptPeersModelFactory.gossip_originAdapter.decode(cursor.getLong(8))
          ),
          cursor.getLong(9),
          cursor.getLong(10),
          cursor.isNull(11) ? null : cursor.getInt(11) == 1,
          cursor.isNull(12) ? null : cursor.getInt(12) == 1,
          cursor.getLong(13),
          cursor.getLong(14)
      );
    }
  }

  interface Creator<T extends AutocryptPeersModel> {
    T create(@NonNull String package_name, @NonNull String identifier, @Nullable Date last_seen,
        @Nullable Date last_seen_key, boolean is_mutual, @Nullable Long master_key_id,
        @Nullable Long gossip_master_key_id, @Nullable Date gossip_last_seen_key,
        @Nullable AutocryptPeer.GossipOrigin gossip_origin);
  }

  final class Mapper<T extends AutocryptPeersModel> implements RowMapper<T> {
    private final Factory<T> autocryptPeersModelFactory;

    public Mapper(@NonNull Factory<T> autocryptPeersModelFactory) {
      this.autocryptPeersModelFactory = autocryptPeersModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return autocryptPeersModelFactory.creator.create(
          cursor.getString(0),
          cursor.getString(1),
          cursor.isNull(2) ? null : autocryptPeersModelFactory.last_seenAdapter.decode(cursor.getLong(2)),
          cursor.isNull(3) ? null : autocryptPeersModelFactory.last_seen_keyAdapter.decode(cursor.getLong(3)),
          cursor.getInt(4) == 1,
          cursor.isNull(5) ? null : cursor.getLong(5),
          cursor.isNull(6) ? null : cursor.getLong(6),
          cursor.isNull(7) ? null : autocryptPeersModelFactory.gossip_last_seen_keyAdapter.decode(cursor.getLong(7)),
          cursor.isNull(8) ? null : autocryptPeersModelFactory.gossip_originAdapter.decode(cursor.getLong(8))
      );
    }
  }

  final class Factory<T extends AutocryptPeersModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Date, Long> last_seenAdapter;

    public final ColumnAdapter<Date, Long> last_seen_keyAdapter;

    public final ColumnAdapter<Date, Long> gossip_last_seen_keyAdapter;

    public final ColumnAdapter<AutocryptPeer.GossipOrigin, Long> gossip_originAdapter;

    public Factory(@NonNull Creator<T> creator, @NonNull ColumnAdapter<Date, Long> last_seenAdapter,
        @NonNull ColumnAdapter<Date, Long> last_seen_keyAdapter,
        @NonNull ColumnAdapter<Date, Long> gossip_last_seen_keyAdapter,
        @NonNull ColumnAdapter<AutocryptPeer.GossipOrigin, Long> gossip_originAdapter) {
      this.creator = creator;
      this.last_seenAdapter = last_seenAdapter;
      this.last_seen_keyAdapter = last_seen_keyAdapter;
      this.gossip_last_seen_keyAdapter = gossip_last_seen_keyAdapter;
      this.gossip_originAdapter = gossip_originAdapter;
    }

    @NonNull
    public SqlDelightQuery selectByIdentifiers(@NonNull String package_name,
        @NonNull String[] identifier) {
      return new SelectByIdentifiersQuery(package_name, identifier);
    }

    @NonNull
    public SqlDelightQuery selectByMasterKeyId(@Nullable Long master_key_id) {
      return new SelectByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectMasterKeyIdByIdentifier(@NonNull String identifier) {
      return new SelectMasterKeyIdByIdentifierQuery(identifier);
    }

    @NonNull
    public SqlDelightQuery selectAutocryptKeyStatus(@NonNull String package_name,
        @NonNull String[] identifier) {
      return new SelectAutocryptKeyStatusQuery(package_name, identifier);
    }

    @NonNull
    public Mapper<T> selectByIdentifiersMapper() {
      return new Mapper<T>(this);
    }

    @NonNull
    public Mapper<T> selectByMasterKeyIdMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<Long> selectMasterKeyIdByIdentifierMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }

    @NonNull
    public <R extends SelectAutocryptKeyStatusModel<T>> SelectAutocryptKeyStatusMapper<T, R> selectAutocryptKeyStatusMapper(
        SelectAutocryptKeyStatusCreator<T, R> creator) {
      return new SelectAutocryptKeyStatusMapper<T, R>(creator, this);
    }

    private final class SelectByIdentifiersQuery extends SqlDelightQuery {
      @NonNull
      private final String package_name;

      @NonNull
      private final String[] identifier;

      SelectByIdentifiersQuery(@NonNull String package_name, @NonNull String[] identifier) {
        super("SELECT *\r\n"
            + "    FROM  autocrypt_peers\r\n"
            + "    WHERE package_name = ?1 AND identifier IN " + QuestionMarks.ofSize(identifier.length),
            new TableSet("autocrypt_peers"));

        this.package_name = package_name;
        this.identifier = identifier;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, package_name);

        int nextIndex = 2;

        for (String item : identifier) {
          program.bindString(nextIndex++, item);
        }
      }
    }

    private final class SelectByMasterKeyIdQuery extends SqlDelightQuery {
      @Nullable
      private final Long master_key_id;

      SelectByMasterKeyIdQuery(@Nullable Long master_key_id) {
        super("SELECT *\r\n"
            + "    FROM  autocrypt_peers\r\n"
            + "    WHERE master_key_id = ?1",
            new TableSet("autocrypt_peers"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Long master_key_id = this.master_key_id;
        if (master_key_id != null) {
          program.bindLong(1, master_key_id);
        } else {
          program.bindNull(1);
        }
      }
    }

    private final class SelectMasterKeyIdByIdentifierQuery extends SqlDelightQuery {
      @NonNull
      private final String identifier;

      SelectMasterKeyIdByIdentifierQuery(@NonNull String identifier) {
        super("SELECT master_key_id\r\n"
            + "    FROM autocrypt_peers\r\n"
            + "    WHERE identifier = ?1",
            new TableSet("autocrypt_peers"));

        this.identifier = identifier;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, identifier);
      }
    }

    private final class SelectAutocryptKeyStatusQuery extends SqlDelightQuery {
      @NonNull
      private final String package_name;

      @NonNull
      private final String[] identifier;

      SelectAutocryptKeyStatusQuery(@NonNull String package_name, @NonNull String[] identifier) {
        super("SELECT autocryptPeer.*,\r\n"
            + "        (CASE WHEN ac_key.expiry IS NULL THEN 0 WHEN ac_key.expiry > strftime('%s', 'now') THEN 0 ELSE 1 END) AS key_is_expired_int,\r\n"
            + "        (CASE WHEN gossip_key.expiry IS NULL THEN 0 WHEN gossip_key.expiry > strftime('%s', 'now') THEN 0 ELSE 1 END) AS gossip_key_is_expired_int,\r\n"
            + "        ac_key.is_revoked AS key_is_revoked_int,\r\n"
            + "        gossip_key.is_revoked AS gossip_key_is_revoked_int,\r\n"
            + "        EXISTS (SELECT * FROM certs WHERE certs.master_key_id = autocryptPeer.master_key_id AND verified = 1 ) AS key_is_verified_int,\r\n"
            + "        EXISTS (SELECT * FROM certs WHERE certs.master_key_id = autocryptPeer.gossip_master_key_id AND verified = 1 ) AS gossip_key_is_verified_int\r\n"
            + "    FROM autocrypt_peers AS autocryptPeer\r\n"
            + "        LEFT JOIN keys AS ac_key ON (ac_key.master_key_id = autocryptPeer.master_key_id AND ac_key.rank = 0)\r\n"
            + "        LEFT JOIN keys AS gossip_key ON (gossip_key.master_key_id = gossip_master_key_id AND gossip_key.rank = 0)\r\n"
            + "    WHERE package_name = ?1 AND identifier IN " + QuestionMarks.ofSize(identifier.length),
            new TableSet("autocrypt_peers", "keys", "certs"));

        this.package_name = package_name;
        this.identifier = identifier;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, package_name);

        int nextIndex = 2;

        for (String item : identifier) {
          program.bindString(nextIndex++, item);
        }
      }
    }
  }

  final class DeleteByIdentifier extends SqlDelightStatement {
    public DeleteByIdentifier(@NonNull SupportSQLiteDatabase database) {
      super("autocrypt_peers", database.compileStatement(""
              + "DELETE FROM autocrypt_peers\r\n"
              + "    WHERE package_name = ? AND identifier = ?"));
    }

    public void bind(@NonNull String package_name, @NonNull String identifier) {
      bindString(1, package_name);
      bindString(2, identifier);
    }
  }

  final class DeleteByMasterKeyId extends SqlDelightStatement {
    public DeleteByMasterKeyId(@NonNull SupportSQLiteDatabase database) {
      super("autocrypt_peers", database.compileStatement(""
              + "DELETE FROM autocrypt_peers\r\n"
              + "    WHERE master_key_id = ?"));
    }

    public void bind(@Nullable Long master_key_id) {
      if (master_key_id == null) {
        bindNull(1);
      } else {
        bindLong(1, master_key_id);
      }
    }
  }

  final class UpdateLastSeen extends SqlDelightStatement {
    private final Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory;

    public UpdateLastSeen(@NonNull SupportSQLiteDatabase database,
        Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory) {
      super("autocrypt_peers", database.compileStatement(""
              + "UPDATE autocrypt_peers SET last_seen = ?3 WHERE package_name = ?1 AND identifier = ?2"));
      this.autocryptPeersModelFactory = autocryptPeersModelFactory;
    }

    public void bind(@NonNull String package_name, @NonNull String identifier,
        @Nullable Date last_seen) {
      bindString(1, package_name);
      bindString(2, identifier);
      if (last_seen == null) {
        bindNull(3);
      } else {
        bindLong(3, autocryptPeersModelFactory.last_seenAdapter.encode(last_seen));
      }
    }
  }

  final class UpdateKey extends SqlDelightStatement {
    private final Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory;

    public UpdateKey(@NonNull SupportSQLiteDatabase database,
        Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory) {
      super("autocrypt_peers", database.compileStatement(""
              + "UPDATE autocrypt_peers SET last_seen_key = ?3, master_key_id = ?4, is_mutual = ?5 WHERE package_name = ?1 AND identifier = ?2"));
      this.autocryptPeersModelFactory = autocryptPeersModelFactory;
    }

    public void bind(@NonNull String package_name, @NonNull String identifier,
        @Nullable Date last_seen_key, @Nullable Long master_key_id, boolean is_mutual) {
      bindString(1, package_name);
      bindString(2, identifier);
      if (last_seen_key == null) {
        bindNull(3);
      } else {
        bindLong(3, autocryptPeersModelFactory.last_seen_keyAdapter.encode(last_seen_key));
      }
      if (master_key_id == null) {
        bindNull(4);
      } else {
        bindLong(4, master_key_id);
      }
      bindLong(5, is_mutual ? 1 : 0);
    }
  }

  final class UpdateGossipKey extends SqlDelightStatement {
    private final Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory;

    public UpdateGossipKey(@NonNull SupportSQLiteDatabase database,
        Factory<? extends AutocryptPeersModel> autocryptPeersModelFactory) {
      super("autocrypt_peers", database.compileStatement(""
              + "UPDATE autocrypt_peers SET gossip_last_seen_key = ?3, gossip_master_key_id = ?4, gossip_origin = ?5 WHERE package_name = ?1 AND identifier = ?2"));
      this.autocryptPeersModelFactory = autocryptPeersModelFactory;
    }

    public void bind(@NonNull String package_name, @NonNull String identifier,
        @Nullable Date gossip_last_seen_key, @Nullable Long gossip_master_key_id,
        @Nullable AutocryptPeer.GossipOrigin gossip_origin) {
      bindString(1, package_name);
      bindString(2, identifier);
      if (gossip_last_seen_key == null) {
        bindNull(3);
      } else {
        bindLong(3, autocryptPeersModelFactory.gossip_last_seen_keyAdapter.encode(gossip_last_seen_key));
      }
      if (gossip_master_key_id == null) {
        bindNull(4);
      } else {
        bindLong(4, gossip_master_key_id);
      }
      if (gossip_origin == null) {
        bindNull(5);
      } else {
        bindLong(5, autocryptPeersModelFactory.gossip_originAdapter.encode(gossip_origin));
      }
    }
  }

  final class InsertPeer extends SqlDelightStatement {
    public InsertPeer(@NonNull SupportSQLiteDatabase database) {
      super("autocrypt_peers", database.compileStatement(""
              + "INSERT OR IGNORE INTO autocrypt_peers (package_name, identifier) VALUES (?, ?)"));
    }

    public void bind(@NonNull String package_name, @NonNull String identifier) {
      bindString(1, package_name);
      bindString(2, identifier);
    }
  }
}

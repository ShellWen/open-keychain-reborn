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
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Boolean;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Date;

public interface KeyMetadataModel {
  @Deprecated
  String TABLE_NAME = "key_metadata";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String LAST_UPDATED = "last_updated";

  @Deprecated
  String SEEN_ON_KEYSERVERS = "seen_on_keyservers";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS key_metadata (\r\n"
      + "    master_key_id INTEGER PRIMARY KEY,\r\n"
      + "    last_updated INTEGER,\r\n"
      + "    seen_on_keyservers INTEGER\r\n"
      + ")";

  @Nullable
  Long master_key_id();

  @Nullable
  Date last_updated();

  @Nullable
  Boolean seen_on_keyservers();

  interface Creator<T extends KeyMetadataModel> {
    T create(@Nullable Long master_key_id, @Nullable Date last_updated,
        @Nullable Boolean seen_on_keyservers);
  }

  final class Mapper<T extends KeyMetadataModel> implements RowMapper<T> {
    private final Factory<T> keyMetadataModelFactory;

    public Mapper(@NonNull Factory<T> keyMetadataModelFactory) {
      this.keyMetadataModelFactory = keyMetadataModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return keyMetadataModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : keyMetadataModelFactory.last_updatedAdapter.decode(cursor.getLong(1)),
          cursor.isNull(2) ? null : cursor.getInt(2) == 1
      );
    }
  }

  final class Factory<T extends KeyMetadataModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Date, Long> last_updatedAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<Date, Long> last_updatedAdapter) {
      this.creator = creator;
      this.last_updatedAdapter = last_updatedAdapter;
    }

    @NonNull
    public SqlDelightQuery selectByMasterKeyId(@Nullable Long master_key_id) {
      return new SelectByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectFingerprintsForKeysOlderThan(@Nullable Date last_updated) {
      return new SelectFingerprintsForKeysOlderThanQuery(last_updated);
    }

    @NonNull
    public Mapper<T> selectByMasterKeyIdMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<byte[]> selectFingerprintsForKeysOlderThanMapper() {
      return new RowMapper<byte[]>() {
        @Override
        public byte[] map(Cursor cursor) {
          return cursor.getBlob(0);
        }
      };
    }

    private final class SelectByMasterKeyIdQuery extends SqlDelightQuery {
      @Nullable
      private final Long master_key_id;

      SelectByMasterKeyIdQuery(@Nullable Long master_key_id) {
        super("SELECT *\r\n"
            + "    FROM key_metadata\r\n"
            + "    WHERE master_key_id = ?1",
            new TableSet("key_metadata"));

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

    private final class SelectFingerprintsForKeysOlderThanQuery extends SqlDelightQuery {
      @Nullable
      private final Date last_updated;

      SelectFingerprintsForKeysOlderThanQuery(@Nullable Date last_updated) {
        super("SELECT fingerprint\r\n"
            + "    FROM keys\r\n"
            + "        LEFT JOIN key_metadata USING (master_key_id)\r\n"
            + "    WHERE rank = 0 AND (last_updated IS NULL OR last_updated < ?1)",
            new TableSet("keys", "key_metadata"));

        this.last_updated = last_updated;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Date last_updated = this.last_updated;
        if (last_updated != null) {
          program.bindLong(1, last_updatedAdapter.encode(last_updated));
        } else {
          program.bindNull(1);
        }
      }
    }
  }

  final class DeleteAllLastUpdatedTimes extends SqlDelightStatement {
    public DeleteAllLastUpdatedTimes(@NonNull SupportSQLiteDatabase database) {
      super("key_metadata", database.compileStatement(""
              + "UPDATE key_metadata\r\n"
              + "    SET last_updated = null, seen_on_keyservers = null"));
    }
  }

  final class ReplaceKeyMetadata extends SqlDelightStatement {
    private final Factory<? extends KeyMetadataModel> keyMetadataModelFactory;

    public ReplaceKeyMetadata(@NonNull SupportSQLiteDatabase database,
        Factory<? extends KeyMetadataModel> keyMetadataModelFactory) {
      super("key_metadata", database.compileStatement(""
              + "REPLACE INTO key_metadata (master_key_id, last_updated, seen_on_keyservers) VALUES (?, ?, ?)"));
      this.keyMetadataModelFactory = keyMetadataModelFactory;
    }

    public void bind(@Nullable Long master_key_id, @Nullable Date last_updated,
        @Nullable Boolean seen_on_keyservers) {
      if (master_key_id == null) {
        bindNull(1);
      } else {
        bindLong(1, master_key_id);
      }
      if (last_updated == null) {
        bindNull(2);
      } else {
        bindLong(2, keyMetadataModelFactory.last_updatedAdapter.encode(last_updated));
      }
      if (seen_on_keyservers == null) {
        bindNull(3);
      } else {
        bindLong(3, seen_on_keyservers ? 1 : 0);
      }
    }
  }
}

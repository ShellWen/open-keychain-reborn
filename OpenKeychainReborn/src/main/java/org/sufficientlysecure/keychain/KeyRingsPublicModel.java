package org.sufficientlysecure.keychain;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface KeyRingsPublicModel {
  @Deprecated
  String TABLE_NAME = "keyrings_public";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String KEY_RING_DATA = "key_ring_data";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS keyrings_public (\r\n"
      + "    master_key_id INTEGER NOT NULL PRIMARY KEY,\r\n"
      + "    key_ring_data BLOB NULL\r\n"
      + ")";

  long master_key_id();

  @Nullable
  byte[] key_ring_data();

  interface Creator<T extends KeyRingsPublicModel> {
    T create(long master_key_id, @Nullable byte[] key_ring_data);
  }

  final class Mapper<T extends KeyRingsPublicModel> implements RowMapper<T> {
    private final Factory<T> keyRingsPublicModelFactory;

    public Mapper(@NonNull Factory<T> keyRingsPublicModelFactory) {
      this.keyRingsPublicModelFactory = keyRingsPublicModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return keyRingsPublicModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getBlob(1)
      );
    }
  }

  final class Factory<T extends KeyRingsPublicModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectAllMasterKeyIds() {
      return new SqlDelightQuery(""
          + "SELECT master_key_id\r\n"
          + "    FROM keyrings_public",
          new TableSet("keyrings_public"));
    }

    @NonNull
    public SqlDelightQuery selectByMasterKeyId(long master_key_id) {
      return new SelectByMasterKeyIdQuery(master_key_id);
    }

    public RowMapper<Long> selectAllMasterKeyIdsMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    @NonNull
    public Mapper<T> selectByMasterKeyIdMapper() {
      return new Mapper<T>(this);
    }

    private final class SelectByMasterKeyIdQuery extends SqlDelightQuery {
      private final long master_key_id;

      SelectByMasterKeyIdQuery(long master_key_id) {
        super("SELECT *\r\n"
            + "    FROM keyrings_public\r\n"
            + "    WHERE master_key_id = ?1",
            new TableSet("keyrings_public"));

        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);
      }
    }
  }

  final class InsertKeyRingPublic extends SqlDelightStatement {
    public InsertKeyRingPublic(@NonNull SupportSQLiteDatabase database) {
      super("keyrings_public", database.compileStatement(""
              + "INSERT INTO keyrings_public (master_key_id, key_ring_data) VALUES (?, ?)"));
    }

    public void bind(long master_key_id, @Nullable byte[] key_ring_data) {
      bindLong(1, master_key_id);
      if (key_ring_data == null) {
        bindNull(2);
      } else {
        bindBlob(2, key_ring_data);
      }
    }
  }

  final class DeleteByMasterKeyId extends SqlDelightStatement {
    public DeleteByMasterKeyId(@NonNull SupportSQLiteDatabase database) {
      super("keyrings_public", database.compileStatement(""
              + "DELETE FROM keyrings_public\r\n"
              + "    WHERE master_key_id = ?"));
    }

    public void bind(long master_key_id) {
      bindLong(1, master_key_id);
    }
  }
}

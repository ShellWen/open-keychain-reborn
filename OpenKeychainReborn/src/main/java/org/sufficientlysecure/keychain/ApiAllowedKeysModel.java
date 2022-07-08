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

public interface ApiAllowedKeysModel {
  @Deprecated
  String TABLE_NAME = "api_allowed_keys";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String KEY_ID = "key_id";

  @Deprecated
  String PACKAGE_NAME = "package_name";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS api_allowed_keys (\r\n"
      + "    _id INTEGER PRIMARY KEY AUTOINCREMENT,\r\n"
      + "    key_id INTEGER,\r\n"
      + "    package_name TEXT NOT NULL,\r\n"
      + "    UNIQUE (key_id, package_name),\r\n"
      + "    FOREIGN KEY (package_name) REFERENCES api_apps (package_name) ON DELETE CASCADE\r\n"
      + ")";

  @Nullable
  Long _id();

  @Nullable
  Long key_id();

  @NonNull
  String package_name();

  interface Creator<T extends ApiAllowedKeysModel> {
    T create(@Nullable Long _id, @Nullable Long key_id, @NonNull String package_name);
  }

  final class Mapper<T extends ApiAllowedKeysModel> implements RowMapper<T> {
    private final Factory<T> apiAllowedKeysModelFactory;

    public Mapper(@NonNull Factory<T> apiAllowedKeysModelFactory) {
      this.apiAllowedKeysModelFactory = apiAllowedKeysModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return apiAllowedKeysModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getLong(1),
          cursor.getString(2)
      );
    }
  }

  final class Factory<T extends ApiAllowedKeysModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery getAllowedKeys(@NonNull String package_name) {
      return new GetAllowedKeysQuery(package_name);
    }

    public RowMapper<Long> getAllowedKeysMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }

    private final class GetAllowedKeysQuery extends SqlDelightQuery {
      @NonNull
      private final String package_name;

      GetAllowedKeysQuery(@NonNull String package_name) {
        super("SELECT key_id\r\n"
            + "    FROM api_allowed_keys\r\n"
            + "    WHERE package_name = ?1",
            new TableSet("api_allowed_keys"));

        this.package_name = package_name;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, package_name);
      }
    }
  }

  final class InsertAllowedKey extends SqlDelightStatement {
    public InsertAllowedKey(@NonNull SupportSQLiteDatabase database) {
      super("api_allowed_keys", database.compileStatement(""
              + "INSERT OR IGNORE INTO api_allowed_keys (package_name, key_id) VALUES (?, ?)"));
    }

    public void bind(@NonNull String package_name, @Nullable Long key_id) {
      bindString(1, package_name);
      if (key_id == null) {
        bindNull(2);
      } else {
        bindLong(2, key_id);
      }
    }
  }

  final class DeleteByPackageName extends SqlDelightStatement {
    public DeleteByPackageName(@NonNull SupportSQLiteDatabase database) {
      super("api_allowed_keys", database.compileStatement(""
              + "DELETE FROM api_allowed_keys\r\n"
              + "    WHERE package_name = ?"));
    }

    public void bind(@NonNull String package_name) {
      bindString(1, package_name);
    }
  }
}

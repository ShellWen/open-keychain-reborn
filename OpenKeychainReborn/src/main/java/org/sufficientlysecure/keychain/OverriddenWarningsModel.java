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

public interface OverriddenWarningsModel {
  @Deprecated
  String TABLE_NAME = "overridden_warnings";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String IDENTIFIER = "identifier";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS overridden_warnings (\r\n"
      + "    _id INTEGER PRIMARY KEY AUTOINCREMENT,\r\n"
      + "    identifier TEXT NOT NULL UNIQUE\r\n"
      + ")";

  @Nullable
  Long _id();

  @NonNull
  String identifier();

  interface Creator<T extends OverriddenWarningsModel> {
    T create(@Nullable Long _id, @NonNull String identifier);
  }

  final class Mapper<T extends OverriddenWarningsModel> implements RowMapper<T> {
    private final Factory<T> overriddenWarningsModelFactory;

    public Mapper(@NonNull Factory<T> overriddenWarningsModelFactory) {
      this.overriddenWarningsModelFactory = overriddenWarningsModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return overriddenWarningsModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  final class Factory<T extends OverriddenWarningsModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectCountByIdentifier(@NonNull String identifier) {
      return new SelectCountByIdentifierQuery(identifier);
    }

    public RowMapper<Long> selectCountByIdentifierMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    private final class SelectCountByIdentifierQuery extends SqlDelightQuery {
      @NonNull
      private final String identifier;

      SelectCountByIdentifierQuery(@NonNull String identifier) {
        super("SELECT COUNT(*)\r\n"
            + "    FROM overridden_warnings\r\n"
            + "    WHERE identifier = ?1",
            new TableSet("overridden_warnings"));

        this.identifier = identifier;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, identifier);
      }
    }
  }

  final class InsertIdentifier extends SqlDelightStatement {
    public InsertIdentifier(@NonNull SupportSQLiteDatabase database) {
      super("overridden_warnings", database.compileStatement(""
              + "INSERT OR IGNORE INTO overridden_warnings (identifier) VALUES (?)"));
    }

    public void bind(@NonNull String identifier) {
      bindString(1, identifier);
    }
  }

  final class DeleteByIdentifier extends SqlDelightStatement {
    public DeleteByIdentifier(@NonNull SupportSQLiteDatabase database) {
      super("overridden_warnings", database.compileStatement(""
              + "DELETE FROM overridden_warnings\r\n"
              + "    WHERE identifier = ?"));
    }

    public void bind(@NonNull String identifier) {
      bindString(1, identifier);
    }
  }
}

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

public interface ApiAppsModel {
  @Deprecated
  String TABLE_NAME = "api_apps";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String PACKAGE_NAME = "package_name";

  @Deprecated
  String PACKAGE_SIGNATURE = "package_signature";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS api_apps (\r\n"
      + "    _id INTEGER PRIMARY KEY AUTOINCREMENT,\r\n"
      + "    package_name TEXT NOT NULL UNIQUE,\r\n"
      + "    package_signature BLOB\r\n"
      + ")";

  @Nullable
  Long _id();

  @NonNull
  String package_name();

  @Nullable
  byte[] package_signature();

  interface Creator<T extends ApiAppsModel> {
    T create(@Nullable Long _id, @NonNull String package_name, @Nullable byte[] package_signature);
  }

  final class Mapper<T extends ApiAppsModel> implements RowMapper<T> {
    private final Factory<T> apiAppsModelFactory;

    public Mapper(@NonNull Factory<T> apiAppsModelFactory) {
      this.apiAppsModelFactory = apiAppsModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return apiAppsModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getBlob(2)
      );
    }
  }

  final class Factory<T extends ApiAppsModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectAll() {
      return new SqlDelightQuery(""
          + "SELECT *\r\n"
          + "    FROM api_apps",
          new TableSet("api_apps"));
    }

    @NonNull
    public SqlDelightQuery selectByPackageName(@NonNull String package_name) {
      return new SelectByPackageNameQuery(package_name);
    }

    @NonNull
    public SqlDelightQuery getCertificate(@NonNull String package_name) {
      return new GetCertificateQuery(package_name);
    }

    @NonNull
    public Mapper<T> selectAllMapper() {
      return new Mapper<T>(this);
    }

    @NonNull
    public Mapper<T> selectByPackageNameMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<byte[]> getCertificateMapper() {
      return new RowMapper<byte[]>() {
        @Override
        public byte[] map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getBlob(0);
        }
      };
    }

    private final class SelectByPackageNameQuery extends SqlDelightQuery {
      @NonNull
      private final String package_name;

      SelectByPackageNameQuery(@NonNull String package_name) {
        super("SELECT *\r\n"
            + "    FROM api_apps\r\n"
            + "    WHERE package_name = ?1",
            new TableSet("api_apps"));

        this.package_name = package_name;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, package_name);
      }
    }

    private final class GetCertificateQuery extends SqlDelightQuery {
      @NonNull
      private final String package_name;

      GetCertificateQuery(@NonNull String package_name) {
        super("SELECT package_signature\r\n"
            + "    FROM api_apps\r\n"
            + "    WHERE package_name = ?1",
            new TableSet("api_apps"));

        this.package_name = package_name;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, package_name);
      }
    }
  }

  final class InsertApiApp extends SqlDelightStatement {
    public InsertApiApp(@NonNull SupportSQLiteDatabase database) {
      super("api_apps", database.compileStatement(""
              + "INSERT INTO api_apps (package_name, package_signature) VALUES (?, ?)"));
    }

    public void bind(@NonNull String package_name, @Nullable byte[] package_signature) {
      bindString(1, package_name);
      if (package_signature == null) {
        bindNull(2);
      } else {
        bindBlob(2, package_signature);
      }
    }
  }

  final class DeleteByPackageName extends SqlDelightStatement {
    public DeleteByPackageName(@NonNull SupportSQLiteDatabase database) {
      super("api_apps", database.compileStatement(""
              + "DELETE FROM api_apps\r\n"
              + "    WHERE package_name = ?"));
    }

    public void bind(@NonNull String package_name) {
      bindString(1, package_name);
    }
  }
}

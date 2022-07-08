package org.sufficientlysecure.keychain;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import com.squareup.sqldelight.prerelease.internal.QuestionMarks;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface KeySignaturesModel {
  @Deprecated
  String TABLE_NAME = "key_signatures";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String SIGNER_KEY_ID = "signer_key_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS key_signatures (\r\n"
      + "    master_key_id INTEGER NOT NULL,\r\n"
      + "    signer_key_id INTEGER NOT NULL,\r\n"
      + "    PRIMARY KEY(master_key_id, signer_key_id),\r\n"
      + "    FOREIGN KEY(master_key_id) REFERENCES keyrings_public(master_key_id) ON DELETE CASCADE\r\n"
      + ")";

  long master_key_id();

  long signer_key_id();

  interface Creator<T extends KeySignaturesModel> {
    T create(long master_key_id, long signer_key_id);
  }

  final class Mapper<T extends KeySignaturesModel> implements RowMapper<T> {
    private final Factory<T> keySignaturesModelFactory;

    public Mapper(@NonNull Factory<T> keySignaturesModelFactory) {
      this.keySignaturesModelFactory = keySignaturesModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return keySignaturesModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  final class Factory<T extends KeySignaturesModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectMasterKeyIdsBySigner(long[] signer_key_id) {
      return new SelectMasterKeyIdsBySignerQuery(signer_key_id);
    }

    public RowMapper<Long> selectMasterKeyIdsBySignerMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    private final class SelectMasterKeyIdsBySignerQuery extends SqlDelightQuery {
      private final long[] signer_key_id;

      SelectMasterKeyIdsBySignerQuery(long[] signer_key_id) {
        super("SELECT master_key_id\r\n"
            + "   FROM key_signatures WHERE signer_key_id IN " + QuestionMarks.ofSize(signer_key_id.length),
            new TableSet("key_signatures"));

        this.signer_key_id = signer_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        for (long item : signer_key_id) {
          program.bindLong(nextIndex++, item);
        }
      }
    }
  }

  final class InsertKeySignature extends SqlDelightStatement {
    public InsertKeySignature(@NonNull SupportSQLiteDatabase database) {
      super("key_signatures", database.compileStatement(""
              + "INSERT INTO key_signatures (master_key_id, signer_key_id) VALUES (?, ?)"));
    }

    public void bind(long master_key_id, long signer_key_id) {
      bindLong(1, master_key_id);
      bindLong(2, signer_key_id);
    }
  }
}

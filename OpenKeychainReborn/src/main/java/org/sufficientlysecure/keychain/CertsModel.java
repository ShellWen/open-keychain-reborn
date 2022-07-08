package org.sufficientlysecure.keychain;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;

public interface CertsModel {
  @Deprecated
  String TABLE_NAME = "certs";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String RANK = "rank";

  @Deprecated
  String KEY_ID_CERTIFIER = "key_id_certifier";

  @Deprecated
  String TYPE = "type";

  @Deprecated
  String VERIFIED = "verified";

  @Deprecated
  String CREATION = "creation";

  @Deprecated
  String DATA = "data";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS certs(\r\n"
      + "    master_key_id INTEGER NOT NULL,\r\n"
      + "    rank INTEGER NOT NULL,\r\n"
      + "    key_id_certifier INTEGER NOT NULL,\r\n"
      + "    type INTEGER NOT NULL,\r\n"
      + "    verified INTEGER NOT NULL DEFAULT 0,\r\n"
      + "    creation INTEGER NOT NULL,\r\n"
      + "    data BLOB NOT NULL,\r\n"
      + "    PRIMARY KEY(master_key_id, rank, key_id_certifier),\r\n"
      + "    FOREIGN KEY(master_key_id) REFERENCES keyrings_public(master_key_id) ON DELETE CASCADE,\r\n"
      + "    FOREIGN KEY(master_key_id, rank) REFERENCES user_packets(master_key_id, rank) ON DELETE CASCADE\r\n"
      + ")";

  long master_key_id();

  long rank();

  long key_id_certifier();

  long type();

  @NonNull
  CanonicalizedKeyRing.VerificationStatus verified();

  long creation();

  @NonNull
  byte[] data();

  interface SelectVerifyingCertDetailsModel {
    long masterKeyId();

    long signerMasterKeyId();

    long creation();
  }

  interface SelectVerifyingCertDetailsCreator<T extends SelectVerifyingCertDetailsModel> {
    T create(long masterKeyId, long signerMasterKeyId, long creation);
  }

  final class SelectVerifyingCertDetailsMapper<T extends SelectVerifyingCertDetailsModel> implements RowMapper<T> {
    private final SelectVerifyingCertDetailsCreator<T> creator;

    public SelectVerifyingCertDetailsMapper(SelectVerifyingCertDetailsCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getLong(2)
      );
    }
  }

  interface Creator<T extends CertsModel> {
    T create(long master_key_id, long rank, long key_id_certifier, long type,
        @NonNull CanonicalizedKeyRing.VerificationStatus verified, long creation,
        @NonNull byte[] data);
  }

  final class Mapper<T extends CertsModel> implements RowMapper<T> {
    private final Factory<T> certsModelFactory;

    public Mapper(@NonNull Factory<T> certsModelFactory) {
      this.certsModelFactory = certsModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return certsModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getLong(2),
          cursor.getLong(3),
          certsModelFactory.verifiedAdapter.decode(cursor.getLong(4)),
          cursor.getLong(5),
          cursor.getBlob(6)
      );
    }
  }

  final class Factory<T extends CertsModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<CanonicalizedKeyRing.VerificationStatus, Long> verifiedAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<CanonicalizedKeyRing.VerificationStatus, Long> verifiedAdapter) {
      this.creator = creator;
      this.verifiedAdapter = verifiedAdapter;
    }

    @NonNull
    public SqlDelightQuery selectVerifyingCertDetails(long master_key_id, long rank) {
      return new SelectVerifyingCertDetailsQuery(master_key_id, rank);
    }

    @NonNull
    public <R extends SelectVerifyingCertDetailsModel> SelectVerifyingCertDetailsMapper<R> selectVerifyingCertDetailsMapper(
        SelectVerifyingCertDetailsCreator<R> creator) {
      return new SelectVerifyingCertDetailsMapper<R>(creator);
    }

    private final class SelectVerifyingCertDetailsQuery extends SqlDelightQuery {
      private final long master_key_id;

      private final long rank;

      SelectVerifyingCertDetailsQuery(long master_key_id, long rank) {
        super("SELECT master_key_id AS masterKeyId, key_id_certifier AS signerMasterKeyId, creation * 1000 AS creation\r\n"
            + "    FROM certs\r\n"
            + "    WHERE verified = 1 AND master_key_id = ?1 AND rank = ?2\r\n"
            + "    ORDER BY creation DESC\r\n"
            + "    LIMIT 1",
            new TableSet("certs"));

        this.master_key_id = master_key_id;
        this.rank = rank;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);

        program.bindLong(2, rank);
      }
    }
  }

  final class InsertCert extends SqlDelightStatement {
    private final Factory<? extends CertsModel> certsModelFactory;

    public InsertCert(@NonNull SupportSQLiteDatabase database,
        Factory<? extends CertsModel> certsModelFactory) {
      super("certs", database.compileStatement(""
              + "INSERT INTO certs (master_key_id, rank, key_id_certifier, type, verified, creation, data) VALUES (?, ?, ?, ?, ?, ?, ?)"));
      this.certsModelFactory = certsModelFactory;
    }

    public void bind(long master_key_id, long rank, long key_id_certifier, long type,
        @NonNull CanonicalizedKeyRing.VerificationStatus verified, long creation,
        @NonNull byte[] data) {
      bindLong(1, master_key_id);
      bindLong(2, rank);
      bindLong(3, key_id_certifier);
      bindLong(4, type);
      bindLong(5, certsModelFactory.verifiedAdapter.encode(verified));
      bindLong(6, creation);
      bindBlob(7, data);
    }
  }
}

package org.sufficientlysecure.keychain;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;

public interface UserPacketsModel {
  @Deprecated
  String UIDSTATUS_VIEW_NAME = "uidStatus";

  @Deprecated
  String TABLE_NAME = "user_packets";

  @Deprecated
  String MASTER_KEY_ID = "master_key_id";

  @Deprecated
  String RANK = "rank";

  @Deprecated
  String TYPE = "type";

  @Deprecated
  String USER_ID = "user_id";

  @Deprecated
  String NAME = "name";

  @Deprecated
  String EMAIL = "email";

  @Deprecated
  String COMMENT = "comment";

  @Deprecated
  String IS_PRIMARY = "is_primary";

  @Deprecated
  String IS_REVOKED = "is_revoked";

  @Deprecated
  String ATTRIBUTE_DATA = "attribute_data";

  String CREATE_TABLE = ""
      + "CREATE TABLE IF NOT EXISTS user_packets(\r\n"
      + "    master_key_id INTEGER NOT NULL,\r\n"
      + "    rank INTEGER NOT NULL,\r\n"
      + "    type INTEGER,\r\n"
      + "    user_id TEXT,\r\n"
      + "    name TEXT,\r\n"
      + "    email TEXT,\r\n"
      + "    comment TEXT,\r\n"
      + "    is_primary INTEGER NOT NULL,\r\n"
      + "    is_revoked INTEGER NOT NULL,\r\n"
      + "    attribute_data BLOB,\r\n"
      + "    PRIMARY KEY(master_key_id, rank),\r\n"
      + "    FOREIGN KEY(master_key_id) REFERENCES keyrings_public(master_key_id) ON DELETE CASCADE\r\n"
      + ")";

  String UIDSTATUS = ""
      + "CREATE VIEW uidStatus AS\r\n"
      + "    SELECT user_packets.email, MIN(certs.verified) AS key_status_int, user_packets.user_id, user_packets.master_key_id, COUNT(DISTINCT user_packets.master_key_id) AS candidates\r\n"
      + "    FROM user_packets\r\n"
      + "        JOIN validMasterKeys USING (master_key_id)\r\n"
      + "        LEFT JOIN certs ON (certs.master_key_id = user_packets.master_key_id AND certs.rank = user_packets.rank AND certs.verified > 0)\r\n"
      + "    WHERE user_packets.email IS NOT NULL\r\n"
      + "    GROUP BY user_packets.email";

  long master_key_id();

  int rank();

  @Nullable
  Long type();

  @Nullable
  String user_id();

  @Nullable
  String name();

  @Nullable
  String email();

  @Nullable
  String comment();

  boolean is_primary();

  boolean is_revoked();

  @Nullable
  byte[] attribute_data();

  interface SelectUserIdsByMasterKeyIdModel {
    long master_key_id();

    int rank();

    @Nullable
    String user_id();

    @Nullable
    String name();

    @Nullable
    String email();

    @Nullable
    String comment();

    boolean is_primary();

    boolean is_revoked();

    @Nullable
    Long verified_int();
  }

  interface SelectUserIdsByMasterKeyIdCreator<T extends SelectUserIdsByMasterKeyIdModel> {
    T create(long master_key_id, int rank, @Nullable String user_id, @Nullable String name,
        @Nullable String email, @Nullable String comment, boolean is_primary, boolean is_revoked,
        @Nullable Long verified_int);
  }

  final class SelectUserIdsByMasterKeyIdMapper<T extends SelectUserIdsByMasterKeyIdModel> implements RowMapper<T> {
    private final SelectUserIdsByMasterKeyIdCreator<T> creator;

    public SelectUserIdsByMasterKeyIdMapper(SelectUserIdsByMasterKeyIdCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.getInt(6) == 1,
          cursor.getInt(7) == 1,
          cursor.isNull(8) ? null : cursor.getLong(8)
      );
    }
  }

  interface SelectUserIdsByMasterKeyIdAndVerificationModel {
    long master_key_id();

    int rank();

    @Nullable
    String user_id();

    @Nullable
    String name();

    @Nullable
    String email();

    @Nullable
    String comment();

    boolean is_primary();

    boolean is_revoked();

    @Nullable
    Long verified_int();
  }

  interface SelectUserIdsByMasterKeyIdAndVerificationCreator<T extends SelectUserIdsByMasterKeyIdAndVerificationModel> {
    T create(long master_key_id, int rank, @Nullable String user_id, @Nullable String name,
        @Nullable String email, @Nullable String comment, boolean is_primary, boolean is_revoked,
        @Nullable Long verified_int);
  }

  final class SelectUserIdsByMasterKeyIdAndVerificationMapper<T extends SelectUserIdsByMasterKeyIdAndVerificationModel> implements RowMapper<T> {
    private final SelectUserIdsByMasterKeyIdAndVerificationCreator<T> creator;

    public SelectUserIdsByMasterKeyIdAndVerificationMapper(
        SelectUserIdsByMasterKeyIdAndVerificationCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.getInt(6) == 1,
          cursor.getInt(7) == 1,
          cursor.isNull(8) ? null : cursor.getLong(8)
      );
    }
  }

  interface SelectUserAttributesByTypeAndMasterKeyIdModel {
    long master_key_id();

    int rank();

    boolean is_primary();

    boolean is_revoked();

    @Nullable
    Long verified_int();

    @Nullable
    byte[] attribute_data();
  }

  interface SelectUserAttributesByTypeAndMasterKeyIdCreator<T extends SelectUserAttributesByTypeAndMasterKeyIdModel> {
    T create(long master_key_id, int rank, boolean is_primary, boolean is_revoked,
        @Nullable Long verified_int, @Nullable byte[] attribute_data);
  }

  final class SelectUserAttributesByTypeAndMasterKeyIdMapper<T extends SelectUserAttributesByTypeAndMasterKeyIdModel> implements RowMapper<T> {
    private final SelectUserAttributesByTypeAndMasterKeyIdCreator<T> creator;

    public SelectUserAttributesByTypeAndMasterKeyIdMapper(
        SelectUserAttributesByTypeAndMasterKeyIdCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.getInt(2) == 1,
          cursor.getInt(3) == 1,
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.isNull(5) ? null : cursor.getBlob(5)
      );
    }
  }

  interface SelectSpecificUserAttributeModel {
    long master_key_id();

    int rank();

    boolean is_primary();

    boolean is_revoked();

    @Nullable
    Long verified_int();

    @Nullable
    byte[] attribute_data();
  }

  interface SelectSpecificUserAttributeCreator<T extends SelectSpecificUserAttributeModel> {
    T create(long master_key_id, int rank, boolean is_primary, boolean is_revoked,
        @Nullable Long verified_int, @Nullable byte[] attribute_data);
  }

  final class SelectSpecificUserAttributeMapper<T extends SelectSpecificUserAttributeModel> implements RowMapper<T> {
    private final SelectSpecificUserAttributeCreator<T> creator;

    public SelectSpecificUserAttributeMapper(SelectSpecificUserAttributeCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.getInt(2) == 1,
          cursor.getInt(3) == 1,
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.isNull(5) ? null : cursor.getBlob(5)
      );
    }
  }

  interface UidStatusModel {
    @Nullable
    String email();

    @Nullable
    Long key_status_int();

    @Nullable
    String user_id();

    long master_key_id();

    long candidates();
  }

  interface UidStatusCreator<T extends UidStatusModel> {
    T create(@Nullable String email, @Nullable Long key_status_int, @Nullable String user_id,
        long master_key_id, long candidates);
  }

  final class UidStatusMapper<T extends UidStatusModel> implements RowMapper<T> {
    private final UidStatusCreator<T> creator;

    public UidStatusMapper(UidStatusCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getLong(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.getLong(3),
          cursor.getLong(4)
      );
    }
  }

  interface Creator<T extends UserPacketsModel> {
    T create(long master_key_id, int rank, @Nullable Long type, @Nullable String user_id,
        @Nullable String name, @Nullable String email, @Nullable String comment, boolean is_primary,
        boolean is_revoked, @Nullable byte[] attribute_data);
  }

  final class Mapper<T extends UserPacketsModel> implements RowMapper<T> {
    private final Factory<T> userPacketsModelFactory;

    public Mapper(@NonNull Factory<T> userPacketsModelFactory) {
      this.userPacketsModelFactory = userPacketsModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userPacketsModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.isNull(2) ? null : cursor.getLong(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.isNull(6) ? null : cursor.getString(6),
          cursor.getInt(7) == 1,
          cursor.getInt(8) == 1,
          cursor.isNull(9) ? null : cursor.getBlob(9)
      );
    }
  }

  final class Factory<T extends UserPacketsModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectUserIdsByMasterKeyId(long[] master_key_id) {
      return new SelectUserIdsByMasterKeyIdQuery(master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectUserIdsByMasterKeyIdAndVerification(
        @NonNull CertsModel.Factory<? extends CertsModel> certsModelFactory, long master_key_id,
        @Nullable CanonicalizedKeyRing.VerificationStatus verified) {
      return new SelectUserIdsByMasterKeyIdAndVerificationQuery(certsModelFactory, master_key_id,
          verified);
    }

    @NonNull
    public SqlDelightQuery selectUserAttributesByTypeAndMasterKeyId(@Nullable Long type,
        long master_key_id) {
      return new SelectUserAttributesByTypeAndMasterKeyIdQuery(type, master_key_id);
    }

    @NonNull
    public SqlDelightQuery selectSpecificUserAttribute(@Nullable Long type, long master_key_id,
        int rank) {
      return new SelectSpecificUserAttributeQuery(type, master_key_id, rank);
    }

    @NonNull
    public SqlDelightQuery selectUserIdStatusByEmail(@Nullable String[] email) {
      return new SelectUserIdStatusByEmailQuery(email);
    }

    @NonNull
    public SqlDelightQuery selectUserIdStatusByEmailLike(@Nullable String email) {
      return new SelectUserIdStatusByEmailLikeQuery(email);
    }

    @NonNull
    public <R extends SelectUserIdsByMasterKeyIdModel> SelectUserIdsByMasterKeyIdMapper<R> selectUserIdsByMasterKeyIdMapper(
        SelectUserIdsByMasterKeyIdCreator<R> creator) {
      return new SelectUserIdsByMasterKeyIdMapper<R>(creator);
    }

    @NonNull
    public <R extends SelectUserIdsByMasterKeyIdAndVerificationModel> SelectUserIdsByMasterKeyIdAndVerificationMapper<R> selectUserIdsByMasterKeyIdAndVerificationMapper(
        SelectUserIdsByMasterKeyIdAndVerificationCreator<R> creator) {
      return new SelectUserIdsByMasterKeyIdAndVerificationMapper<R>(creator);
    }

    @NonNull
    public <R extends SelectUserAttributesByTypeAndMasterKeyIdModel> SelectUserAttributesByTypeAndMasterKeyIdMapper<R> selectUserAttributesByTypeAndMasterKeyIdMapper(
        SelectUserAttributesByTypeAndMasterKeyIdCreator<R> creator) {
      return new SelectUserAttributesByTypeAndMasterKeyIdMapper<R>(creator);
    }

    @NonNull
    public <R extends SelectSpecificUserAttributeModel> SelectSpecificUserAttributeMapper<R> selectSpecificUserAttributeMapper(
        SelectSpecificUserAttributeCreator<R> creator) {
      return new SelectSpecificUserAttributeMapper<R>(creator);
    }

    @NonNull
    public <R extends UidStatusModel> UidStatusMapper<R> selectUserIdStatusByEmailMapper(
        UidStatusCreator<R> creator) {
      return new UidStatusMapper<R>(creator);
    }

    @NonNull
    public <R extends UidStatusModel> UidStatusMapper<R> selectUserIdStatusByEmailLikeMapper(
        UidStatusCreator<R> creator) {
      return new UidStatusMapper<R>(creator);
    }

    private final class SelectUserIdsByMasterKeyIdQuery extends SqlDelightQuery {
      private final long[] master_key_id;

      SelectUserIdsByMasterKeyIdQuery(long[] master_key_id) {
        super("SELECT user_packets.master_key_id, user_packets.rank, user_id, name, email, comment, is_primary, is_revoked, MIN(certs.verified) AS verified_int\r\n"
            + "    FROM user_packets\r\n"
            + "        LEFT JOIN certs ON ( user_packets.master_key_id = certs.master_key_id AND user_packets.rank = certs.rank AND certs.verified > 0 )\r\n"
            + "    WHERE user_packets.type IS NULL AND user_packets.is_revoked = 0 AND user_packets.master_key_id IN " + QuestionMarks.ofSize(master_key_id.length) + "\r\n"
            + "    GROUP BY user_packets.master_key_id, user_packets.rank\r\n"
            + "    ORDER BY user_packets.master_key_id ASC,user_packets.rank ASC",
            new TableSet("user_packets", "certs"));

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

    private final class SelectUserIdsByMasterKeyIdAndVerificationQuery extends SqlDelightQuery {
      @NonNull
      private final CertsModel.Factory<? extends CertsModel> certsModelFactory;

      private final long master_key_id;

      @Nullable
      private final CanonicalizedKeyRing.VerificationStatus verified;

      SelectUserIdsByMasterKeyIdAndVerificationQuery(
          @NonNull CertsModel.Factory<? extends CertsModel> certsModelFactory, long master_key_id,
          @Nullable CanonicalizedKeyRing.VerificationStatus verified) {
        super("SELECT user_packets.master_key_id, user_packets.rank, user_id, name, email, comment, is_primary, is_revoked, MIN(certs.verified) AS verified_int\r\n"
            + "    FROM user_packets\r\n"
            + "        LEFT JOIN certs ON ( user_packets.master_key_id = certs.master_key_id AND user_packets.rank = certs.rank AND certs.verified > 0 )\r\n"
            + "    WHERE user_packets.type IS NULL AND user_packets.is_revoked = 0 AND user_packets.master_key_id = ?1 AND certs.verified = ?2\r\n"
            + "    GROUP BY user_packets.rank\r\n"
            + "    ORDER BY user_packets.rank ASC",
            new TableSet("user_packets", "certs"));

        this.certsModelFactory = certsModelFactory;
        this.master_key_id = master_key_id;
        this.verified = verified;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, master_key_id);

        CanonicalizedKeyRing.VerificationStatus verified = this.verified;
        if (verified != null) {
          program.bindLong(2, certsModelFactory.verifiedAdapter.encode(verified));
        } else {
          program.bindNull(2);
        }
      }
    }

    private final class SelectUserAttributesByTypeAndMasterKeyIdQuery extends SqlDelightQuery {
      @Nullable
      private final Long type;

      private final long master_key_id;

      SelectUserAttributesByTypeAndMasterKeyIdQuery(@Nullable Long type, long master_key_id) {
        super("SELECT user_packets.master_key_id, user_packets.rank, is_primary, is_revoked, MIN(certs.verified) AS verified_int, attribute_data\r\n"
            + "    FROM user_packets\r\n"
            + "        LEFT JOIN certs ON ( user_packets.master_key_id = certs.master_key_id AND user_packets.rank = certs.rank AND certs.verified > 0 )\r\n"
            + "    WHERE user_packets.type = ?1 AND user_packets.is_revoked = 0 AND user_packets.master_key_id = ?2\r\n"
            + "    GROUP BY user_packets.rank\r\n"
            + "    ORDER BY user_packets.rank ASC",
            new TableSet("user_packets", "certs"));

        this.type = type;
        this.master_key_id = master_key_id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Long type = this.type;
        if (type != null) {
          program.bindLong(1, type);
        } else {
          program.bindNull(1);
        }

        program.bindLong(2, master_key_id);
      }
    }

    private final class SelectSpecificUserAttributeQuery extends SqlDelightQuery {
      @Nullable
      private final Long type;

      private final long master_key_id;

      private final int rank;

      SelectSpecificUserAttributeQuery(@Nullable Long type, long master_key_id, int rank) {
        super("SELECT user_packets.master_key_id, user_packets.rank, is_primary, is_revoked, MIN(certs.verified) AS verified_int, attribute_data\r\n"
            + "    FROM user_packets\r\n"
            + "        LEFT JOIN certs ON ( user_packets.master_key_id = certs.master_key_id AND user_packets.rank = certs.rank AND certs.verified > 0 )\r\n"
            + "    WHERE user_packets.type = ?1 AND user_packets.master_key_id = ?2 AND user_packets.rank = ?3\r\n"
            + "    GROUP BY user_packets.master_key_id, user_packets.rank",
            new TableSet("user_packets", "certs"));

        this.type = type;
        this.master_key_id = master_key_id;
        this.rank = rank;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Long type = this.type;
        if (type != null) {
          program.bindLong(1, type);
        } else {
          program.bindNull(1);
        }

        program.bindLong(2, master_key_id);

        program.bindLong(3, rank);
      }
    }

    private final class SelectUserIdStatusByEmailQuery extends SqlDelightQuery {
      @Nullable
      private final String[] email;

      SelectUserIdStatusByEmailQuery(@Nullable String[] email) {
        super("SELECT *\r\n"
            + "FROM uidStatus\r\n"
            + "    WHERE email IN " + QuestionMarks.ofSize(email.length),
            new TableSet("user_packets", "keys", "certs"));

        this.email = email;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        String[] email = this.email;
        if (email != null) {
          for (String item : email) {
            program.bindString(nextIndex++, item);
          }
        } else {
          program.bindNull(nextIndex++);
        }
      }
    }

    private final class SelectUserIdStatusByEmailLikeQuery extends SqlDelightQuery {
      @Nullable
      private final String email;

      SelectUserIdStatusByEmailLikeQuery(@Nullable String email) {
        super("SELECT *\r\n"
            + "FROM uidStatus\r\n"
            + "    WHERE email LIKE ?1",
            new TableSet("user_packets", "keys", "certs"));

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
  }

  final class InsertUserPacket extends SqlDelightStatement {
    public InsertUserPacket(@NonNull SupportSQLiteDatabase database) {
      super("user_packets", database.compileStatement(""
              + "INSERT INTO user_packets (master_key_id, rank, type, user_id, name, email, comment, is_primary, is_revoked, attribute_data)\r\n"
              + "    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
    }

    public void bind(long master_key_id, int rank, @Nullable Long type, @Nullable String user_id,
        @Nullable String name, @Nullable String email, @Nullable String comment, boolean is_primary,
        boolean is_revoked, @Nullable byte[] attribute_data) {
      bindLong(1, master_key_id);
      bindLong(2, rank);
      if (type == null) {
        bindNull(3);
      } else {
        bindLong(3, type);
      }
      if (user_id == null) {
        bindNull(4);
      } else {
        bindString(4, user_id);
      }
      if (name == null) {
        bindNull(5);
      } else {
        bindString(5, name);
      }
      if (email == null) {
        bindNull(6);
      } else {
        bindString(6, email);
      }
      if (comment == null) {
        bindNull(7);
      } else {
        bindString(7, comment);
      }
      bindLong(8, is_primary ? 1 : 0);
      bindLong(9, is_revoked ? 1 : 0);
      if (attribute_data == null) {
        bindNull(10);
      } else {
        bindBlob(10, attribute_data);
      }
    }
  }
}

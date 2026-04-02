package com.powerguard.app.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EventLogDao_Impl implements EventLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EventLogEntity> __insertionAdapterOfEventLogEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public EventLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEventLogEntity = new EntityInsertionAdapter<EventLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `event_log` (`id`,`timestamp`,`featureTypeKey`,`featureDisplayName`,`action`,`reason`,`wasDirectControl`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EventLogEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getFeatureTypeKey());
        statement.bindString(4, entity.getFeatureDisplayName());
        statement.bindString(5, entity.getAction());
        statement.bindString(6, entity.getReason());
        final int _tmp = entity.getWasDirectControl() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM event_log WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM event_log";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final EventLogEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEventLogEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long beforeTimestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, beforeTimestamp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EventLogEntity>> observeAll() {
    final String _sql = "SELECT * FROM event_log ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"event_log"}, new Callable<List<EventLogEntity>>() {
      @Override
      @NonNull
      public List<EventLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfFeatureTypeKey = CursorUtil.getColumnIndexOrThrow(_cursor, "featureTypeKey");
          final int _cursorIndexOfFeatureDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "featureDisplayName");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfWasDirectControl = CursorUtil.getColumnIndexOrThrow(_cursor, "wasDirectControl");
          final List<EventLogEntity> _result = new ArrayList<EventLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EventLogEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpFeatureTypeKey;
            _tmpFeatureTypeKey = _cursor.getString(_cursorIndexOfFeatureTypeKey);
            final String _tmpFeatureDisplayName;
            _tmpFeatureDisplayName = _cursor.getString(_cursorIndexOfFeatureDisplayName);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpReason;
            _tmpReason = _cursor.getString(_cursorIndexOfReason);
            final boolean _tmpWasDirectControl;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWasDirectControl);
            _tmpWasDirectControl = _tmp != 0;
            _item = new EventLogEntity(_tmpId,_tmpTimestamp,_tmpFeatureTypeKey,_tmpFeatureDisplayName,_tmpAction,_tmpReason,_tmpWasDirectControl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecent(final int limit,
      final Continuation<? super List<EventLogEntity>> $completion) {
    final String _sql = "SELECT * FROM event_log ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EventLogEntity>>() {
      @Override
      @NonNull
      public List<EventLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfFeatureTypeKey = CursorUtil.getColumnIndexOrThrow(_cursor, "featureTypeKey");
          final int _cursorIndexOfFeatureDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "featureDisplayName");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfWasDirectControl = CursorUtil.getColumnIndexOrThrow(_cursor, "wasDirectControl");
          final List<EventLogEntity> _result = new ArrayList<EventLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EventLogEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpFeatureTypeKey;
            _tmpFeatureTypeKey = _cursor.getString(_cursorIndexOfFeatureTypeKey);
            final String _tmpFeatureDisplayName;
            _tmpFeatureDisplayName = _cursor.getString(_cursorIndexOfFeatureDisplayName);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpReason;
            _tmpReason = _cursor.getString(_cursorIndexOfReason);
            final boolean _tmpWasDirectControl;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWasDirectControl);
            _tmpWasDirectControl = _tmp != 0;
            _item = new EventLogEntity(_tmpId,_tmpTimestamp,_tmpFeatureTypeKey,_tmpFeatureDisplayName,_tmpAction,_tmpReason,_tmpWasDirectControl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> observeCount() {
    final String _sql = "SELECT COUNT(*) FROM event_log";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"event_log"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

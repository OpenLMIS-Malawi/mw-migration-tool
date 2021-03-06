package mw.gov.health.lmis.migration.tool.scm.repository;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import mw.gov.health.lmis.migration.tool.config.ToolProperties;
import mw.gov.health.lmis.migration.tool.scm.ScmDatabaseHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;


public abstract class BaseAccessRepository<T> {
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ScmDatabaseHandler handler;

  @Autowired
  protected ToolProperties properties;

  /**
   * Find all rows from the given table.
   */
  public List<T> findAll() {
    Database database = getDatabase();

    try {
      Table table = getTable(database);
      List<T> list = Lists.newArrayList();

      for (Row row : table) {
        list.add(mapRow(row));
      }

      return list;
    } finally {
      IOUtils.closeQuietly(database);
    }
  }

  List<T> findAll(Predicate<T> predicate) {
    return findAll(predicate, null);
  }

  List<T> findAll(Predicate<T> predicate, Details details) {
    Database database = getDatabase();

    try {
      Cursor cursor = getCursor(database);

      if (null != details) {
        cursor.moveNextRows(details.getFirst());
      }

      List<T> list = Lists.newArrayList();
      Row row;

      while ((row = cursor.getNextRow()) != null) {
        T element = mapRow(row);

        if (predicate.test(element)) {
          list.add(element);
        }

        if (null != details && Objects.equals(list.size(), details.getCount())) {
          break;
        }
      }

      if (null != details && !Objects.equals(list.size(), details.getCount())) {
        logger.warn(
            "List contains less elements ({}) than required: {}",
            list.size(), details.getCount()
        );
      }

      return list;
    } catch (IOException exp) {
      throw new IllegalStateException("Can't retrieve data for table: " + getTableName(), exp);
    } finally {
      IOUtils.closeQuietly(database);
    }
  }

  /**
   * Find a row by field and value.
   */
  T find(String field, Object value) {
    Database database = getDatabase();

    try {
      Row row = findRow(database, ImmutableMap.of(field, value));
      return null == row ? null : mapRow(row);
    } finally {
      IOUtils.closeQuietly(database);
    }
  }

  abstract String getTableName();

  abstract T mapRow(Row row);

  Database getDatabase() {
    return handler.getDatabase();
  }

  private Table getTable(Database database) {
    try {
      return database.getTable(getTableName());
    } catch (IOException exp) {
      throw new IllegalStateException("Can't get table: " + getTableName(), exp);
    }
  }

  Cursor getCursor(Database database) {
    try {
      return CursorBuilder.createCursor(getTable(database));
    } catch (IOException exp) {
      throw new IllegalStateException("Can't create cursors for table: " + getTableName(), exp);
    }
  }

  private Row findRow(Database database, Map<String, Object> rowPattern) {
    try {
      return CursorBuilder.findRow(getTable(database), rowPattern);
    } catch (IOException exp) {
      throw new IllegalStateException("Can't find a row in table: " + getTableName(), exp);
    }
  }

  interface Details {

    int getFirst();

    int getCount();

  }

}

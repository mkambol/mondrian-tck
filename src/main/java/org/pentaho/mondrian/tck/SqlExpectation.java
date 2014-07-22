/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */
package org.pentaho.mondrian.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class SqlExpectation {

  final String query;
  final String[] columns;
  final String[] rows;
  final boolean partial;
  final int[] types;

  public SqlExpectation( String query, String[] columns, int[] types, String[] rows, boolean partial ) {
    this.query = query;
    this.columns = columns;
    this.types = types;
    this.rows = rows;
    this.partial = partial;
  }

  public void verify( ResultSet rs ) throws Exception {
    // Validate column names
    validateColumns( rs );
    // Validate rows
    validateRows( rs );
  }

  private void validateColumns( ResultSet rs ) throws Exception {
    if ( columns == null ) {
      return;
    }
    assertEquals(
      "Column names do not correspond to those expected.",
      columns,
      rsToColumns( rs ) );
  }

  private String[] rsToColumns( ResultSet rs ) throws Exception {
    final List<String> effectiveCols = new ArrayList<String>();
    final ResultSetMetaData rsm = rs.getMetaData();
    for ( int i = 1; i <= rsm.getColumnCount(); i++ ) {
      effectiveCols.add( rsm.getColumnName( i ) );
    }
    return effectiveCols.toArray( new String[ effectiveCols.size() ] );
  }

  private void validateRows( ResultSet rs ) throws Exception {
    final int nbCols = rs.getMetaData().getColumnCount();

    int rowNum = -1;
    while ( rs.next() ) {

      final StringBuilder curRow = new StringBuilder();

      // Check whether the RS has more rows but we were not expecting more
      if ( rows != null && ++rowNum >= rows.length && !partial ) {
        fail( "ResultSet returned more rows than expected" );
      }

      // Build a string representation of the row
      for ( int j = 1; j <= nbCols; j++ ) {
        if ( j > 1 ) {
          // Add a delimiter
          curRow.append( "|" );
        }

        final Object rawValue = rs.getObject( j );

        // Validate types
        if ( types != null ) {
          validateMetaType( rs );
          validateType( rs.getMetaData().getColumnName( j ), rawValue, types[j - 1] );
        }

        // Print the value to the buffer.
        curRow.append( String.valueOf( rawValue.toString() ) );
      }

      // Now validate that row
      if ( rows != null ) {
        assertEquals(
          "Row content doesn't match.",
          rows[rowNum],
          curRow.toString() );
      } else {
        // There was no row defined. we bail now.
        break;
      }

      // If there are still rows and we're not in partial mode, that's bad
      if ( !partial && rowNum < ( rows.length - 1 ) ) {
        fail( "Expected number of rows doesn't match the result." );
      }

      // Check whether we have reached the limit of the rows to validate
      if ( partial && rowNum == ( rows.length - 1 ) ) {
        break;
      }
    }
  }

  private void validateMetaType( ResultSet rs ) throws Exception {
    if ( types == null ) {
      return;
    }
    int columnCount = rs.getMetaData().getColumnCount();
    for ( int i = 1; i <= columnCount; i++ ) {
      assertEquals(
        "Wrong meta type for column " + rs.getMetaData().getColumnName( i )
        + ", expected meta type "
        + types[i - 1]
        + " but actual meta type was " + rs.getMetaData().getColumnType( i ),
        types[i - 1],
        rs.getMetaData().getColumnType( i ) );
    }
  }

  private void validateType( String colName, Object actual, int expected ) throws Exception {
    switch ( expected ) {
      case java.sql.Types.BIGINT:
        assertTrue(
          "Wrong type for column " + colName
          + ", expected type Long but object class was " + actual.getClass().getName(),
          actual.getClass().equals( Long.class ) );
        break;

      case java.sql.Types.DECIMAL:
        assertTrue(
          "Wrong type for column " + colName
          + ", expected type Double / BigDecimal but object class was " + actual.getClass().getName(),
          actual.getClass().equals( BigDecimal.class )
          || actual.getClass().equals( Double.class ) );
        break;

      case java.sql.Types.BOOLEAN:
        assertTrue(
          "Wrong type for column " + colName
          + ", expected type Boolean but object class was " + actual.getClass().getName(),
          actual.getClass().equals( Boolean.class ) );
        break;

      case java.sql.Types.INTEGER:
        assertTrue(
          "Wrong type for column " + colName
          + ", expected type Integer but object class was " + actual.getClass().getName(),
          actual.getClass().equals( Integer.class ) );
        break;

      case java.sql.Types.VARCHAR:
        assertTrue(
          "Wrong type for column " + colName
          + ", expected type String but object class was " + actual.getClass().getName(),
          actual.getClass().equals( String.class ) );
        break;

      default:
        throw new Exception( "Expected type check not implemented." );
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String query;
    private String[] columns;
    private String[] rows;
    private int[] types;
    private boolean partial = false;

    private Builder() {
    }

    /**
     * Sets the SQL query to run.
     * <p>(mandatory)
     */
    public Builder query( String query ) {
      this.query = query;
      return this;
    }

    /**
     * Sets the column names expected
     * <p>(optional)
     */
    public Builder columns( String... columns ) {
      this.columns = columns;
      return this;
    }

    /**
     * Sets the expected column types. Use values in {@link Types}.
     * <p>(optional)
     */
    public Builder types( int... types ) {
      this.types = types;
      return this;
    }

    /**
     * Sets the expected rows. The value delimiter is pipe ( '|' ).
     * <p>(optional)
     */
    public Builder rows( String... rows ) {
      this.rows = rows;
      return this;
    }

    /**
     * Sets whether the rows provided in {@link #rows(String[])} are only the
     * first rows of the result set and we didn't intend to validate them all.
     */
    public Builder partial() {
      this.partial  = true;
      return this;
    }

    public SqlExpectation build() {
      return new SqlExpectation( query, columns, types, rows, partial );
    }
  }
}
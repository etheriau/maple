/*
 * Copyright (c) 2009 Concurrent, Inc.
 *
 * This work has been released into the public domain
 * by the copyright holder. This applies worldwide.
 *
 * In case this is not legally possible:
 * The copyright holder grants any entity the right
 * to use this work for any purpose, without any
 * conditions, unless such conditions are required by law.
 */

package com.twitter.maple.jdbc;

import cascading.tuple.Tuple;
import com.twitter.maple.jdbc.db.DBWritable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TupleRecord implements DBWritable {
    private Tuple tuple;

    public TupleRecord() {
    }

    public TupleRecord( Tuple tuple ) {
        this.tuple = tuple;
    }

    public void setTuple( Tuple tuple ) {
        this.tuple = tuple;
    }

    public Tuple getTuple() {
        return tuple;
    }

    public void write( PreparedStatement statement ) throws SQLException {
        int statementParameterCount = statement.getParameterMetaData().getParameterCount();
        for( int i = 0, sz = Math.min( tuple.size(), statementParameterCount ); i < sz; i++ )
            statement.setObject( i + 1, tuple.getObject( i ) );
        for ( int i = tuple.size(), sz = statementParameterCount; i < sz; ++ i ) {
            statement.setObject( i + 1, null );
        }
    }

    public void readFields( ResultSet resultSet ) throws SQLException {
        tuple = new Tuple();

        for( int i = 0; i < resultSet.getMetaData().getColumnCount(); i++ ) {
          Object o = resultSet.getObject( i + 1 );
          // If this is a Date column and you are using any of the UTC-based fields in MySQL,
          // the timezone conversion wont be done if you put it back to MySQL. This allows it to work.
          if ( o instanceof java.sql.Date ) {
            o = new java.util.Date( ( (java.sql.Date)o ).getTime() );
          }
          tuple.add( o );
        }
    }

}

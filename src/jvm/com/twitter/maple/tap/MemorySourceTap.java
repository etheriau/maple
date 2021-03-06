package com.twitter.maple.tap;

import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.SourceTap;
import cascading.tap.Tap;
import cascading.tap.hadoop.io.HadoopTupleEntrySchemeIterator;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntryIterator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class MemorySourceTap extends SourceTap<JobConf, RecordReader<TupleWrapper, NullWritable>>
    implements Serializable {
    private static final long serialVersionUID = -3883105721794069739L;

    public static class MemorySourceScheme
        extends Scheme<JobConf, RecordReader<TupleWrapper, NullWritable>, Void, Object[], Void> {

        private static final long serialVersionUID = 919689539720607932L;

        private transient List<Tuple> tuples;
        private final String id;

        public MemorySourceScheme(List<Tuple> tuples, Fields fields, String id) {
            super(fields);
            assert tuples != null;
            this.tuples = tuples;
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public List<Tuple> getTuples() {
            return this.tuples;
        }

        @Override
        public void sourceConfInit(FlowProcess<JobConf> flowProcess,
            Tap<JobConf, RecordReader<TupleWrapper, NullWritable>, Void> tap, JobConf conf) {
            FileInputFormat.setInputPaths(conf, this.id);
            conf.setInputFormat(TupleMemoryInputFormat.class);
            TupleMemoryInputFormat.storeTuples(conf, TupleMemoryInputFormat.TUPLES_PROPERTY, this.tuples);
        }

        @Override
        public void sinkConfInit(FlowProcess<JobConf> flowProcess,
            Tap<JobConf, RecordReader<TupleWrapper, NullWritable>, Void> tap, JobConf conf) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sourcePrepare( FlowProcess<JobConf> flowProcess, SourceCall<Object[],
            RecordReader<TupleWrapper, NullWritable>> sourceCall ) {
            sourceCall.setContext( new Object[ 2 ] );

            sourceCall.getContext()[ 0 ] = sourceCall.getInput().createKey();
            sourceCall.getContext()[ 1 ] = sourceCall.getInput().createValue();
        }

        @Override
        public boolean source(FlowProcess<JobConf> flowProcess, SourceCall<Object[],
            RecordReader<TupleWrapper, NullWritable>> sourceCall) throws IOException {
            TupleWrapper key = (TupleWrapper) sourceCall.getContext()[ 0 ];
            NullWritable value = (NullWritable) sourceCall.getContext()[ 1 ];

            boolean result = sourceCall.getInput().next( key, value );

            if( !result )
                return false;

            sourceCall.getIncomingEntry().setTuple(key.tuple);
            return true;
        }

        @Override
        public void sourceCleanup( FlowProcess<JobConf> flowProcess, SourceCall<Object[],
            RecordReader<TupleWrapper, NullWritable>> sourceCall ) {
            sourceCall.setContext( null );
        }

        @Override
        public void sink(FlowProcess<JobConf> flowProcess, SinkCall<Void, Void> sinkCall ) throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

    }

    private final String id;

    public MemorySourceTap(List<Tuple> tuples, Fields fields) {
        super(new MemorySourceScheme(tuples, fields, "/" + UUID.randomUUID().toString()));
        this.id = ((MemorySourceScheme) this.getScheme()).getId();
    }

    @Override
    public String getIdentifier() {
        return getPath().toString();
    }

    public Path getPath() {
        return new Path(id);
    }

    @Override
    public boolean resourceExists( JobConf conf ) throws IOException {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if(!getClass().equals(object.getClass())) {
            return false;
        }
        MemorySourceTap other = (MemorySourceTap) object;
        return id.equals(other.id);
    }

    @Override
    public TupleEntryIterator openForRead( FlowProcess<JobConf> flowProcess, RecordReader<TupleWrapper,
        NullWritable> input ) throws IOException {
        // input may be null when this method is called on the client side or cluster side when accumulating
        // for a HashJoin
        return new HadoopTupleEntrySchemeIterator( flowProcess, this, input );
    }

    @Override
    public long getModifiedTime( JobConf conf ) throws IOException {
        return System.currentTimeMillis();
    }
}

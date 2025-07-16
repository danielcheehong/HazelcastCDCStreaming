package com.myhazelcast.cdc;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.cdc.DebeziumCdcSources;
import oracle.jdbc.pool.OracleDataSource;

public class CdcToOracleJob {
    public static void main(String[] args) throws Exception {
        Pipeline p = Pipeline.create();

        p.readFrom(DebeziumCdcSources.sqlServer()
                        .host("sqlserver").port(1433)
                        .username("sa").password("YourStrong!Passw0rd")
                        .database("testdb").tableWhitelist("dbo.Customers"))
                .map(record -> {
                    int id = record.value().getInt("Id");
                    String name = record.value().getString("Name");
                    java.sql.Timestamp createdAt = record.value().getTimestamp("CreatedAt");
                    int counter = id + 1;  // simple processing
                    return new Object[]{ id, name, createdAt, counter };
                })
                .writeTo(SinkBuilder
                        .sinkBuilder("oracle-sink",
                                ctx -> {
                                    OracleDataSource ds = new OracleDataSource();
                                    ds.setURL("jdbc:oracle:thin:@//oracle:1521/XE");
                                    ds.setUser("SYS");
                                    ds.setPassword("YourStrong!Passw0rd");
                                    return ds;
                                },
                                (stmt, item) -> {
                                    stmt.setInt(1, (Integer) item[0]);
                                    stmt.setString(2, (String) item[1]);
                                    stmt.setTimestamp(3, (java.sql.Timestamp) item[2]);
                                    stmt.setInt(4, (Integer) item[3]);
                                })
                        .setQuery(
                                "MERGE INTO Customers c " +
                                        "USING (SELECT ? AS Id, ? AS Name, ? AS CreatedAt, ? AS Cnt FROM dual) vals " +
                                        "ON (c.Id = vals.Id) " +
                                        "WHEN MATCHED THEN UPDATE SET c.Name = vals.Name, c.Cnt = vals.Cnt " +
                                        "WHEN NOT MATCHED THEN INSERT (Id, Name, CreatedAt, Cnt) " +
                                        "VALUES (vals.Id, vals.Name, vals.CreatedAt, vals.Cnt)")
                        .build()
                );

        JetInstance jet = Jet.bootstrappedInstance();
        jet.newJob(p).join();
        Jet.shutdownAll();
    }
}


package com.myhazelcast.cdc;

import java.sql.Timestamp;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.cdc.DebeziumCdcSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SinkBuilder;
import oracle.jdbc.pool.OracleDataSource;
import com.hazelcast.jet.pipeline.StreamSource;
import io.debezium.connector.sqlserver.SqlServerConnector;
import com.hazelcast.jet.cdc.ChangeRecord;

public class CdcToOracleJob {
    public static void main(String[] args) throws Exception {
        Pipeline p = Pipeline.create();

        // 1. Create the generic Debezium source for SQL Server
        StreamSource<ChangeRecord> sqlServerSource = DebeziumCdcSources
                .debezium("sqlserver-connector", SqlServerConnector.class)      // connector class :contentReference[oaicite:0]{index=0}
                .setProperty("database.server.name", "localhost")               // logical name for offsets & topics
                .setProperty("database.hostname", "sqlserver")                  // host (Docker service name or IP)
                .setProperty("database.port",     "1433")                       // SQL Server port
                .setProperty("database.user",     "sa")                         // SQL Server user
                .setProperty("database.password", "YourStrong!Passw0rd")        // SQL Server password
                .setProperty("database.dbname",   "testdb")                     // database to capture
                .setProperty("table.include.list","dbo.Customers")              // table(s) to capture
                .setProperty("snapshot.mode",     "initial")                    // take snapshot on startup
                .build();

        p.readFrom(sqlServerSource)
                .withNativeTimestamps(0)
                .map(record -> {
                    int id = (Integer) record.value().toMap().get("Id");
                    String name = (String) record.value().toMap().get("Name");
                    java.sql.Timestamp createdAt = (java.sql.Timestamp) record.value().toMap().get("CreatedAt");
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
                                    return ds.getConnection().prepareStatement(
                                            "MERGE INTO Customers c " +
                                                    "USING (SELECT ? AS Id, ? AS Name, ? AS CreatedAt, ? AS Cnt FROM dual) vals " +
                                                    "ON (c.Id = vals.Id) " +
                                                    "WHEN MATCHED THEN UPDATE SET c.Name = vals.Name, c.Cnt = vals.Cnt " +
                                                    "WHEN NOT MATCHED THEN INSERT (Id, Name, CreatedAt, Cnt) " +
                                                    "VALUES (vals.Id, vals.Name, vals.CreatedAt, vals.Cnt)"
                                    );
                                })
                        .receiveFn((stmt, item) -> {
                                    Object[] row = (Object[]) item; // Cast item to Object[]
                                    stmt.setInt(1, (Integer) row[0]);
                                    stmt.setString(2, (String)  row[1]);
                                    stmt.setTimestamp(3, (Timestamp) row[2]);
                                    stmt.setInt(4, (Integer) row[3]);
                                    stmt.executeUpdate();                  // << you must execute
                        })
                        .build()
                );

        HazelcastInstance hz = Hazelcast.bootstrappedInstance();
        hz.getJet().newJob(p).join();

    }
}


package com.databend.jdbc;

import com.databend.client.StageAttachment;
import org.junit.jupiter.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestPrepareStatement {
    @BeforeTest
    public void setUp()
            throws SQLException {
        // create table
        Connection c = Utils.createConnection();
        System.out.println("-----------------");
        System.out.println("drop all existing test table");
        c.createStatement().execute("drop table if exists test_prepare_statement");
        c.createStatement().execute("drop table if exists test_prepare_time");
        c.createStatement().execute("drop table if exists objects_test1");
        c.createStatement().execute("drop table if exists binary1");
        c.createStatement().execute("drop table if exists test_prepare_statement_null");
        c.createStatement().execute("create table test_prepare_statement (a int, b string)");
        c.createStatement().execute("create table test_prepare_statement_null (a int, b string)");
        c.createStatement().execute("create table test_prepare_time(a DATE, b TIMESTAMP)");
        // json data
        c.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS objects_test1(id TINYINT, obj VARIANT, d TIMESTAMP, s String, arr ARRAY(INT64)) Engine = Fuse");
        // Binary data
        c.createStatement().execute("create table IF NOT EXISTS binary1 (a binary);");
    }

    @Test(groups = "IT")
    public void TestBatchInsert() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);

        PreparedStatement ps = c.prepareStatement("insert into test_prepare_statement values");
        ps.setInt(1, 1);
        ps.setString(2, "a");
        ps.addBatch();
        ps.setInt(1, 2);
        ps.setString(2, "b");
        ps.addBatch();
        System.out.println("execute batch insert");
        int[] ans = ps.executeBatch();
        Assert.assertEquals(ans.length, 2);
        Assert.assertEquals(ans[0], 1);
        Assert.assertEquals(ans[1], 1);
        Statement statement = c.createStatement();

        System.out.println("execute select");
        statement.execute("SELECT * from test_prepare_statement");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            System.out.println(r.getString(2));
        }
    }

    @Test(groups = "IT")
    public void TestBatchInsertWithNULL() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);

        PreparedStatement ps = c.prepareStatement("insert into test_prepare_statement_null values");
        ps.setInt(1, 1);
        ps.setNull(2, Types.NULL);
        ps.addBatch();
        ps.setInt(1, 2);
        ps.setObject(2, null, Types.NULL);
        ps.addBatch();
        System.out.println("execute batch insert");
        int[] ans = ps.executeBatch();
        Assert.assertEquals(ans.length, 2);
        Assert.assertEquals(ans[0], 1);
        Assert.assertEquals(ans[1], 1);
        Statement statement = c.createStatement();

        System.out.println("execute select");
        statement.execute("SELECT * from test_prepare_statement_null");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            Assert.assertEquals(r.getObject(2), null);
        }
    }

    @Test(groups = "IT")
    public void TestConvertSQLWithBatchValues() throws SQLException {
        List<String[]> batchValues = new ArrayList<>();
        // Add string arrays to batchValues
        String[] values1 = { "1" };
        String[] values2 = { "2" };
        batchValues.add(values1);
        batchValues.add(values2);

        String originalSql = "delete from table where id = ?";
        String expectedSql = "delete from table where id = 1;\ndelete from table where id = 2;\n";
        Assert.assertEquals(DatabendPreparedStatement.convertSQLWithBatchValues(originalSql, batchValues), expectedSql);

        List<String[]> batchValues1 = new ArrayList<>();
        // Add string arrays to batchValues
        String[] values3 = { "1", "2" };
        String[] values4 = { "3", "4" };
        batchValues1.add(values3);
        batchValues1.add(values4);

        String originalSql1 = "delete from table where id = ? and uuid = ?";
        String expectedSql1 = "delete from table where id = 1 and uuid = 2;\ndelete from table where id = 3 and uuid = 4;\n";
        Assert.assertEquals(DatabendPreparedStatement.convertSQLWithBatchValues(originalSql1, batchValues1),
                expectedSql1);
    }

    @Test(groups = "IT")
    public void TestBatchDelete() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);

        PreparedStatement ps = c.prepareStatement("insert into test_prepare_statement values");
        ps.setInt(1, 1);
        ps.setString(2, "b");
        ps.addBatch();
        ps.setInt(1, 3);
        ps.setString(2, "b");
        ps.addBatch();
        System.out.println("execute batch insert");
        int[] ans = ps.executeBatch();
        Assert.assertEquals(ans.length, 2);
        Assert.assertEquals(ans[0], 1);
        Assert.assertEquals(ans[1], 1);
        Statement statement = c.createStatement();

        System.out.println("execute select");
        statement.execute("SELECT * from test_prepare_statement");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            System.out.println(r.getString(2));
        }

        PreparedStatement deletePs = c.prepareStatement("delete from test_prepare_statement where a = ?");
        deletePs.setInt(1, 1);
        deletePs.addBatch();
        int[] ansDel = deletePs.executeBatch();
        System.out.println(ansDel);

        System.out.println("execute select");
        statement.execute("SELECT * from test_prepare_statement");
        ResultSet r1 = statement.getResultSet();

        int resultCount = 0;
        while (r1.next()) {
            resultCount += 1;
        }
        Assert.assertEquals(resultCount, 1);
    }

    @Test(groups = "IT")
    public void TestBatchInsertWithTime() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);
        PreparedStatement ps = c.prepareStatement("insert into test_prepare_time values");
        ps.setDate(1, Date.valueOf("2020-01-10"));
        ps.setTimestamp(2, Timestamp.valueOf("1983-07-12 21:30:55.888"));
        ps.addBatch();
        ps.setDate(1, Date.valueOf("1970-01-01"));
        ps.setTimestamp(2, Timestamp.valueOf("1970-01-01 00:00:01"));
        ps.addBatch();
        ps.setDate(1, Date.valueOf("2021-01-01"));
        ps.setTimestamp(2, Timestamp.valueOf("1970-01-01 00:00:01.234"));
        int[] ans = ps.executeBatch();
        Statement statement = c.createStatement();

        System.out.println("execute select on time");
        statement.execute("SELECT * from test_prepare_time");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getDate(1).toString());
            System.out.println(r.getTimestamp(2).toString());
        }
    }

    @Test(groups = "IT")
    public void TestBatchInsertWithComplexDataType() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);
        PreparedStatement ps = c.prepareStatement("insert into objects_test1 values");
        ps.setInt(1, 1);
        ps.setString(2, "{\"a\": 1,\"b\": 2}");
        ps.setTimestamp(3, Timestamp.valueOf("1983-07-12 21:30:55.888"));
        ps.setString(4, "hello world, 你好");
        ps.setString(5, "[1,2,3,4,5]");
        ps.addBatch();
        int[] ans = ps.executeBatch();
        Statement statement = c.createStatement();

        System.out.println("execute select on object");
        statement.execute("SELECT * from objects_test1");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            System.out.println(r.getString(2));
            System.out.println(r.getTimestamp(3).toString());
            System.out.println(r.getString(4));
            System.out.println(r.getString(5));
        }
    }

    @Test(groups = "IT")
    public void TestBatchInsertWithComplexDataTypeWithPresignAPI() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);
        PreparedStatement ps = c.prepareStatement("insert into objects_test1 values");
        ps.setInt(1, 1);
        ps.setString(2, "{\"a\": 1,\"b\": 2}");
        ps.setTimestamp(3, Timestamp.valueOf("1983-07-12 21:30:55.888"));
        ps.setString(4, "hello world, 你好");
        ps.setString(5, "[1,2,3,4,5]");
        ps.addBatch();
        int[] ans = ps.executeBatch();
        Statement statement = c.createStatement();

        System.out.println("execute select on object");
        statement.execute("SELECT * from objects_test1");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            System.out.println(r.getString(2));
            System.out.println(r.getTimestamp(3).toString());
            System.out.println(r.getString(4));
            System.out.println(r.getString(5));
        }
    }

    @Test(groups = "IT")
    public void TestBatchInsertWithComplexDataTypeWithPresignAPIPlaceHolder() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);
        PreparedStatement ps = c.prepareStatement("insert into objects_test1 values(?,?,?,?,?)");
        for (int i = 0; i < 500000; i++) {
            ps.setInt(1, 2);
            ps.setString(2, "{\"a\": 1,\"b\": 2}");
            ps.setTimestamp(3, Timestamp.valueOf("1983-07-12 21:30:55.888"));
            ps.setString(4, "hello world, 你好");
            ps.setString(5, "[1,2,3,4,5]");
            ps.addBatch();
        }

        int[] ans = ps.executeBatch();
        Statement statement = c.createStatement();

        System.out.println("execute select on object");
        statement.execute("SELECT * from objects_test1");
        ResultSet r = statement.getResultSet();
        int count = 0;
        while (r.next()) {
            count++;
        }
        System.out.println(count);
    }

    @Test(groups = "IT")
    public void TestBatchReplaceInto() throws SQLException {
        Connection c = Utils.createConnection();
        c.setAutoCommit(false);
        PreparedStatement ps1 = c.prepareStatement("insert into test_prepare_statement values");
        ps1.setInt(1, 1);
        ps1.setInt(2, 2);
        ps1.addBatch();
        ps1.executeBatch();

        PreparedStatement ps = c.prepareStatement("replace into test_prepare_statement on(a) values");
        ps.setInt(1, 1);
        ps.setString(2, "a");
        ps.addBatch();
        ps.setInt(1, 3);
        ps.setString(2, "b");
        ps.addBatch();
        System.out.println("execute batch replace into");
        int[] ans = ps.executeBatch();
        Assert.assertEquals(ans.length, 2);
        Assert.assertEquals(ans[0], 1);
        Assert.assertEquals(ans[1], 1);
        Statement statement = c.createStatement();

        System.out.println("execute select");
        statement.execute("SELECT * from test_prepare_statement");
        ResultSet r = statement.getResultSet();

        while (r.next()) {
            System.out.println(r.getInt(1));
            System.out.println(r.getString(2));
        }
        // truncate table
        c.createStatement().execute("truncate table test_prepare_statement");
    }

    @Test
    public void testPrepareStatementExecute() throws SQLException {
        Connection conn = Utils.createConnection();
        conn.createStatement().execute("delete from test_prepare_statement");
        String insertSql = "insert into test_prepare_statement values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.setInt(1, 1);
            statement.setString(2, "b");
            statement.execute();
        }
        String updateSql = "update test_prepare_statement set b = ? where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(updateSql)) {
            statement.setString(1, "c");
            statement.setInt(2, 1);
            statement.execute();
        }

        String selectSql = "select * from test_prepare_statement";
        try {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(selectSql);
            while (rs.next()) {
                Assert.assertEquals("c", rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String deleteSql = "delete from test_prepare_statement where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(deleteSql)) {
            statement.setInt(1, 1);
            statement.execute();
        }

        try {
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(selectSql);
            Assert.assertEquals(0, rs.getRow());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // truncate table
        conn.createStatement().execute("truncate table test_prepare_statement");
    }

    @Test
    public void testUpdateSetNull() throws SQLException {
        Connection conn = Utils.createConnection();
        String sql = "insert into test_prepare_statement values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, 1);
            statement.setString(2, "b");
            statement.addBatch();
            int[] result = statement.executeBatch();
            System.out.println(result);
            Assertions.assertEquals(1, result.length);
        }
        String updateSQL = "update test_prepare_statement set b = ? where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(updateSQL)) {
            statement.setInt(2, 1);
            statement.setNull(1, Types.NULL);
            int result = statement.executeUpdate();
            System.out.println(result);
            Assertions.assertEquals(1, result);
        }
        try (PreparedStatement statement = conn
                .prepareStatement("select a, regexp_replace(b, '\\d', '*') from test_prepare_statement where a = ?")) {
            statement.setInt(1, 1);
            ResultSet r = statement.executeQuery();
            while (r.next()) {
                Assertions.assertEquals(1, r.getInt(1));
                Assertions.assertEquals(null, r.getString(2));
            }
        }
        String insertSelectSql = "insert overwrite test_prepare_statement select * from test_prepare_statement";
        try (PreparedStatement statement = conn.prepareStatement(insertSelectSql)) {
            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // truncate table
        conn.createStatement().execute("truncate table test_prepare_statement");
    }

    @Test
    public void testUpdateStatement() throws SQLException {
        Connection conn = Utils.createConnection();
        String sql = "insert into test_prepare_statement values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, 1);
            statement.setString(2, "b");
            statement.addBatch();
            int[] result = statement.executeBatch();
            System.out.println(result);
            Assertions.assertEquals(1, result.length);
        }
        String updateSQL = "update test_prepare_statement set b = ? where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(updateSQL)) {
            statement.setInt(2, 1);
            statement.setObject(1, "c'c");
            int result = statement.executeUpdate();
            System.out.println(result);
            Assertions.assertEquals(1, result);
        }
        try (PreparedStatement statement = conn
                .prepareStatement("select a, regexp_replace(b, '\\d', '*') from test_prepare_statement where a = ?")) {
            statement.setInt(1, 1);
            ResultSet r = statement.executeQuery();
            while (r.next()) {
                Assertions.assertEquals(1, r.getInt(1));
                Assertions.assertEquals("c'c", r.getString(2));
            }

            // truncate table
            conn.createStatement().execute("truncate table test_prepare_statement");
        }
    }

    @Test
    public void testAllPreparedStatement() throws SQLException {
        String sql = "insert into test_prepare_statement values (?,?)";
        Connection conn = Utils.createConnection();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, 1);
            statement.setString(2, "b");
            statement.addBatch();
            statement.setInt(1, 2);
            statement.setString(2, "z");
            statement.addBatch();
            statement.setInt(1, 3);
            statement.setString(2, "x");
            statement.addBatch();
            statement.setInt(1, 4);
            statement.setString(2, "dd");
            statement.addBatch();
            statement.setInt(1, 5);
            statement.setString(2, "ddd");
            statement.addBatch();
            int[] result = statement.executeBatch();
            System.out.println(result);
            Assertions.assertEquals(5, result.length);
        }
        String updateSQL = "update test_prepare_statement set b = ? where b = ?";
        try (PreparedStatement statement = conn.prepareStatement(updateSQL)) {
            statement.setString(1, "c");
            statement.setString(2, "b");
            int result = statement.executeUpdate();
            Assertions.assertEquals(1, result);
        }
        try (PreparedStatement statement = conn
                .prepareStatement("select a, regexp_replace(b, '\\d', '*') from test_prepare_statement where b = ?")) {
            statement.setString(1, "c");
            ResultSet r = statement.executeQuery();
            while (r.next()) {
                Assertions.assertEquals(1, r.getInt(1));
                Assertions.assertEquals("c", r.getString(2));
            }
        }
        String replaceIntoSQL = "replace into test_prepare_statement on(a) values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(replaceIntoSQL)) {
            statement.setInt(1, 1);
            statement.setString(2, "d");
            statement.addBatch();
            int[] result = statement.executeBatch();
        }
        ResultSet r2 = conn.createStatement().executeQuery("select * from test_prepare_statement");
        while (r2.next()) {
            System.out.println(r2.getInt(1));
            System.out.println(r2.getString(2));
        }

        String deleteSQL = "delete from test_prepare_statement where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(deleteSQL)) {
            statement.setInt(1, 1);
            boolean result = statement.execute();
            System.out.println(result);
        }

        String deleteSQLVarchar = "delete from test_prepare_statement where b = ?";
        try (PreparedStatement statement = conn.prepareStatement(deleteSQLVarchar)) {
            statement.setString(1, "1");
            int result = statement.executeUpdate();
            System.out.println(result);
        }

        ResultSet r3 = conn.createStatement().executeQuery("select * from test_prepare_statement");
        Assert.assertEquals(0, r3.getRow());
        while (r3.next()) {
            // noting print
            System.out.println(r3.getInt(1));
            System.out.println(r3.getString(2));
        }
        // truncate table
        conn.createStatement().execute("truncate table test_prepare_statement");
    }

    @Test
    public void shouldBuildStageAttachmentWithFileFormatOptions() throws SQLException {
        Connection conn = Utils.createConnection();
        Assertions.assertEquals("", conn.unwrap(DatabendConnection.class).binaryFormat());
        StageAttachment stageAttachment = DatabendPreparedStatement.buildStateAttachment((DatabendConnection) conn,
                "stagePath");

        Assertions.assertFalse(stageAttachment.getFileFormatOptions().containsKey("binary_format"));
        Assertions.assertTrue(stageAttachment.getFileFormatOptions().containsKey("type"));
        Assertions.assertEquals("true", stageAttachment.getCopyOptions().get("PURGE"));
        Assertions.assertEquals("\\N", stageAttachment.getCopyOptions().get("NULL_DISPLAY"));
    }

    @Test
    public void testSelectWithClusterKey() throws SQLException {
        Connection conn = Utils.createConnection();
        conn.createStatement().execute("drop table if exists default.test_clusterkey");
        conn.createStatement().execute("create table default.test_clusterkey (a int, b string)");
        String insertSql = "insert into default.test_clusterkey values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.setInt(1, 1);
            statement.setString(2, "b");
            statement.addBatch();
            statement.setInt(1, 2);
            statement.setString(2, "c");
            statement.addBatch();
            int[] result = statement.executeBatch();
            System.out.println(result);
            Assertions.assertEquals(2, result.length);
        }
        conn.createStatement().execute("alter table default.test_clusterkey cluster by (a)");
        String selectSQL = "select * from clustering_information('default','test_clusterkey')";
        try (PreparedStatement statement = conn.prepareStatement(selectSQL)) {
            ResultSet rs = statement.executeQuery();
            int rows = 0;
            while (rs.next()) {
                Assertions.assertEquals("linear", rs.getString(2));
                rows += 1;
            }
            Assertions.assertEquals(1, rows);
        }
    }

    @Test
    public void testEncodePass() throws SQLException {
        Connection conn = Utils.createConnection();
        conn.createStatement().execute("create user if not exists 'u01' identified by 'mS%aFRZW*GW';");
        conn.createStatement().execute("GRANT ALL PRIVILEGES ON default.* TO 'u01'@'%'");
        Properties p = new Properties();
        p.setProperty("user", "u01");
        p.setProperty("password", "mS%aFRZW*GW");

        Connection conn2 = Utils.createConnection();
        conn2.createStatement().execute("select 1");
        conn.createStatement().execute("drop user if exists 'u01'");
    }

    @Test
    public void testExecuteUpdate() throws SQLException {
        Connection conn = Utils.createConnection();
        conn.createStatement().execute("delete from test_prepare_statement");

        String insertSql = "insert into test_prepare_statement values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.setInt(1, 1);
            statement.setString(2, "a");
            statement.executeUpdate();

            statement.setInt(1, 2);
            statement.setString(2, "b");
            statement.executeUpdate();

            statement.setInt(1, 3);
            statement.setString(2, "c");
            statement.executeUpdate();
        }

        String updateSingleSql = "update test_prepare_statement set b = ? where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(updateSingleSql)) {
            statement.setString(1, "x");
            statement.setInt(2, 1);
            int updatedRows = statement.executeUpdate();
            Assertions.assertEquals(1, updatedRows, "only update one row");
        }

        String updateMultiSql = "update test_prepare_statement set b = ? where a in (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(updateMultiSql)) {
            statement.setString(1, "y");
            statement.setInt(2, 2);
            statement.setInt(3, 3);
            int updatedRows = statement.executeUpdate();
            Assertions.assertEquals(2, updatedRows, "should update two rows");
        }

        String updateAndSql = "update test_prepare_statement set b = ? where ((a = ?)) and (b = ?)";
        try (PreparedStatement statement = conn.prepareStatement(updateAndSql)) {
            statement.setString(1, "z");
            statement.setInt(2, 2);
            statement.setString(3, "y");
            int updatedRows = statement.executeUpdate();
            Assertions.assertEquals(1, updatedRows, "should update one row with and condition");
        }

        String deleteSql = "delete from test_prepare_statement where a = ?";
        try (PreparedStatement statement = conn.prepareStatement(deleteSql)) {
            statement.setInt(1, 1);
            int deletedRows = statement.executeUpdate();
            Assertions.assertEquals(1, deletedRows, "should delete one row");
        }

        ResultSet rs = conn.createStatement().executeQuery("select * from test_prepare_statement order by a");
        int count = 0;
        while (rs.next()) {
            count++;
            if (count == 1) {
                Assertions.assertEquals(2, rs.getInt(1));
                Assertions.assertEquals("z", rs.getString(2));
            } else if (count == 2) {
                Assertions.assertEquals(3, rs.getInt(1));
                Assertions.assertEquals("y", rs.getString(2));
            }
        }
        Assertions.assertEquals(2, count, "should have two rows left in the table");
        // Clean up
        conn.createStatement().execute("delete from test_prepare_statement");
    }

    @Test

    public void testInsertWithSelect() throws SQLException {
        Connection conn = Utils.createConnection();
        conn.createStatement().execute("delete from test_prepare_statement");

        String insertSql = "insert into test_prepare_statement select a, b from test_prepare_statement where b = ?";
        try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.setString(1, "a");
            int insertedRows = statement.executeUpdate();
            Assertions.assertEquals(0, insertedRows, "should not insert any rows as the table is empty");
        }

        // Insert some data
        String insertDataSql = "insert into test_prepare_statement values (?,?)";
        try (PreparedStatement statement = conn.prepareStatement(insertDataSql)) {
            statement.setInt(1, 1);
            statement.setString(2, "a");
            statement.executeUpdate();

            statement.setInt(1, 2);
            statement.setString(2, "b");
            statement.executeUpdate();
        }

        // Now try to insert again with select
        try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.setString(1, "a");
            int insertedRows = statement.executeUpdate();
            Assertions.assertEquals(1, insertedRows, "should insert two rows from the select");
        }

        ResultSet rs = conn.createStatement().executeQuery("select * from test_prepare_statement order by a");
        int count = 0;
        while (rs.next()) {
            count++;
        }
        Assertions.assertEquals(3, count, "should have four rows in the table after insert with select");

        // Clean up
        conn.createStatement().execute("delete from test_prepare_statement");
    }

}

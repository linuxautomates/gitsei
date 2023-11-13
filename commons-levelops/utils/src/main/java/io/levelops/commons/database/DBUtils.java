package io.levelops.commons.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class DBUtils {
    @SuppressWarnings("rawtypes")
    public static Array createArray(final Connection conn, ArrayWrapper arrayWrapper) throws SQLException {
        String type = arrayWrapper.getType();
        Array result = null;
        switch (type){
            case "uuid":
                result = conn.createArrayOf("uuid",  arrayWrapper.getData().toArray(new UUID[0]));
                break;
            case "varchar":
                result = conn.createArrayOf("varchar", arrayWrapper.getData().toArray());
                break;
            case "int":
                result = conn.createArrayOf("int", ((List)arrayWrapper.getData()).toArray());
                break;
            case "text":
                result = conn.createArrayOf("text", ((List)arrayWrapper.getData()).toArray());
                break;
            case "bigint":
                result = conn.createArrayOf("bigint", ((List)arrayWrapper.getData()).toArray());
                break;
            default:
                throw new RuntimeException("Unsupported type : " + type);
        }
        return result;
    }

    public static Object processArrayValues(final Connection conn, Object obj) throws SQLException {
        if (obj instanceof ArrayWrapper) {
            return DBUtils.createArray(conn, (ArrayWrapper) obj);
        }
        return obj;
    }
}

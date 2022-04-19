package com.baomidou.dynamic.datasource;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.sql.SQLException;

public class ValidTest {

    @Test
    public void validFalse() {
        Assert.assertThrows(SQLException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                DynamicDataSourceUtils.valid("ss", "root", "123456");
            }
        });
    }

    @Test
    public void validTrue() {
        try {
            boolean valid = DynamicDataSourceUtils.valid("jdbc:mysql://110.40.253.205:3306/seata_order?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&useSSL=false",
                    "root", "123456");
            Assert.assertTrue(valid);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

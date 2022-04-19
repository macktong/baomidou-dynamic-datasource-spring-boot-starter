/*
 * Copyright © 2018 organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.dynamic.datasource.toolkit;

import lombok.NonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 多数据源工具类
 *
 * @author TaoYu
 * @since 3.5.2
 */
public final class DynamicDataSourceUtils {

    /**
     * 验证数据库配置是否可用
     *
     * @param url      url地址
     * @param userName 用户
     * @param password 渺茫
     * @return 是否可用
     */
    public static boolean valid(@NonNull String url, @NonNull String userName, @NonNull String password) throws SQLException {
        return valid(url, userName, password, null);
    }

    /**
     * 验证数据库配置是否可用
     *
     * @param url      url地址
     * @param userName 用户
     * @param password 渺茫
     * @param validSql 验证的sql 如select 1
     * @return 是否可用
     */
    public static boolean valid(@NonNull String url, @NonNull String userName, @NonNull String password, String validSql) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection(url, userName, password);
            statement = connection.createStatement();
            if (validSql != null && validSql.length() > 0) {
                statement.execute(validSql);
            }
            return true;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignore) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignore) {
            }
        }
    }
}

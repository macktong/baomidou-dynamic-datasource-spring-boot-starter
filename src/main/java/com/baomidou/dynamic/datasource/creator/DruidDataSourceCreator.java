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
package com.baomidou.dynamic.datasource.creator;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.logging.CommonsLogFilter;
import com.alibaba.druid.filter.logging.Log4j2Filter;
import com.alibaba.druid.filter.logging.Log4jFilter;
import com.alibaba.druid.filter.logging.Slf4jLogFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.baomidou.dynamic.datasource.exception.ErrorCreateDataSourceException;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidLogConfigUtil;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidStatConfigUtil;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidWallConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.baomidou.dynamic.datasource.support.DdConstants.DRUID_DATASOURCE;

/**
 * Druid数据源创建器
 *
 * @author TaoYu
 * @since 2020/1/21
 */
@Slf4j
public class DruidDataSourceCreator extends AbstractDataSourceCreator implements DataSourceCreator {

    private static final Map<String, Method> SETTER_METHODS = new HashMap<>();

    static {
        Method[] methods = DruidDataSource.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<?> parameterType = parameterTypes[0];
                if (String.class == parameterType || ClassUtils.isPrimitiveOrWrapper(parameterType)) {
                    String realField = methodName.substring(3);
                    SETTER_METHODS.put(realField.toUpperCase(), method);
                } else {

                }
            }
        }
    }

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Override
    public DataSource doCreateDataSource(DataSourceProperty dataSourceProperty) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUsername(dataSourceProperty.getUsername());
        dataSource.setPassword(dataSourceProperty.getPassword());
        dataSource.setUrl(dataSourceProperty.getUrl());
        dataSource.setName(dataSourceProperty.getPoolName());
        String driverClassName = dataSourceProperty.getDriverClassName();
        if (!StringUtils.isEmpty(driverClassName)) {
            dataSource.setDriverClassName(driverClassName);
        }
        Map<String, Object> druidConfig = properties.getDruid();
        Map<String, Object> itemDruidConfig = dataSourceProperty.getDruid();
        druidConfig.putAll(itemDruidConfig);
        Map<String, Map> filterMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : druidConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                filterMap.put(key, (Map) value);
                continue;
            }
            String s = key.replaceAll("-", "").toUpperCase();
            Method method = SETTER_METHODS.get(s);
            if (method != null) {
                try {
                    method.invoke(dataSource, value);
                    log.debug("druid 设置参数[{}] 值[{}] 成功", key, value);
                } catch (Exception e) {
                    log.warn("druid 设置参数[{}] 值[{}] 失败", key, value);
                }
            } else {
                log.warn("druid 设置参数[{}] 值[{}] 不存在此参数", key, value);
            }
        }
//
        String filters = (String) properties.getDruid().get("filters");
        List<Filter> proxyFilters = this.initFilters(dataSourceProperty, filters);
        dataSource.setProxyFilters(proxyFilters);
//
//        //连接参数单独设置
//        dataSource.setConnectProperties(config.getConnectionProperties());

        if (Boolean.FALSE.equals(dataSourceProperty.getLazy())) {
            try {
                dataSource.init();
            } catch (SQLException e) {
                throw new ErrorCreateDataSourceException("druid create error", e);
            }
        }
        return dataSource;
    }

    private List<Filter> initFilters(DataSourceProperty dataSourceProperty, String filters) {
        List<Filter> proxyFilters = new ArrayList<>(2);
        if (!StringUtils.isEmpty(filters)) {
            String[] filterItems = filters.split(",");
            for (String filter : filterItems) {
                Map<String, Object> itemMap = (Map<String, Object>) dataSourceProperty.getDruid().get(filter);
                Map<String, Object> globalMap = (Map<String, Object>) properties.getDruid().get(filter);
                switch (filter) {
                    case "stat":
                        proxyFilters.add(DruidStatConfigUtil.toStatFilter(itemMap, globalMap));
                        break;
                    case "wall":
                        WallConfig wallConfig = DruidWallConfigUtil.toWallConfig(itemMap, globalMap);
                        WallFilter wallFilter = new WallFilter();
                        wallFilter.setConfig(wallConfig);
                        proxyFilters.add(wallFilter);
                        break;
                    case "slf4j":
                        proxyFilters.add(DruidLogConfigUtil.initFilter(Slf4jLogFilter.class, itemMap, globalMap));
                        break;
                    case "commons-log":
                        proxyFilters.add(DruidLogConfigUtil.initFilter(CommonsLogFilter.class, itemMap, globalMap));
                        break;
                    case "log4j":
                        proxyFilters.add(DruidLogConfigUtil.initFilter(Log4jFilter.class, itemMap, globalMap));
                        break;
                    case "log4j2":
                        proxyFilters.add(DruidLogConfigUtil.initFilter(Log4j2Filter.class, itemMap, globalMap));
                        break;
                    default:
                        log.warn("dynamic-datasource current not support [{}]", filter);
                }
            }
        }
//        if (this.applicationContext != null) {
//            for (String filterId : gConfig.getProxyFilters()) {
//                proxyFilters.add(this.applicationContext.getBean(filterId, Filter.class));
//            }
//        }
        return proxyFilters;
    }

    @Override
    public boolean support(DataSourceProperty dataSourceProperty) {
        Class<? extends DataSource> type = dataSourceProperty.getType();
        return type == null || DRUID_DATASOURCE.equals(type.getName());
    }
}

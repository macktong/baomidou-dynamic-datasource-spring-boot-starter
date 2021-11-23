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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DsConfigUtil {

    private static final Pattern LINE_PATTERN = Pattern.compile("-(\\w)");

    /**
     * 横划线转驼峰
     */
    public static String lineToUpper(String str) {
        Matcher matcher = LINE_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, Object> mergeConfig(Map<String, Object> c, Map<String, Object> g) {
        int size = 1 + (int) ((c.size() + g.size()) / 0.75);
        Map<String, Object> map = new HashMap<>(size);
        map.putAll(g);
        map.putAll(c);
        return map;
    }

    public static Map<String, Method> getSetterMethods(Class<?> clazz) {
        Map<String, Method> methodMap = new HashMap<>();
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
                Method writeMethod = pd.getWriteMethod();
                if (writeMethod != null) {
                    methodMap.put(pd.getName(), writeMethod);
                }
            }
        } catch (Exception ignore) {
        }
        return methodMap;
    }
}

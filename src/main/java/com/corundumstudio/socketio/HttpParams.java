/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpParams {
    private final Map<String, List<String>> params;

    public HttpParams(Map<String, List<String>> params) {
        this.params = params;
    }

    public Set<String> getNames() {
        return params.keySet();
    }

    public String get(String name) {
        List<String> values = getAll(name);
        if (values == null || values.isEmpty()) return null;

        return values.get(0);
    }

    public List<String> getAll(String name) {
        return params.get(name);
    }

    public Map<String, List<String>> asMap() {
        return params;
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
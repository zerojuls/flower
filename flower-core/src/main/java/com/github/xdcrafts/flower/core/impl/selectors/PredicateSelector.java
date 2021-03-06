/*
 * Copyright (c) 2017 Vadim Dubs https://github.com/xdcrafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.github.xdcrafts.flower.core.impl.selectors;

import com.github.xdcrafts.flower.core.Action;
import com.github.xdcrafts.flower.core.Core;
import com.github.xdcrafts.flower.core.Extension;
import com.github.xdcrafts.flower.core.Middleware;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.xdcrafts.flower.tools.map.MapApi.get;

/**
 * Implementation of Selector that selects Action based on predicates.
 */
@SuppressWarnings("unchecked")
public class PredicateSelector extends WithMiddlewareSelectorBase {

    /**
     * Class with configuration keys.
     */
    public static final class ConfigurationKeys {
        public static final String PREDICATE = "predicate";
    }

    private final String name;
    private final boolean required;
    private final Map<Predicate, Extension> extensions;

    public PredicateSelector(String name, boolean required) {
        this(name, required, Collections.emptyList());
    }

    public PredicateSelector(String name, boolean required, List<Middleware> middleware) {
        super(middleware);
        this.name = name;
        this.required = required;
        this.extensions = new ConcurrentHashMap<>();
        this.meta.put(Core.ActionMeta.NAME, name);
        this.meta.put(Core.ActionMeta.TYPE, getClass().getName());
        this.meta.put(Core.ActionMeta.MIDDLEWARE, middleware);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Collection<Extension> extensions() {
        return this.extensions.values();
    }

    @Override
    public List<Action> selectAction(Map context) {
        final List<Action> actions = this.extensions
            .entrySet()
            .stream()
            .filter(e -> e.getKey().test(context))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        if (actions.isEmpty()) {
            if (this.required) {
                throw new IllegalArgumentException("Unable to select action, no suitable action found.");
            } else {
                return Collections.emptyList();
            }
        }
        return actions;
    }

    @Override
    public void register(Extension extension) {
        final Map configuration = extension.configuration();
        final Predicate predicate =
            get(configuration, Predicate.class, ConfigurationKeys.PREDICATE)
            .orElseThrow(() -> new IllegalArgumentException(
                extension + ": '" + ConfigurationKeys.PREDICATE + "' key required."
            ));
        if (this.extensions.containsKey(predicate)) {
            throw new IllegalArgumentException(predicate + " already registered!");
        }
        this.extensions.put(predicate, extension);
    }

    @Override
    public String toString() {
        return "PredicateSelector{"
                + "name='" + this.name + '\''
                + ", extensions=" + extensions
                + ", extensions=" + extensions
                + '}';
    }
}

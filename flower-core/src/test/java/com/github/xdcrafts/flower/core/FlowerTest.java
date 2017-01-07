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

package com.github.xdcrafts.flower.core;

import com.github.xdcrafts.flower.core.impl.DefaultAction;
import com.github.xdcrafts.flower.core.impl.selectors.KeywordSelector;
import com.github.xdcrafts.flower.core.impl.extensions.DefaultExtension;
import com.github.xdcrafts.flower.core.impl.flows.BasicSyncFlow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.xdcrafts.flower.tools.MapApi.get;
import static com.github.xdcrafts.flower.tools.MapApi.getUnsafe;
import static com.github.xdcrafts.flower.tools.MapApi.Mutable.assoc;
import static com.github.xdcrafts.flower.tools.MapDsl.Mutable.with;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Simple tests.
 */
public class FlowerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowerTest.class);

    @Test
    public void test() {
        final Middleware counterMiddleware = Middleware.middleware("counterMiddleware", (map, function) -> ctx -> {
            final int counter = get(ctx, Integer.class, "meta", "dummy").orElse(0);
            return function.apply(assoc(ctx, "meta", "dummy", counter + 1));
        });
        final Action firstAction = new DefaultAction(
            "firstAction",
            ctx -> assoc(ctx, "data", "first", true),
            Collections.singletonList(counterMiddleware)
        );
        final Action secondAction = new DefaultAction(
            "secondAction",
            ctx -> assoc(ctx, "data", "second", true),
            Collections.singletonList(counterMiddleware)
        );
        final Action thirdAction = new DefaultAction(
            "thirdAction",
            ctx -> assoc(ctx, "data", "third", true),
            Collections.singletonList(counterMiddleware)
        );
        final Flow simpleFlow = new BasicSyncFlow("simpleFlow", Arrays.asList(firstAction, secondAction));
        final Flow complexFlow = new BasicSyncFlow("complexFlow", Arrays.asList(simpleFlow, thirdAction));
        final Map result = complexFlow.apply(new HashMap());
        assertTrue(getUnsafe(result, Boolean.class, "data", "first"));
        assertTrue(getUnsafe(result, Boolean.class, "data", "second"));
        assertTrue(getUnsafe(result, Boolean.class, "data", "third"));
        assertEquals(3, getUnsafe(result, Integer.class, "meta", "dummy").longValue());
    }

    @Test
    public void extensionsTest() {
        final Action firstAction = Action.action("first-action", ctx -> assoc(ctx, "data", "first", true));
        final Extension firstExtension = new DefaultExtension(
            "firstExtension",
            firstAction,
            with(new HashMap()).assoc(KeywordSelector.ConfigurationKeys.KEYWORD_VALUE, "first").value()
        );
        final Action secondAction = Action.action("second-action", ctx -> assoc(ctx, "data", "second", true));
        final Extension secondExtension = new DefaultExtension(
            "secondExtension",
            secondAction,
            with(new HashMap()).assoc(KeywordSelector.ConfigurationKeys.KEYWORD_VALUE, "second").value()
        );
        final Selector keywordSelector = new KeywordSelector(
            "aSelector", "data.selectAction", Arrays.asList(firstExtension, secondExtension)
        );
        final Map firstResult = keywordSelector.apply(
            with(new HashMap()).assoc("data", "selectAction", "first").value()
        );
        final Map secondResult = keywordSelector.apply(
            with(new HashMap()).assoc("data", "selectAction", "second").value()
        );
        assertTrue(getUnsafe(firstResult, Boolean.class, "data", "first"));
        assertTrue(getUnsafe(secondResult, Boolean.class, "data", "second"));
    }
}
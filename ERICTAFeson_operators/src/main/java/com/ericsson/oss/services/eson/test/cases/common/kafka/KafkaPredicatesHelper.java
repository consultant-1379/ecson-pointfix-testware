/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.cases.common.kafka;

import com.google.common.base.Predicate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KafkaPredicatesHelper {

    private static final JsonParser JSON_PARSER = new JsonParser();

    private static final String SOURCE = "source";
    private static final String CM_SERVICE = "CmService";
    private static final String RET_5G = "RET_5G";
    private static final String COUNTER_DEFINITIONS = "counterDefinitions";
    private static final String ACTIVATION_SOURCE = "alg_RET_1";

    private KafkaPredicatesHelper() {
    }

    public static Predicate<String> getCmMediationPredicate() {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String json) {
                final JsonObject jsonObject = getJsonObject(json);
                return verifySource(jsonObject, CM_SERVICE) || verifySource(jsonObject, RET_5G);
            }
        };
    }

    public static Predicate<String> getPmMediationPredicate() {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String json) {
                final JsonObject jsonObject = getJsonObject(json);
                if (jsonObject.has(COUNTER_DEFINITIONS)) {
                    return jsonObject.getAsJsonArray(COUNTER_DEFINITIONS).size() > 0;
                }
                return false;
            }
        };
    }

    public static Predicate<String> getCmChangeMediationPredicate() {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String json) {
                final JsonObject jsonObject = getJsonObject(json);
                return verifySource(jsonObject, ACTIVATION_SOURCE);
            }
        };
    }

    private static JsonObject getJsonObject(final String json) {
        final JsonElement jsonElement = JSON_PARSER.parse(json);
        return jsonElement.getAsJsonObject();
    }

    private static boolean verifySource(final JsonObject jsonObject, final String expectedValue) {
        if (jsonObject.has(SOURCE)) {
            return jsonObject.get(SOURCE).getAsString().equalsIgnoreCase(expectedValue);
        }
        return false;
    }

}

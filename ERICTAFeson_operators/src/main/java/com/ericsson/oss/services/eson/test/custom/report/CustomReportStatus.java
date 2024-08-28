/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson  2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.custom.report;

public enum CustomReportStatus {

    PASSED(1, "green"),
    FAILED(2, "red");

    private String font;
    private int result;

    CustomReportStatus(final int result, final String font) {
        this.result = result;
        this.font = font;
    }

    /**
     * Results that can be used with colors.
     * <li>{@link #PASSED}</li>
     * <li>{@link #FAILED}</li>
     *
     * @param result result
     * @return Status
     */
    protected static String valueOf(final int result) {
        for (final CustomReportStatus v : values()) {
            if (v.getResult() == result) {
                return v.getFont() + v.name();
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Results that can be used with colors.
     * <li>{@link #PASSED}</li>
     * <li>{@link #FAILED}</li>
     *
     * @param result result
     * @return Color
     */
    public static String valueOf(final boolean result) {
        final int resultId = result ? 1 : 2;
        return valueOf(resultId);
    }

    /**
     *
     * @return result
     */
    private int getResult() {
        return result;
    }

    /**
     *
     * @return font
     */
    private String getFont() {
        return "<font color=" + font + ">";
    }
}

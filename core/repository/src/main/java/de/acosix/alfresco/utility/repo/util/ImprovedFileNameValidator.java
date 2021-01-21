/*
 * Copyright 2016 - 2021 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.alfresco.utility.repo.util;

import java.util.regex.Pattern;

import org.alfresco.util.FileNameValidator;
import org.alfresco.util.ParameterCheck;

/**
 * Slightly improved file name validator (compared to {@link FileNameValidator the default class}) addressing corner cases where the
 * original would fail to {@link #getValidFileName(String) produce a valid fiel name}.
 *
 * @author Axel Faust
 */
public class ImprovedFileNameValidator
{

    /**
     * The bad file name pattern.
     */
    public static final String FILENAME_ILLEGAL_REGEX = "[\\\"\\*\\\\\\>\\<\\?\\/\\:\\|]|\\.+$";

    private static final Pattern FILENAME_ILLEGAL_PATTERN_REPLACE = Pattern.compile(FILENAME_ILLEGAL_REGEX);

    public static boolean isValid(final String name)
    {
        return !FILENAME_ILLEGAL_PATTERN_REPLACE.matcher(name).find();
    }

    /**
     * Replaces illegal filename characters with '_'
     *
     * @param fileName
     *            the input file name
     * @return the valid file name, potentially different from the input if characters needed to be replaced
     */
    public static String getValidFileName(final String fileName)
    {
        ParameterCheck.mandatoryString("fileName", fileName);

        final String trimmed = fileName.trim();
        final String replaced = FILENAME_ILLEGAL_PATTERN_REPLACE.matcher(trimmed).replaceAll("_");
        return replaced;
    }
}
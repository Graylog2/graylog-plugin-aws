/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.graylog2.input;

import org.graylog2.plugin.Version;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputVersion {

    public static final Version PLUGIN_VERSION = new Version(0, 0, 1, "dev");
    public static final Version REQUIRED_VERSION = new Version(0, 90, 0);

}

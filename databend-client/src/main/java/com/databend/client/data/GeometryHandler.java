/*
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

package com.databend.client.data;

public class GeometryHandler implements ColumnTypeHandler {

    private boolean isNullable;

    public GeometryHandler(boolean isNullable) {
        this.isNullable = isNullable;
    }

    @Override
    public Object parseValue(Object value) {
        if (value == null) {
            if (isNullable) {
                return null;
            } else {
                throw new IllegalArgumentException("Geometry type is not nullable");
            }
        }
        if (value instanceof String) {
            String wkbOrWkt = (String) value;
            // binary wkb is converted to string during rest data transfer
            if (wkbOrWkt.startsWith("00") || wkbOrWkt.startsWith("01")) {
                return hexStringToByteArray(wkbOrWkt);
            }
            // todo We are not distinguishing between geo_json and wkt for the time being, they are now both in text format
            //      If we have a separate object to describe the variant type, we can consider handling geo_json
        }
        return value;
    }

    @Override
    public void setNullable(boolean isNullable) {
        this.isNullable = isNullable;
    }

    /**
     * Write the hexadecimal text back as a binary array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
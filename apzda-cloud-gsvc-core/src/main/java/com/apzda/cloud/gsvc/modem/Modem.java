/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.modem;

import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface Modem {

    default byte[] decode(HttpHeaders headers, InputStream stream) throws IOException, HttpMessageNotReadableException {
        return decode(headers, stream.readAllBytes());
    }

    byte[] decode(HttpHeaders headers, byte[] encodedText) throws IOException, HttpMessageNotReadableException;

    default byte[] encode(HttpHeaders headers, String plainText) throws IOException, HttpMessageNotWritableException {
        return encode(headers, plainText.getBytes(StandardCharsets.UTF_8));
    }

    byte[] encode(HttpHeaders headers, byte[] plainText) throws IOException, HttpMessageNotWritableException;

}

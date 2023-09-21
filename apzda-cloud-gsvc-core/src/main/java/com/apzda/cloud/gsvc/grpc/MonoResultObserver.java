/*
 * Copyright (C) 2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.grpc;

import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class MonoResultObserver<V> implements StreamObserver<V> {

    private V value;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private Throwable throwable;

    @Override
    public void onNext(V value) {
        this.value = value;
    }

    @Override
    public void onError(Throwable t) {
        this.throwable = t;
        this.countDownLatch.countDown();
    }

    @Override
    public void onCompleted() {
        this.countDownLatch.countDown();
    }

    public V getValue(Duration timeout) throws Throwable {
        if (this.throwable != null) {
            throw throwable;
        }
        this.countDownLatch.wait(timeout.toMillis());
        return value;
    }

}

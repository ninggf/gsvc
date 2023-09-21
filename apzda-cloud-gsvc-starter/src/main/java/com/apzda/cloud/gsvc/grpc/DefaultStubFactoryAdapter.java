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

import io.grpc.stub.AbstractStub;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.devh.boot.grpc.client.inject.StubTransformer;
import net.devh.boot.grpc.client.stubfactory.AsyncStubFactory;
import net.devh.boot.grpc.client.stubfactory.BlockingStubFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class DefaultStubFactoryAdapter implements StubFactoryAdapter {

    private final AsyncStubFactory asyncStubFactory;

    private final BlockingStubFactory blockingStubFactory;

    private final GrpcChannelFactoryAdapter grpcChannelFactoryAdapter;

    private final ApplicationContext applicationContext;

    private ArrayList<StubTransformer> stubTransformers;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createAsyncStub(String serviceName, Class<T> stubType) {
        val channel = grpcChannelFactoryAdapter.createChannel(serviceName);
        val stub = asyncStubFactory
            .createStub((Class<? extends AbstractStub<?>>) stubType.asSubclass(AbstractStub.class), channel);

        return stubType.cast(applyTransformers(serviceName, stub));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createBlockingStub(String serviceName, Class<T> stubType) {
        val channel = grpcChannelFactoryAdapter.createChannel(serviceName);
        val stub = blockingStubFactory
            .createStub((Class<? extends AbstractStub<?>>) stubType.asSubclass(AbstractStub.class), channel);

        return stubType.cast(applyTransformers(serviceName, stub));
    }

    protected AbstractStub<?> applyTransformers(String name, AbstractStub<?> stub) {
        for (final StubTransformer stubTransformer : getStubTransformers()) {
            stub = stubTransformer.transform(name, stub);
        }
        return stub;
    }

    private List<StubTransformer> getStubTransformers() {
        if (this.stubTransformers == null) {
            final Collection<StubTransformer> transformers = this.applicationContext
                .getBeansOfType(StubTransformer.class)
                .values();
            this.stubTransformers = new ArrayList<>(transformers);
            return this.stubTransformers;
        }
        return this.stubTransformers;
    }

}

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
package com.apzda.cloud.demo.math.service;

import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.demo.math.proto.OpNum;
import com.apzda.cloud.demo.math.proto.Result;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
public class MathServiceImpl implements MathService {

    @Override
    public Result add(OpNum request) {
        val num1 = request.getNum1();
        val num2 = request.getNum2();
        log.info("[{}] 收到请求, num1={}, num2={}", GsvcContextHolder.getRequestId(), num1, num2);
        return Result.newBuilder().setResult(num1 + num2).build();
    }

    @Override
    public Flux<Result> sum(final Flux<OpNum> request) {
        log.info("执行SUM");
        final Sinks.Many<Result> resultMany = Sinks.many().replay().all();
        val atomicLong = new AtomicLong();
        val context = GsvcContextHolder.current();
        request.subscribeOn(Schedulers.boundedElastic()).doOnComplete(() -> {
            context.restore();
            log.info("[{}] 请求处理完成啦: {}", GsvcContextHolder.getRequestId(), atomicLong.get());
            resultMany.tryEmitNext(Result.newBuilder().setResult(atomicLong.get()).build());
            resultMany.tryEmitComplete();
        }).doOnError(resultMany::tryEmitError).subscribe(opNum -> {
            context.restore();
            log.info("[{}] 收到操作数: {}", GsvcContextHolder.getRequestId(), opNum.getNum1());
            atomicLong.addAndGet(opNum.getNum1());
        });
        return resultMany.asFlux();
    }

    @Override
    public Flux<Result> even(OpNum request) {
        return Flux.range(request.getNum1(), request.getNum2())
            .filter(integer -> integer % 2 == 1)
            .map(integer -> Result.newBuilder().setResult(integer).build());
    }

    @Override
    public Flux<Result> square(Flux<OpNum> request) {
        return request.map(opNum -> Result.newBuilder().setResult((long) opNum.getNum1() * opNum.getNum1()).build());
    }

    @Override
    public Result divide(OpNum request) {
        log.info("[{}] Calculate {}/{}", GsvcContextHolder.getRequestId(), request.getNum1(), request.getNum2());
        return Result.newBuilder().setResult(request.getNum1() / request.getNum2()).build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Result auth(OpNum request) {
        return null;
    }

    @Override
    @PreAuthorize("@authz.isSa(#root)")
    public Result authz(OpNum request) {
        return null;
    }

}

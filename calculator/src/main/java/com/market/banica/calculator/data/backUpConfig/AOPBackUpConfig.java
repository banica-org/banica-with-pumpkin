package com.market.banica.calculator.data.backUpConfig;

import com.market.banica.calculator.data.ReceiptsBase;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
@RequiredArgsConstructor
public class AOPBackUpConfig {

    private final ReceiptsBase receiptsBase;

    @Before("execution(void com.market.banica.calculator.data.ReceiptsBase.set*(*))")
    public void before(JoinPoint jp) throws Exception {

        String prop = jp.getSignature().getName().substring(3);
        Object target = jp.getTarget();
        Object before = target.getClass().getMethod("get" + prop).invoke(target);
        Object now = jp.getArgs()[0];

        if (before.equals(now)){
            return;
        }
        receiptsBase.writeBackUp();
    }
}

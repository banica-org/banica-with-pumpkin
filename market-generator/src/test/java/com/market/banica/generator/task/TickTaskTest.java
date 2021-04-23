package com.market.banica.generator.task;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.TickGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TickTaskTest {

//    private static final TickGenerator tickGenerator = mock(TickGenerator.class);
//    private static final GoodSpecification good = mock(GoodSpecification.class);
//
//    private static TickTask tickTask;
//
//    @BeforeAll
//    static void beforeAll() {
//
//        tickTask = new TickTask(tickGenerator, good);
//
//    }
//
//    @Test
//    void run() {
//
//        TickTask nextTick = new TickTask(tickGenerator, good);
//
//        tickTask.run();
//
//        verify(tickGenerator, times(1))
//                .executeTickTask(any(MarketTick.class), eq(nextTick), anyLong());
//
//    }

}

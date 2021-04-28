package com.market.banica.generator.service;

import com.market.banica.generator.model.GoodSpecification;
import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.task.TickTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TickGeneratorImplTest {

    private static final String GOOD_BANICA = "banica";

    private static final ScheduledExecutorService tickScheduler = mock(ScheduledExecutorService.class);
    @SuppressWarnings("unchecked")
    private static final Map<String, ScheduledFuture<?>> tickTasks = mock(Map.class);
    private static final MarketState marketState = mock(MarketState.class);

    private static TickGenerator tickGenerator;

    @BeforeAll
    static void beforeAll() {

        tickGenerator = new TickGeneratorImpl("europe", marketState);
        ReflectionTestUtils.setField(tickGenerator, "tickScheduler", tickScheduler);
        ReflectionTestUtils.setField(tickGenerator, "tickTasks", tickTasks);

    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetInvocations() {

        reset(tickScheduler);
        reset(tickTasks);

    }

    @Test
    void startTickGeneration_NewGood() {

        GoodSpecification goodSpecification = mock(GoodSpecification.class);
        when(goodSpecification.getName()).thenReturn(GOOD_BANICA);
        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(false);

        TickTask startedTask = new TickTask(tickGenerator, goodSpecification);
        ScheduledFuture<?> taskScheduledFuture = mock(ScheduledFuture.class);
        doReturn(taskScheduledFuture).when(tickScheduler).schedule(eq(startedTask), anyLong(), eq(TimeUnit.SECONDS));


        tickGenerator.startTickGeneration(goodSpecification);


        verify(goodSpecification, times(1)).getName();
        verify(tickTasks, times(1)).containsKey(GOOD_BANICA);
        verify(tickScheduler, times(1)).schedule(eq(startedTask),
                anyLong(), eq(TimeUnit.SECONDS));
        verify(tickTasks, times(1)).put(GOOD_BANICA, taskScheduledFuture);

    }

    @Test
    void startTickGeneration_OldGood() {

        GoodSpecification goodSpecification = mock(GoodSpecification.class);
        when(goodSpecification.getName()).thenReturn(GOOD_BANICA);
        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(true);

        TickTask startedTask = new TickTask(tickGenerator, goodSpecification);
        ScheduledFuture<?> taskScheduledFuture = mock(ScheduledFuture.class);


        tickGenerator.startTickGeneration(goodSpecification);


        verify(goodSpecification, times(1)).getName();
        verify(tickTasks, times(1)).containsKey(GOOD_BANICA);
        verify(tickScheduler, times(0)).schedule(eq(startedTask),
                anyLong(), eq(TimeUnit.SECONDS));
        verify(tickTasks, times(0)).put(GOOD_BANICA, taskScheduledFuture);

    }

    @Test
    void stopTickGeneration_OldGood() {

        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(true);
        ScheduledFuture<?> taskScheduledFuture = mock(ScheduledFuture.class);
        doReturn(taskScheduledFuture).when(tickTasks).remove(GOOD_BANICA);

        tickGenerator.stopTickGeneration(GOOD_BANICA);

        verify(tickTasks, times(1)).remove(GOOD_BANICA);
        verify(taskScheduledFuture, times(1)).cancel(true);

    }

    @Test
    void stopTickGeneration_NewGood() {

        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(false);

        tickGenerator.stopTickGeneration(GOOD_BANICA);

        verify(tickTasks, times(0)).remove(GOOD_BANICA);

    }

    @Test
    void updateTickGeneration_OldGood() {

        GoodSpecification goodSpecification = mock(GoodSpecification.class);
        when(goodSpecification.getName()).thenReturn(GOOD_BANICA);
        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(true);

        ScheduledFuture<?> taskScheduledFutureOld = mock(ScheduledFuture.class);
        doReturn(taskScheduledFutureOld).when(tickTasks).remove(GOOD_BANICA);

        TickTask startedTask = new TickTask(tickGenerator, goodSpecification);
        ScheduledFuture<?> taskScheduledFutureNew = mock(ScheduledFuture.class);
        doReturn(taskScheduledFutureNew)
                .when(tickScheduler).schedule(eq(startedTask), anyLong(), eq(TimeUnit.SECONDS));


        tickGenerator.updateTickGeneration(goodSpecification);


        verify(goodSpecification, times(1)).getName();
        verify(tickTasks, times(1)).containsKey(GOOD_BANICA);
        verify(tickTasks, times(1)).remove(GOOD_BANICA);
        verify(taskScheduledFutureOld, times(1)).cancel(true);
        verify(tickScheduler, times(1)).schedule(eq(startedTask),
                anyLong(), eq(TimeUnit.SECONDS));
        verify(tickTasks, times(1)).put(GOOD_BANICA, taskScheduledFutureNew);

    }

    @Test
    void updateTickGeneration_NewGood() {

        GoodSpecification goodSpecification = mock(GoodSpecification.class);
        when(goodSpecification.getName()).thenReturn(GOOD_BANICA);
        when(tickTasks.containsKey(GOOD_BANICA)).thenReturn(false);

        ScheduledFuture<?> taskScheduledFutureOld = mock(ScheduledFuture.class);
        doReturn(taskScheduledFutureOld).when(tickTasks).remove(GOOD_BANICA);

        TickTask startedTask = new TickTask(tickGenerator, goodSpecification);
        ScheduledFuture<?> taskScheduledFutureNew = mock(ScheduledFuture.class);
        doReturn(taskScheduledFutureNew)
                .when(tickScheduler).schedule(eq(startedTask), anyLong(), eq(TimeUnit.SECONDS));


        tickGenerator.updateTickGeneration(goodSpecification);


        verify(goodSpecification, times(1)).getName();
        verify(tickTasks, times(1)).containsKey(GOOD_BANICA);
        verify(tickTasks, times(0)).remove(GOOD_BANICA);
        verify(taskScheduledFutureOld, times(0)).cancel(true);
        verify(tickScheduler, times(0)).schedule(eq(startedTask),
                anyLong(), eq(TimeUnit.SECONDS));
        verify(tickTasks, times(0)).put(GOOD_BANICA, taskScheduledFutureNew);

    }

    @Test
    void executeTickTask() {

        MarketTick marketTick = mock(MarketTick.class);
        TickTask tickTask = mock(TickTask.class);
        long delay = 10000;
        when(marketTick.getGood()).thenReturn(GOOD_BANICA);

        ScheduledFuture<?> taskScheduledFuture = mock(ScheduledFuture.class);
        doReturn(taskScheduledFuture).when(tickScheduler).schedule(tickTask, delay, TimeUnit.SECONDS);


        tickGenerator.executeTickTask(marketTick, tickTask, delay);


        verify(marketState, times(1)).addTickToMarket(marketTick);
        verify(tickScheduler, times(1)).schedule(tickTask, delay, TimeUnit.SECONDS);
        verify(tickTasks, times(1)).put(GOOD_BANICA, taskScheduledFuture);

    }

}
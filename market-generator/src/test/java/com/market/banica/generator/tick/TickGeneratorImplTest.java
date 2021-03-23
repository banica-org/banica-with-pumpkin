package com.market.banica.generator.tick;

import com.market.banica.generator.model.GoodSpecification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TickGeneratorImplTest {
    private static final String ORIGIN = "Europe";
    private static final String GOOD = "egg";
    private final Map<String, TickGeneratorTask> tickGeneratorTasks = new HashMap<>();

    @Mock
    private final MarketTick marketTick = new MarketTick(ORIGIN, GOOD, 3, 5);

    @Mock
    private BlockingQueue<MarketTick> tickBlockingQueue;

    @Mock
    private GoodSpecification goodSpecification;

    private TickGeneratorImpl tickGenerator;


    @Before
    public void initialize() throws InterruptedException {
        tickBlockingQueue.put(marketTick);
        tickGenerator = new TickGeneratorImpl();
    }


    @Test
    public void startTickGenerationWithValidDataAddsGood() {
        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);

        //Assert
        assertEquals(1, tickGenerator.getTickGeneratorTasks().size());
        assertTrue(this.tickGenerator.getTickGeneratorTasks().containsKey("egg"));
    }

    @Test
    public void stopTickGenerationWithValidDataRemovesGood() {
        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);

        tickGenerator.stopTickGeneration(GOOD);

        //Assert
        assertEquals(0, tickGenerator.getTickGeneratorTasks().size());
        assertFalse(this.tickGenerator.getTickGeneratorTasks().containsKey("egg"));
    }

    @Test
    public void updateTickGenerationWithValidDataUpdatesGood() {
        GoodSpecification goodSpecificationUpdated = new GoodSpecification(GOOD,
                1, 10, 1,
                1, 6, 1,
                1, 8, 1);

        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);

        tickGenerator.updateTickGeneration(GOOD, goodSpecificationUpdated);

        //Assert
        assertEquals(8, tickGenerator.getTickGeneratorTasks()
                .get(GOOD).getGoodSpecification().getPeriodHigh());

    }
}
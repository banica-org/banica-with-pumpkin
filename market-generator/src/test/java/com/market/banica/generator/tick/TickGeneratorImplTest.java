package com.market.banica.generator.tick;

import com.market.TickResponse;
import com.market.banica.generator.model.GoodSpecification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TickGeneratorImplTest {
    private static final String GOOD = "europe/egg";

    @Mock
    private GoodSpecification goodSpecification;

    private TickGeneratorImpl tickGenerator;

    @Before
    public void initialize() {
        tickGenerator = new TickGeneratorImpl();
    }

    @Test
    public void startTickGenerationWithValidDataAddsGood() {

        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);

        //Assert
        assertEquals(1, tickGenerator.getTickGeneratorTasks().size());
        assertTrue(this.tickGenerator.getTickGeneratorTasks().containsKey(GOOD));
    }

    @Test
    public void stopTickGenerationWithValidDataRemovesGood() {

        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);

        tickGenerator.stopTickGeneration(GOOD);

        //Assert
        assertEquals(0, tickGenerator.getTickGeneratorTasks().size());
        assertFalse(this.tickGenerator.getTickGeneratorTasks().containsKey(GOOD));
    }

    @Test
    public void updateTickGenerationWithValidDataUpdatesGood() {

        //Arrange
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

    @Test
    public void generateTicksWithValidDataCreatesTicks() {

        //Arrange
        List<TickResponse> tickResponses;

        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);
        tickResponses = tickGenerator.generateTicks(GOOD);
        String goodNameActual = tickResponses.get(0).getGoodName();

        //Assert
        assertFalse(tickResponses.isEmpty());
        assertEquals(GOOD, goodNameActual);
    }

    @Test
    public void generateTicksWithInvalidData() {

        //Arrange
        List<TickResponse> tickResponses;
        String newGood = "europe/milk";

        //Act
        tickGenerator.startTickGeneration(GOOD, goodSpecification);
        tickResponses = tickGenerator.generateTicks(newGood);

        //Assert
        assertEquals(tickResponses.size(), 0);
    }

}
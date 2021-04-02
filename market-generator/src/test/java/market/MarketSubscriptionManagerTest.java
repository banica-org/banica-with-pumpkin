package market;

import com.aurora.Aurora;
import com.market.TickResponse;
import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.service.MarketSubscriptionManager;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketSubscriptionManagerTest {

    @InjectMocks
    private MarketSubscriptionManager marketSubscriptionManager;

    private final ServerCallStreamObserver<Aurora.AuroraResponse> subscriberOne = mock(ServerCallStreamObserver.class);

    private final ServerCallStreamObserver<Aurora.AuroraResponse> subscriberTwo = mock(ServerCallStreamObserver.class);

    private static final String GOOD_BANICA = "banica";

    private static final String TOPIC_EGGS = "market/eggs";

    private static final Aurora.AuroraRequest AURORA_REQUEST_BANICA = Aurora.AuroraRequest.newBuilder().setTopic("market/"+GOOD_BANICA).build();

    private static final Aurora.AuroraRequest AURORA_REQUEST_INVALID = Aurora.AuroraRequest.newBuilder().build();

    @Test
    void subscribeForItemWithValidDataInputExecutesWithSuccess() {
        // Arrange
        assertNull(marketSubscriptionManager.getSubscribers(GOOD_BANICA));
        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);
        int expected = 1;

        // Act
        int actual = marketSubscriptionManager.getSubscribers(GOOD_BANICA).size();

        // Assert
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void subscribeForItemWithInputParameterWithInvalidGoodNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> marketSubscriptionManager.subscribe(AURORA_REQUEST_INVALID, subscriberOne));
    }

    @Test
    void notifySubscribersWithParameterGoodNameBanicaNotifiesSubscriberForBanica() {
        // Arrange
        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setPrice(1)
                .setQuantity(1)
                .build();

        // Act
        marketSubscriptionManager.notifySubscribers(tickResponse);

        // Assert
        verify(subscriberOne, times(1)).onNext(any());
    }

    @Test
    void notifySubscribersWithParameterGoodNameEggsDoesNotNotifySubscriberForBanica() {
        // Arrange
        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(TOPIC_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();
        // Act
        marketSubscriptionManager.notifySubscribers(tickResponse);

        // Assert
        verify(subscriberOne, times(0)).onNext(any());
    }

    @Test
    void getTickResponseGoodName_ReturnsTickGoodName() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(TOPIC_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        Assertions.assertEquals(TOPIC_EGGS, tickResponse.getGoodName());
    }

    @Test
    void getSubscribersReturnsGivenSubscribers() {
        // Arrange
        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(AURORA_REQUEST_BANICA, subscriberTwo);

        // Act
        Set<StreamObserver<Aurora.AuroraResponse>> actual = marketSubscriptionManager.getSubscribers(AURORA_REQUEST_BANICA.getTopic().split("/")[1]);

        HashSet<StreamObserver<Aurora.AuroraResponse>> expected = new HashSet<>();
        expected.add(subscriberOne);
        expected.add(subscriberTwo);

        // Assert
        Assertions.assertEquals(expected, actual);
    }

}
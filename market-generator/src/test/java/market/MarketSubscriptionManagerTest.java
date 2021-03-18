package market;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.exception.NotFoundException;
import com.market.banica.generator.service.MarketSubscriptionManager;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

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

    private final StreamObserver<TickResponse> subscriberOne = mock(StreamObserver.class);

    private final StreamObserver<TickResponse> subscriberTwo = mock(StreamObserver.class);

    private static final String GOOD_BANICA = "Banica";

    private static final String GOOD_EGGS = "Eggs";

    private static final MarketDataRequest MARKET_DATA_REQUEST_BANICA = MarketDataRequest.newBuilder().setGoodName(GOOD_BANICA).build();

    private static final MarketDataRequest MARKET_DATA_REQUEST_INVALID = MarketDataRequest.newBuilder().build();

    @Test
    void subscribeForItemWithValidDataInputExecutesWithSuccess() {
        // Arrange
        assertNull(marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()));
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        int expected = 1;

        // Act
        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void subscribeForItemWithInputParameterWithInvalidGoodNameThrowsException() {
        assertThrows(NotFoundException.class, () -> marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne));
    }

    @Test
    void unsubscribeForItemWithValidDataInputExecutesWithSuccess() {
        // Arrange
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);
        int expected = 1;

        // Act
        marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);
        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void unsubscribeForItemWithInputParameterWithInvalidGoodNameThrowsException() {
        assertThrows(NotFoundException.class, () -> marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne));
    }

    @Test
    void unsubscribeForItemWithInputParameterWithValidGoodNameRemovesSubscriber() {
        // Arrange
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        assertEquals(1, marketSubscriptionManager.getSubscribers(GOOD_BANICA).size());

        // Act
        marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        // Assert
        assertNull(marketSubscriptionManager.getSubscribers(GOOD_BANICA));
    }

    @Test
    public void unsubscribeForItemWithNonExistentGoodNameInMapThrowsException() {
        assertThrows(NotFoundException.class, () -> marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne));
    }

    @Test
    void notifySubscribersWithParameterGoodNameBanicaNotifiesSubscriberForBanica() {
        // Arrange
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setPrice(1)
                .setQuantity(1)
                .build();

        // Act
        marketSubscriptionManager.notifySubscribers(tickResponse);

        // Assert
        verify(marketSubscriptionManager.getSubscribers(GOOD_BANICA).iterator().next(), times(1)).onNext(any());
    }

    @Test
    void notifySubscribersWithParameterGoodNameEggsDoesNotNotifySubscriberForBanica() {
        // Arrange
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();
        // Act
        marketSubscriptionManager.notifySubscribers(tickResponse);

        // Assert
        verify(marketSubscriptionManager.getSubscribers(GOOD_BANICA).iterator().next(), times(0)).onNext(any());
    }

    @Test
    public void notifySubscribersForItemWithInputParameterWithInvalidGoodNameThrowsException() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName("")
                .setPrice(1)
                .setQuantity(1)
                .build();

        assertThrows(NotFoundException.class, () -> marketSubscriptionManager.notifySubscribers(tickResponse));
    }

    @Test
    void getRequestItemNameReturnsItemName() {
        assertEquals(GOOD_BANICA, marketSubscriptionManager.getRequestGoodName(MARKET_DATA_REQUEST_BANICA));
    }

    @Test
    void getTickResponseGoodNameReturnsTickGoodName() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        assertEquals(marketSubscriptionManager.getTickResponseGoodName(tickResponse), GOOD_EGGS);
    }

    @Test
    void getSubscribersReturnsGivenSubscribers() {
        // Arrange
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        // Act
        HashSet<StreamObserver<TickResponse>> actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName());

        HashSet<StreamObserver<TickResponse>> expected = new HashSet<>();
        expected.add(subscriberOne);
        expected.add(subscriberTwo);

        // Assert
        assertEquals(expected, actual);
    }
}
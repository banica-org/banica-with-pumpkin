package market;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.exception.NoSuchGoodException;
import com.market.banica.generator.service.MarketSubscriptionManager;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
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
    void subscribeShouldAddNewSubscriberToSubscriptions() {
        assertNull(marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()));

        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void subscribeShouldThrowExceptionAndNotAddNewSubscriberToSubscriptionsIfIllegalGoodName() {
        assertNull(marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()));

        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        assertThrows(NoSuchGoodException.class, () ->  marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne));

        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void unsubscribeShouldUnsubscribeTheSubscriber() {
        assertNull(marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()));

        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void unsubscribeShouldThrowExceptionAndNotRemoveSubscriberIfIllegalGoodName() {
        assertNull(marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()));

        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        assertThrows(NoSuchGoodException.class, () ->  marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne));

        int actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void unsubscribeShouldRemoveMapWhenOnlyOneSubscriberInside() {
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        assertEquals(1, marketSubscriptionManager.getSubscribers(GOOD_BANICA).size());

        marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        assertNull(marketSubscriptionManager.getSubscribers(GOOD_BANICA));
    }

    @Test
    public void unsubscribeShouldThrowNoSuchGoodExceptionIfNoSuchGoodNameInMap() {
        assertThrows(NoSuchGoodException.class, () -> marketSubscriptionManager.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne));
    }


    @Test
    void notifySubscribersShouldNotifySubscriber() {
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_BANICA)
                .setPrice(1)
                .setQuantity(1)
                .build();

        marketSubscriptionManager.notifySubscribers(tickResponse);

        verify(marketSubscriptionManager.getSubscribers(GOOD_BANICA).iterator().next(), times(1)).onNext(any());
    }

    @Test
    void notifySubscribersShouldNotNotifySubscriberWhenTickResponseIsForDifferentGood() {
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        marketSubscriptionManager.notifySubscribers(tickResponse);

        verify(marketSubscriptionManager.getSubscribers(GOOD_BANICA).iterator().next(), times(0)).onNext(any());
    }

    @Test
    public void notifySubscribersShouldThrowNoSuchGoodExceptionIfIllegalGoodName() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName("")
                .setPrice(1)
                .setQuantity(1)
                .build();

        assertThrows(NoSuchGoodException.class, () -> marketSubscriptionManager.notifySubscribers(tickResponse));
    }

    @Test
    void getRequestItemNameShouldReturnItemName() {
        assertEquals(marketSubscriptionManager.getRequestGoodName(MARKET_DATA_REQUEST_BANICA), GOOD_BANICA);
    }

    @Test
    void getTickResponseItemNameShouldReturnTickItemName() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setGoodName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        assertEquals(marketSubscriptionManager.getTickResponseGoodName(tickResponse), GOOD_EGGS);
    }

    @Test
    void getSubscribersShouldReturnGivenSubscribers() {
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionManager.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        HashSet<StreamObserver<TickResponse>> actual = marketSubscriptionManager.getSubscribers(MARKET_DATA_REQUEST_BANICA.getGoodName());

        HashSet<StreamObserver<TickResponse>> expected = new HashSet<>();
        expected.add(subscriberOne);
        expected.add(subscriberTwo);

        Assert.assertEquals(expected, actual);
    }
}
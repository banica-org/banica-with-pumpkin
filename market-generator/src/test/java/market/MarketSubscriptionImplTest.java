package market;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.exception.NoSuchGoodException;
import com.market.banica.generator.service.MarketSubscriptionImpl;
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
class MarketSubscriptionImplTest {

    @InjectMocks
    private MarketSubscriptionImpl marketSubscriptionImpl;

    private final StreamObserver<TickResponse> subscriberOne = mock(StreamObserver.class);

    private final StreamObserver<TickResponse> subscriberTwo = mock(StreamObserver.class);

    private static final String GOOD_BANICA = "Banica";

    private static final String GOOD_EGGS = "Eggs";

    private static final MarketDataRequest MARKET_DATA_REQUEST_BANICA = MarketDataRequest.newBuilder().setItemName(GOOD_BANICA).build();

    private static final MarketDataRequest MARKET_DATA_REQUEST_INVALID = MarketDataRequest.newBuilder().build();


    @Test
    void subscribeShouldAddNewSubscriberToSubscriptions() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        int actual = marketSubscriptionImpl.getSubscribers(MARKET_DATA_REQUEST_BANICA.getItemName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void subscribeShouldNotAddNewSubscriberToSubscriptionsIfIllegalGoodName() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne);
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        int actual = marketSubscriptionImpl.getSubscribers(MARKET_DATA_REQUEST_BANICA.getItemName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void unsubscribeShouldNotRemoveSubscriberIfIllegalGoodName() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionImpl.unsubscribe(MARKET_DATA_REQUEST_INVALID, subscriberOne);

        int actual = marketSubscriptionImpl.getSubscribers(MARKET_DATA_REQUEST_BANICA.getItemName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    void unsubscribeShouldUnsubscribeTheSubscriber() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        marketSubscriptionImpl.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        int actual = marketSubscriptionImpl.getSubscribers(MARKET_DATA_REQUEST_BANICA.getItemName()).size();
        int expected = 1;

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void unsubscribeShouldRemoveMapWhenOnlyOneSubscriberInside() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        assertEquals(1, marketSubscriptionImpl.getSubscribers(GOOD_BANICA).size());

        marketSubscriptionImpl.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        assertNull(marketSubscriptionImpl.getSubscribers(GOOD_BANICA));
    }

    @Test
    public void unsubscribeShouldThrowNoSuchGoodException() {
        assertThrows(NoSuchGoodException.class, () -> marketSubscriptionImpl.unsubscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne));
    }

    @Test
    void notifySubscribersShouldNotifySubscriber() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setItemName(GOOD_BANICA)
                .setPrice(1)
                .setQuantity(1)
                .build();

        marketSubscriptionImpl.notifySubscribers(tickResponse);

        verify(marketSubscriptionImpl.getSubscribers(GOOD_BANICA).iterator().next(), times(1)).onNext(any());
    }

    @Test
    void notifySubscribersShouldNotNotifySubscriber() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        TickResponse tickResponse = TickResponse.newBuilder()
                .setItemName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        marketSubscriptionImpl.notifySubscribers(tickResponse);

        verify(marketSubscriptionImpl.getSubscribers(GOOD_BANICA).iterator().next(), times(0)).onNext(any());
    }

    @Test
    void getRequestItemNameShouldReturnItemName() {
        assertEquals(marketSubscriptionImpl.getRequestGoodName(MARKET_DATA_REQUEST_BANICA), GOOD_BANICA);
    }

    @Test
    void getTickResponseItemNameShouldReturnTickItemName() {
        TickResponse tickResponse = TickResponse.newBuilder()
                .setItemName(GOOD_EGGS)
                .setPrice(1)
                .setQuantity(1)
                .build();

        assertEquals(marketSubscriptionImpl.getTickResponseGoodName(tickResponse), GOOD_EGGS);
    }

    @Test
    void getSubscribersShouldReturnGivenSubscribers() {
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);
        marketSubscriptionImpl.subscribe(MARKET_DATA_REQUEST_BANICA, subscriberTwo);

        HashSet<StreamObserver<TickResponse>> actual = marketSubscriptionImpl.getSubscribers(MARKET_DATA_REQUEST_BANICA.getItemName());

        HashSet<StreamObserver<TickResponse>> expected = new HashSet<>();
        expected.add(subscriberOne);
        expected.add(subscriberTwo);

        Assert.assertEquals(expected, actual);
    }
}
package market;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketService;
import com.market.banica.generator.service.MarketSubscriptionManager;
import com.market.banica.generator.service.TickGenerator;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @InjectMocks
    private MarketService marketService;

    @Mock
    private MarketSubscriptionManager marketSubscriptionServiceImpl;

    @Mock
    private TickGenerator tickGenerator;

    private static final String GOOD_BANICA = "Banica";

    private static final MarketDataRequest MARKET_DATA_REQUEST_BANICA = MarketDataRequest.newBuilder().setGoodName(GOOD_BANICA).build();

    private final StreamObserver<TickResponse> subscriberOne = mock(StreamObserver.class);

    @Test
    void subscribeForItemShouldVerifyMethodCalls() {
        List<TickResponse> ticks = Arrays.asList(TickResponse.newBuilder().setGoodName("firstTick").build(),
                TickResponse.newBuilder().setGoodName("secondTick").build());

        Mockito.when(marketSubscriptionServiceImpl.getRequestGoodName(MARKET_DATA_REQUEST_BANICA)).thenReturn(GOOD_BANICA);
        Mockito.when(tickGenerator.generateTicks(GOOD_BANICA)).thenReturn(ticks);

        marketService.subscribeForItem(MARKET_DATA_REQUEST_BANICA, subscriberOne);


        verify(tickGenerator, times(1)).generateTicks(GOOD_BANICA);
        verify(marketSubscriptionServiceImpl, times(1)).subscribe(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        verify(subscriberOne, times(2)).onNext(any());
        verify(subscriberOne, times(1)).onCompleted();
    }
}
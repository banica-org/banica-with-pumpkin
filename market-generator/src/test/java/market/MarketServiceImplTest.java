package market;

import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketServiceImpl;
import com.market.banica.generator.service.MarketSubscriptionImpl;
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
class MarketServiceImplTest {

    @InjectMocks
    private MarketServiceImpl marketService;

    @Mock
    private MarketSubscriptionImpl marketSubscriptionServiceImpl;

    @Mock
    private TickGenerator tickGenerator;

    private static final String GOOD_BANICA = "Banica";

    private static final MarketDataRequest MARKET_DATA_REQUEST_BANICA = MarketDataRequest.newBuilder().setItemName(GOOD_BANICA).build();

    private final StreamObserver<TickResponse> subscriberOne = mock(StreamObserver.class);

    @Test
    void subscribeForItem() {
        List<TickResponse> ticks = Arrays.asList(TickResponse.newBuilder().setItemName("firstTick").build(),
                TickResponse.newBuilder().setItemName("secondTick").build());

        Mockito.when(marketSubscriptionServiceImpl.getRequestGoodName(MARKET_DATA_REQUEST_BANICA)).thenReturn(GOOD_BANICA);
        Mockito.when(tickGenerator.generateTicks(GOOD_BANICA)).thenReturn(ticks);

        marketService.subscribeForItem(MARKET_DATA_REQUEST_BANICA, subscriberOne);

        verify(subscriberOne,times(2)).onNext(any());
        verify(subscriberOne, times(1)).onCompleted();
    }
}
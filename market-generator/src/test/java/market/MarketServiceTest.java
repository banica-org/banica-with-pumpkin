package market;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketService;
import com.market.banica.generator.service.MarketSubscriptionManager;
import com.market.banica.generator.tick.TickGeneratorImpl;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    private static final String GOOD = "europe/banica";
    public static final String CLIENT_ID = "validId";

    private static MarketDataRequest MARKET_DATA_REQUEST;

    private static CatalogueRequest CATALOGUE_REQUEST;

    private static CatalogueResponse CATALOGUE_RESPONSE;

    private static StreamObserver<TickResponse> TICK_RESPONSE_STREAM_OBSERVER;
    private static StreamObserver<CatalogueResponse> CATALOGUE_RESPONSE_STREAM_OBSERVER;

    private List<String> marketCatalogue = Arrays.asList("eggs");


    private MarketSubscriptionManager marketSubscriptionManager;

    private TickGeneratorImpl tickGenerator;

    private MarketService marketService;


    @BeforeEach
    void setUp() {

        MARKET_DATA_REQUEST = MarketDataRequest.newBuilder().setGoodName(GOOD).build();
        TICK_RESPONSE_STREAM_OBSERVER = Mockito.mock(StreamObserver.class);

        CATALOGUE_REQUEST = CatalogueRequest.newBuilder().setClientId(CLIENT_ID).setMarketOrigin("europe").build();
        CATALOGUE_RESPONSE = CatalogueResponse.newBuilder().addAllFoodItems(marketCatalogue).build();
        CATALOGUE_RESPONSE_STREAM_OBSERVER = Mockito.mock(StreamObserver.class);

        marketSubscriptionManager = Mockito.mock(MarketSubscriptionManager.class);
        tickGenerator = Mockito.mock(TickGeneratorImpl.class);

        marketService = new MarketService(marketSubscriptionManager, tickGenerator);
    }

    @Test
    void subscribeForItemShouldVerifyMethodCalls() {

        //Act
        marketService.subscribeForItem(MARKET_DATA_REQUEST, TICK_RESPONSE_STREAM_OBSERVER);


        //Assert
        verify(tickGenerator, times(1)).generateTicks(MARKET_DATA_REQUEST.getGoodName());
        verify(marketSubscriptionManager, times(1)).subscribe(MARKET_DATA_REQUEST, TICK_RESPONSE_STREAM_OBSERVER);
        verify(TICK_RESPONSE_STREAM_OBSERVER, times(1)).onCompleted();
    }

    @Test
    void requestCatalogueShouldVerifyMethodCalls() {
        //Arrange
        when(tickGenerator.getMarketCatalogue(CATALOGUE_REQUEST.getMarketOrigin())).thenReturn(marketCatalogue);

        //Act
        marketService.requestCatalogue(CATALOGUE_REQUEST, CATALOGUE_RESPONSE_STREAM_OBSERVER);

        //Assert
        verify(tickGenerator, times(1)).getMarketCatalogue(CATALOGUE_REQUEST.getMarketOrigin());
        verify(CATALOGUE_RESPONSE_STREAM_OBSERVER, times(1)).onNext(CATALOGUE_RESPONSE);
        verify(CATALOGUE_RESPONSE_STREAM_OBSERVER, times(1)).onCompleted();

    }
}


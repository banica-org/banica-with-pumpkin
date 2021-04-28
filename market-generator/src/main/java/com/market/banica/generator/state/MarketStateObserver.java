package com.market.banica.generator.state;

import com.market.banica.generator.model.MarketTick;
import com.market.banica.generator.service.MarketStateImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.Map;

@EnableMBeanExport
@ManagedResource
@Service
public class MarketStateObserver {

    private final MarketStateImpl marketState;

    @Autowired
    public MarketStateObserver(MarketStateImpl itemMarket) {
        this.marketState = itemMarket;
    }

    @ManagedOperation
    public Map<String, Map<Double, Long>> getMarketStateProductsQuantity() {
        return marketState.getMarketStateProductsQuantity();
    }

    @ManagedOperation
    public Map<String, Map<Double, Long>> getMarketSnapshotProductsQuantity() {
        return marketState.getMarketSnapshotProductsQuantity();
    }

}
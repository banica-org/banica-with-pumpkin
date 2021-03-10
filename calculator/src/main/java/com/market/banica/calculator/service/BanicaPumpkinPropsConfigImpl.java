package com.market.banica.calculator.service;

import com.market.banica.calculator.configuration.BanicaPumpkinProps;
import com.market.banica.calculator.service.contract.BanicaPumpkinPropsConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BanicaPumpkinPropsConfigImpl implements BanicaPumpkinPropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BanicaPumpkinPropsConfigImpl.class);

    private final BanicaPumpkinProps banicaPumpkinProps;

    @Override
    public BanicaPumpkinProps createReceipt(BanicaPumpkinProps banicaPumpkinPropsNew){
        LOGGER.debug("BanicaPumpkinPropsConfig service impl: In createReceipt method");

        banicaPumpkinProps.setEggCount(banicaPumpkinPropsNew.getEggCount());
        LOGGER.debug("BanicaPumpkinProps's eggCount set with value {}",banicaPumpkinPropsNew.getEggCount());

        banicaPumpkinProps.setCrustsCount(banicaPumpkinPropsNew.getCrustsCount());
        LOGGER.debug("BanicaPumpkinProps's crustsCount set with value {}",banicaPumpkinPropsNew.getCrustsCount());

        banicaPumpkinProps.setPumpkinGrams(banicaPumpkinPropsNew.getPumpkinGrams());
        LOGGER.debug("BanicaPumpkinProps's pumpkinGrams set with value {}",banicaPumpkinPropsNew.getPumpkinGrams());
        return banicaPumpkinProps;
    }

}

package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.ProductDto;
import com.market.banica.common.exception.ProductNotAvailableException;

import java.util.List;

public interface TransactionService {

    List<ProductDto> buyProduct(String clientId, String itemName, long quantity) throws ProductNotAvailableException;
}

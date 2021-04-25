package com.market.banica.calculator.service.contract;

import com.market.banica.calculator.dto.ProductDto;

import java.util.List;

public interface TransactionService {

    List<ProductDto> buyProduct(String clientId, String itemName, long quantity);
}

package com.market.banica.calculator.service.contract;

import java.io.IOException;

public interface BackUpService {

    void readBackUp() throws IOException;

    void writeBackUp();

}

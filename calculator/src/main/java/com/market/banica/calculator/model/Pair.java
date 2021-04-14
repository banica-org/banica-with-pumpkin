package com.market.banica.calculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair  <T,K>{
    private T fst;
    private K snd;
}

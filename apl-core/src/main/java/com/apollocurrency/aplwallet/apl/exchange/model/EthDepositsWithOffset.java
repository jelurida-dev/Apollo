package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EthDepositsWithOffset  extends OffsetModel{
    private List<UserEthDepositInfo> deposits = new ArrayList<>();
}

/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.monetary.MonetarySystem;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemPublishExchangeOffer;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

public final class GetExpectedBuyOffers extends AbstractAPIRequestHandler {

    private static class GetExpectedBuyOffersHolder {
        private static final GetExpectedBuyOffers INSTANCE = new GetExpectedBuyOffers();
    }

    public static GetExpectedBuyOffers getInstance() {
        return GetExpectedBuyOffersHolder.INSTANCE;
    }

    private GetExpectedBuyOffers() {
        super(new APITag[] {APITag.MS}, "currency", "account", "sortByRate");
    }

    private final Comparator<Transaction> rateComparator = (o1, o2) -> {
        MonetarySystemPublishExchangeOffer a1 = (MonetarySystemPublishExchangeOffer)o1.getAttachment();
        MonetarySystemPublishExchangeOffer a2 = (MonetarySystemPublishExchangeOffer)o2.getAttachment();
        return Long.compare(a2.getBuyRateATM(), a1.getBuyRateATM());
    };
    private static PhasingPollService phasingPollService = CDI.current().select(PhasingPollService.class).get();
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long currencyId = ParameterParser.getUnsignedLong(req, "currency", false);
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean sortByRate = "true".equalsIgnoreCase(req.getParameter("sortByRate"));

        Filter<Transaction> filter = transaction -> {
            if (transaction.getType() != MonetarySystem.PUBLISH_EXCHANGE_OFFER) {
                return false;
            }
            if (accountId != 0 && transaction.getSenderId() != accountId) {
                return false;
            }
            MonetarySystemPublishExchangeOffer attachment = (MonetarySystemPublishExchangeOffer)transaction.getAttachment();
            return currencyId == 0 || attachment.getCurrencyId() == currencyId;
        };

        List<Transaction> transactions = phasingPollService.getExpectedTransactions(filter);
        if (sortByRate) {
            Collections.sort(transactions, rateComparator);
        }

        JSONObject response = new JSONObject();
        JSONArray offerData = new JSONArray();
        transactions.forEach(transaction -> offerData.add(JSONData.expectedBuyOffer(transaction)));
        response.put("offers", offerData);
        return response;
    }

}

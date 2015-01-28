package com.xeiam.xchange.cryptsy.service.polling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.RestProxyFactory;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.cryptsy.Cryptsy;
import com.xeiam.xchange.cryptsy.CryptsyAdapters;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.cryptsy.dto.CryptsyGenericReturn;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyCurrencyPairsReturn;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyMarketId;
import com.xeiam.xchange.cryptsy.service.CryptsyHmacPostBodyDigest;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.service.BaseExchangeService;
import com.xeiam.xchange.service.polling.BasePollingService;

/**
 * @author ObsessiveOrange
 */
public class CryptsyBasePollingService<T extends Cryptsy> extends BaseExchangeService implements BasePollingService {

  private final Logger logger = LoggerFactory.getLogger(CryptsyBasePollingService.class);

  protected final String apiKey;
  protected final T cryptsyPrivate;
  protected final ParamsDigest signatureCreator;

  protected final T cryptsyPublic;

  /**
   * Constructor
   *
   * @param cryptsyType
   * @param exchange
   */
  public CryptsyBasePollingService(Class<T> cryptsyType, Exchange exchange) {

    super(exchange);

    // for private API data (trade and account)
    this.cryptsyPrivate = RestProxyFactory.createProxy(cryptsyType, exchange.getExchangeSpecification().getSslUri());
    this.apiKey = exchange.getExchangeSpecification().getApiKey();
    this.signatureCreator = CryptsyHmacPostBodyDigest.createInstance(exchange.getExchangeSpecification().getSecretKey());

    // for public API (market data)
    this.cryptsyPublic = RestProxyFactory.createProxy(cryptsyType,
        (String) exchange.getExchangeSpecification().getParameter(CryptsyExchange.KEY_PUBLIC_API_URL));

  }

  @Override
  public List<CurrencyPair> getExchangeSymbols() throws IOException {

    List<CurrencyPair> currencyPairs = new ArrayList<CurrencyPair>();

    CryptsyCurrencyPairsReturn response = cryptsyPublic.getCryptsyCurrencyPairs();
    HashMap<String, CryptsyMarketId> map = response.getReturnValue();

    CryptsyCurrencyUtils.marketIds_CurrencyPairs.clear();
    CryptsyCurrencyUtils.currencyPairs_MarketIds.clear();

    for (String pairString : map.keySet()) {
      CurrencyPair currencyPair = CryptsyAdapters.adaptCurrencyPair(pairString);
      String idString = map.get(pairString).getMarketid();
      Integer marketId = Integer.valueOf(idString);

      CryptsyCurrencyUtils.marketIds_CurrencyPairs.put(marketId, currencyPair);
      CryptsyCurrencyUtils.currencyPairs_MarketIds.put(currencyPair, marketId);
      currencyPairs.add(currencyPair);
    }

    return currencyPairs;
  }

  @SuppressWarnings("rawtypes")
  public static <T extends CryptsyGenericReturn> T checkResult(T info) {

    if (info == null) {
      throw new ExchangeException("Cryptsy returned nothing");
    } else if (!info.isSuccess()) {
      throw new ExchangeException(info.getError());
    } else if (info.getError() != null) {
      throw new ExchangeException(info.getError());
    } else if (info.getReturnValue() == null) {
      throw new ExchangeException("Null data returned");
    }
    return info;
  }

}

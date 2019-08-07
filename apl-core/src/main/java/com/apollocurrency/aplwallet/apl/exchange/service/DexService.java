package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.api.request.GetEthBalancesRequest;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessorImpl;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageService;
import com.apollocurrency.aplwallet.apl.core.app.service.SecureStorageServiceImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.PhasingPollServiceImpl;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.rest.converter.HttpRequestToCreateTransactionRequestConverter;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexCloseOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexContractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexControlOfFrozenMoneyAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachmentV2;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingPhasingVoteCasting;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixV2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.eth.model.EthWalletBalanceInfo;
import com.apollocurrency.aplwallet.apl.eth.service.EthereumWalletService;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexContractTable;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferDao;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOfferTable;
import com.apollocurrency.aplwallet.apl.exchange.model.DexContractDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBMatchingRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContract;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeContractStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.ExchangeOrder;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.model.SwapDataInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.utils.DexCurrencyValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class DexService {
    private static final Logger LOG = LoggerFactory.getLogger(DexService.class);

    private EthereumWalletService ethereumWalletService;
    private DexSmartContractService dexSmartContractService;
    private DexOfferDao dexOfferDao;
    private DexOfferTable dexOfferTable;
    private DexContractTable dexContractTable;
    private DexContractDao dexContractDao;
    private TransactionProcessorImpl transactionProcessor;
    private SecureStorageService secureStorageService;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private TimeService timeService;
    private Blockchain blockchain;
    private PhasingPollServiceImpl phasingPollService;
    private DexMatcherServiceImpl dexMatcherService;


    @Inject
    public DexService(EthereumWalletService ethereumWalletService, DexOfferDao dexOfferDao, DexOfferTable dexOfferTable, TransactionProcessorImpl transactionProcessor,
                      DexSmartContractService dexSmartContractService, SecureStorageServiceImpl secureStorageService, DexContractTable dexContractTable,
                      DexOfferTransactionCreator dexOfferTransactionCreator, TimeService timeService, DexContractDao dexContractDao, Blockchain blockchain, PhasingPollServiceImpl phasingPollService,
                      DexMatcherServiceImpl dexMatcherService) {
        this.ethereumWalletService = ethereumWalletService;
        this.dexOfferDao = dexOfferDao;
        this.dexOfferTable = dexOfferTable;
        this.transactionProcessor = transactionProcessor;
        this.dexSmartContractService = dexSmartContractService;
        this.secureStorageService = secureStorageService;
        this.dexContractTable = dexContractTable;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.timeService = timeService;
        this.dexContractDao = dexContractDao;
        this.blockchain = blockchain;
        this.phasingPollService = phasingPollService;
        this.dexMatcherService = dexMatcherService;
    }


    @Transactional
    public DexOffer getOfferByTransactionId(Long transactionId){
        return dexOfferDao.getByTransactionId(transactionId);
    }

    @Transactional
    public DexOffer getOfferById(Long id){
        return dexOfferDao.getById(id);
    }

    /**
     * Use dexOfferTable for insert, to be sure that everything in one transaction.
     */
    @Transactional
    public void saveOffer (DexOffer offer){
        dexOfferTable.insert(offer);
    }

    @Transactional
    public void saveDexContract(ExchangeContract exchangeContract){
        dexContractTable.insert(exchangeContract);
    }

    @Transactional
    public List<ExchangeContract> getDexContracts(DexContractDBRequest dexContractDBRequest){
        return dexContractDao.getAll(dexContractDBRequest);
    }

    @Transactional
    public ExchangeContract getDexContract(DexContractDBRequest dexContractDBRequest){
        return dexContractDao.get(dexContractDBRequest);
    }

    @Transactional
    public List<DexOffer> getOffers(DexOfferDBRequest dexOfferDBRequest){
        return dexOfferDao.getOffers(dexOfferDBRequest);
    }

    @Transactional
    public List<DexOffer> getOffersForMatching(DexOfferDBMatchingRequest dexOfferDBMatchingRequest){
        return dexOfferDao.getOffersForMatching(dexOfferDBMatchingRequest);
    }

    public WalletsBalance getBalances(GetEthBalancesRequest getBalancesRequest){
        List<String> eth = getBalancesRequest.ethAddresses;
        List<EthWalletBalanceInfo> ethWalletsBalance = new ArrayList<>();

        for (String ethAddress : eth) {
            ethWalletsBalance.add(ethereumWalletService.balanceInfo(ethAddress));
        }

        return new WalletsBalance(ethWalletsBalance);
    }

    public List<ExchangeOrder> getHistory(String account, String pair, String type){
        return new ArrayList<>();
    }


    public String withdraw(long accountId, String secretPhrase, String fromAddress,  String toAddress, BigDecimal amount, DexCurrencies currencies, Long transferFee) throws AplException.ExecutiveProcessException{
        if (currencies != null && currencies.isEthOrPax()) {
            return ethereumWalletService.transfer(secretPhrase, accountId, fromAddress, toAddress, amount, transferFee, currencies);
        } else {
            throw new AplException.ExecutiveProcessException("Withdraw not supported for " + currencies.getCurrencyCode());
        }
    }


    public void closeOverdueOrders(Integer time){
        List<DexOffer> offers = dexOfferDao.getOverdueOrders(time);

        for (DexOffer offer : offers) {
            try {
                offer.setStatus(OfferStatus.EXPIRED);
                dexOfferTable.insert(offer);

                refundFrozenMoneyForOffer(offer);
            } catch (AplException.ExecutiveProcessException ex){
                LOG.error(ex.getMessage(), ex);
                //TODO take a look ones again do we need this throw exception there.
//                throw new RuntimeException(ex);
            }
        }

    }

    public void refundFrozenMoneyForOffer(DexOffer offer) throws AplException.ExecutiveProcessException {
        if(DexCurrencyValidator.haveFreezeOrRefundApl(offer)){
            refundAPLFrozenMoney(offer);
        } else if(DexCurrencyValidator.haveFreezeOrRefundEthOrPax(offer)) {
            String passphrase = secureStorageService.getUserPassPhrase(offer.getAccountId());
            if(StringUtils.isNotBlank(passphrase)) {
                refundEthPaxFrozenMoney(passphrase, offer);
            }
        }
    }

    public void refundAPLFrozenMoney(DexOffer offer) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundApl(offer);

        //Return APL.
        Account account = Account.getAccount(offer.getAccountId());
        account.addToUnconfirmedBalanceATM(LedgerEvent.DEX_REFUND_FROZEN_MONEY, offer.getTransactionId(), offer.getOfferAmount());
    }

    public String refundEthPaxFrozenMoney(String passphrase, DexOffer offer) throws AplException.ExecutiveProcessException {
        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(offer);

        String txHash = dexSmartContractService.withdraw(passphrase, offer.getAccountId(), offer.getFromAddress(), new BigInteger(Long.toUnsignedString(offer.getTransactionId())), null, offer.getPairCurrency());

        if(txHash==null){
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }
        return txHash;
    }

    public String freezeEthPax(String passphrase, DexOffer offer) throws ExecutionException, AplException.ExecutiveProcessException {
        String txHash;

        DexCurrencyValidator.checkHaveFreezeOrRefundEthOrPax(offer);

        BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
        txHash = dexSmartContractService.deposit(passphrase, offer.getTransactionId(), offer.getAccountId(), offer.getFromAddress(), EthUtil.etherToWei(haveToPay), null, offer.getPairCurrency());


        if(txHash==null){
            throw new AplException.ExecutiveProcessException("Exception in the process of freezing money.");
        }

        return txHash;
    }

    public void cancelOffer(DexOffer offer) {
        offer.setStatus(OfferStatus.CANCEL);
        saveOffer(offer);
    }

    /**
     * @param cancelTrId  can be null if we just want to check are there any unconfirmed transactions for this order.
     */
    public boolean isThereAnotherCancelUnconfirmedTx(Long orderId, Long cancelTrId){
        try(DbIterator<UnconfirmedTransaction>  tx = transactionProcessor.getAllUnconfirmedTransactions()) {
            while (tx.hasNext()) {
                UnconfirmedTransaction unconfirmedTransaction = tx.next();
                if (TransactionType.TYPE_DEX == unconfirmedTransaction.getTransaction().getType().getType() &&
                        TransactionType.SUBTYPE_DEX_OFFER_CANCEL == unconfirmedTransaction.getTransaction().getType().getSubtype()) {
                    DexOfferCancelAttachment dexOfferCancelAttachment = (DexOfferCancelAttachment) unconfirmedTransaction.getTransaction().getAttachment();

                    if(dexOfferCancelAttachment.getTransactionId() == orderId &&
                            !Objects.equals(unconfirmedTransaction.getTransaction().getId(),cancelTrId)){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Transfer money with approve for APL/ETH/PAX
     * @return Tx hash id/link
     */
    public String transferMoneyWithApproval(CreateTransactionRequest createTransactionRequest, DexOffer offer, String toAddress, byte [] secretHash, ExchangeContractStatus contractStatus) throws AplException.ExecutiveProcessException {
        String transactionStr = null;

        if(offer.getType().isSell()) {
            createTransactionRequest.setRecipientId(Convert.parseAccountId(toAddress));
            createTransactionRequest.setAmountATM(offer.getOfferAmount());
            createTransactionRequest.setDeadlineValue("1440");

            createTransactionRequest.setFeeATM(Constants.ONE_APL * 3);
            PhasingParams phasingParams = new PhasingParams((byte) 5, 0, 1, 0, (byte) 0, null);
            PhasingAppendixV2 phasing = new PhasingAppendixV2(-1, timeService.getEpochTime() + contractStatus.timeOfWaiting(), phasingParams, null, secretHash, (byte) 2);
            createTransactionRequest.setPhased(true);
            createTransactionRequest.setPhasing(phasing);
            //contractStatus.isStep1 doesn't have frozen money.
            createTransactionRequest.setAttachment(new DexControlOfFrozenMoneyAttachment(offer.getTransactionId(), contractStatus.isStep2()));

            try {
                Transaction transaction = dexOfferTransactionCreator.createTransaction(createTransactionRequest);
                transactionStr = transaction != null ? Long.toUnsignedString(transaction.getId()) : null;
            } catch (AplException.ValidationException | ParameterException e) {
                LOG.error(e.getMessage(), e);
                throw new AplException.ExecutiveProcessException(e.getMessage());
            }
        } else if(offer.getType().isBuy() && offer.getPairCurrency().isEthOrPax()){
            BigDecimal haveToPay = EthUtil.aplToEth(offer.getOfferAmount()).multiply(offer.getPairRate());
            String token = null;

            if(offer.getPairCurrency().isPax()){
                token = ethereumWalletService.PAX_CONTRACT_ADDRESS;
            }

            if(contractStatus.isStep1()) {
                transactionStr = dexSmartContractService.depositAndInitiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        offer.getFromAddress(), offer.getTransactionId(),
                        EthUtil.etherToWei(haveToPay),
                        secretHash, toAddress, contractStatus.timeOfWaiting(),
                        null, token);
            } else if (contractStatus.isStep2()){
                transactionStr = dexSmartContractService.initiate(createTransactionRequest.getPassphrase(), createTransactionRequest.getSenderAccount().getId(),
                        offer.getFromAddress(), offer.getTransactionId(), secretHash, toAddress, contractStatus.timeOfWaiting(), null);
            }

        }
        return transactionStr;
    }


    public boolean approveMoneyTransfer(String passphrase, Long accountId, Long orderId, String txId, byte[] secret) throws AplException.ExecutiveProcessException {

        try {
            DexOffer offer = getOfferByTransactionId(orderId);

            CreateTransactionRequest templatTransactionRequest = CreateTransactionRequest
                    .builder()
                    .passphrase(passphrase)
                    .publicKey(Account.getPublicKey(accountId))
                    .senderAccount(Account.getAccount(accountId))
                    .keySeed(Crypto.getKeySeed(Helper2FA.findAplSecretBytes(accountId, passphrase)))
                    .deadlineValue("1440")
                    .feeATM(Constants.ONE_APL * 2)
                    .broadcast(true)
                    .recipientId(0L)
                    .ecBlockHeight(0)
                    .ecBlockId(0L)
                    .build();

            if(offer.getType().isSell()){
                dexSmartContractService.approve(passphrase, secret, offer.getToAddress(), accountId);
                log.info("Transaction:" + txId +" was approved.");

                DexCloseOfferAttachment closeOfferAttachment = new DexCloseOfferAttachment(offer.getTransactionId());
                templatTransactionRequest.setAttachment(closeOfferAttachment);

                Transaction respCloseOffer = dexOfferTransactionCreator.createTransaction(templatTransactionRequest);
                log.info("Order:" + offer.getTransactionId() +" was closed. TxId:" + respCloseOffer.getId());

                //TODO add for make graphics

            } else if(offer.getType().isBuy()){

                Transaction transaction = blockchain.getTransaction(Long.parseUnsignedLong(txId));
                List<byte[]> txHash = new ArrayList<>();
                txHash.add(transaction.getFullHash());

                Attachment attachment = new MessagingPhasingVoteCasting(txHash, secret);
                templatTransactionRequest.setAttachment(attachment);

                Transaction respApproveTx = dexOfferTransactionCreator.createTransaction(templatTransactionRequest);
                log.info("Transaction:" + txId +" was approved. TxId: " + respApproveTx.getId());

                DexCloseOfferAttachment closeOfferAttachment = new DexCloseOfferAttachment(offer.getTransactionId());
                templatTransactionRequest.setAttachment(closeOfferAttachment);

                Transaction respCloseOffer = dexOfferTransactionCreator.createTransaction(templatTransactionRequest);
                log.info("Order:" + offer.getTransactionId() +" was closed. TxId:" + respCloseOffer.getId());

                //TODO add for make graphics
            }
        } catch (Exception ex){
            throw new AplException.ExecutiveProcessException(ex.getMessage());
        }

        return true;
    }


    public JSONStreamAware createOffer(CustomRequestWrapper requestWrapper, Account account, DexOffer offer) throws ParameterException, AplException.ValidationException, AplException.ExecutiveProcessException, ExecutionException {
        DexOffer counterOffer = dexMatcherService.findCounterOffer(offer);
        String freezeTx=null;
        JSONStreamAware response = new JSONObject();

        if (counterOffer != null) {
            // 1. Create offer.
            offer.setStatus(OfferStatus.WAITING_APPROVAL);
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOfferAttachmentV2(offer));
            Transaction offerTx = dexOfferTransactionCreator.createTransaction(createOfferTransactionRequest);
            offer.setTransactionId(offerTx.getId());

            byte[] secretX = new byte[32];
            Crypto.getSecureRandom().nextBytes(secretX);
            byte[] secretHash = Crypto.sha256().digest(secretX);
            String passphrase = ParameterParser.getPassphrase(requestWrapper, true);
            byte[] encryptedSecretX = Crypto.aesGCMEncrypt(secretX, Crypto.sha256().digest(Convert.toBytes(passphrase)));

            // 2. Send money to the counter offer.
            CreateTransactionRequest transferMoneyWithApprovalRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, counterOffer.getAccountId(), offer.getOfferAmount(), null);
            String transactionId = transferMoneyWithApproval(transferMoneyWithApprovalRequest, offer, counterOffer.getToAddress(), secretHash, ExchangeContractStatus.STEP_1);

            if(StringUtils.isBlank(transactionId)){
                throw new AplException.ExecutiveProcessException("Money wasn't send to the counter offer.");
            }

            // 3. Create contract.
            DexContractAttachment contractAttachment = new DexContractAttachment(offer.getTransactionId(), counterOffer.getTransactionId(), secretHash, transactionId, encryptedSecretX, ExchangeContractStatus.STEP_1);
            response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, contractAttachment);
        } else {
            CreateTransactionRequest createOfferTransactionRequest = HttpRequestToCreateTransactionRequestConverter
                    .convert(requestWrapper, account, 0L, 0L, new DexOfferAttachmentV2(offer));
            Transaction tx = dexOfferTransactionCreator.createTransaction(createOfferTransactionRequest);

            if (offer.getPairCurrency().isEthOrPax() && offer.getType().isBuy()) {
                offer.setTransactionId(tx.getId());
                String passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(requestWrapper, true));
                freezeTx = freezeEthPax(passphrase, offer);
            }
            if (freezeTx != null) {
                ((JSONObject) response).put("frozenTx", freezeTx);
            }
        }

        return response;
    }





    public boolean isTxApproved(byte[] secretHash, OfferType offerType, DexCurrencies dexCurrencies, String transferTxId) throws AplException.ExecutiveProcessException {
        if(offerType.isBuy() && dexCurrencies.isEthOrPax()) {
            SwapDataInfo swapDataInfo = dexSmartContractService.getSwapData(secretHash);
            return swapDataInfo != null && swapDataInfo.getSecret() != null;
        } else if(offerType.isSell()) {
            PhasingPollResult phasingPoll = phasingPollService.getResult(Long.parseUnsignedLong(transferTxId));
            return phasingPoll != null && phasingPoll.getApprovedTx() != null;
        }

        throw new AplException.ExecutiveProcessException("Unknown offer pair.");
    }

    public byte[] getSecretIfTxApproved(byte[] secretHash, OfferType offerType, DexCurrencies dexCurrencies, String transferTxId) throws AplException.ExecutiveProcessException {

        if(offerType.isBuy() && dexCurrencies.isEthOrPax()) {
            SwapDataInfo swapDataInfo = dexSmartContractService.getSwapData(secretHash);
            if(swapDataInfo!=null && swapDataInfo.getSecret()!= null) {
                return swapDataInfo.getSecret();
            }
        } else if(offerType.isSell()) {
            PhasingPollResult phasingPoll = phasingPollService.getResult(Long.parseUnsignedLong(transferTxId));
            if(phasingPoll == null){
                return null;
            }

            Long approvedTx = phasingPoll.getApprovedTx();
            Transaction transaction = blockchain.getTransaction(approvedTx);
            MessagingPhasingVoteCasting voteCasting = (MessagingPhasingVoteCasting) transaction.getAttachment();
            return  voteCasting.getRevealedSecret();
        }

        return null;
    }


}

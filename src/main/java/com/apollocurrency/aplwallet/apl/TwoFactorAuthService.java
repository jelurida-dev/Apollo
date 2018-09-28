/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.util.exception.TwoFactoAuthAlreadyRegisteredException;

public interface TwoFactorAuthService {
    /**
     * Enable 2fa for account specified by accountId and return url for representation via QR code and plain secret word
     * representation in Base32 format
     * @param accountId id of account, which want to enable 2fa
     * @return <ul>Dto, which consist of:
     * <li>QR code url for creating QR code and scan it by QR code reader</li>
     * <li>plain 2fa secret word</li>
     * </ul>
     * @throws TwoFactoAuthAlreadyRegisteredException if 2fa was already enabled
     */
    TwoFactorAuthDetails enable(long accountId);

    /**
     * Disable 2fa for account specified by accountId, require authCode to perform 2fa firstly.
     * @param accountId id of account, which want to disable 2fa
     * @param authCode temporal code number based on 2fa secret word and generated by user device, app, etc
     * @throws com.apollocurrency.aplwallet.apl.util.exception.InvalidTwoFactorAuthCredentialsException if 2fa credentials
     * are wrong
     */
    void disable(long accountId, int authCode);

    /**
     * Check is a 2fa enabled for account specified by accountId
     * @param accountId id of account, which 2fa should be verified
     * @return true if account has 2fa or false if account has not 2fa
     */
    boolean isEnabled(long accountId);

    /**
     * Perform 2fa authentication using user account and generated temporal authCode
     * @param accountId id of account which should pass 2fa
     * @param authCode temporal code number based on 2fa secret word and generated by user device, app, etc
     * @return true if authCode is appropriate for this account, otherwise - false
     */
    boolean tryAuth(long accountId, int authCode);

    boolean confirmEnabling(long accountId, int authCode);
}

/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.support.annotation.Nullable;

import timber.log.Timber;

/**
 * This transaction is designed to block the transaction queue while modifying the state of the
 * connection.  This should only be used to reset the connection to a usable state after an error
 * that has been ADDRESSED.  This should not be used to ignore errors.  Where appropriate create
 * a non-gatt library strategy to use this transaction appropriately.
 */

public class SetServerConnectionStateTransaction extends GattServerTransaction {

    private static final String NAME = "SetServerConnectionStateTransaction";
    private GattState destinationState;

    /**
     * For the purposes of this transaction the end state should be {@link GattState#GATT_CONNECTION_STATE_SET_SUCCESSFULLY}.  Achieving this state
     * does not mean that this is the system state, it means that the transaction completed properly, the callback will deliver this state upon success
     * assuming the entry criteria is correct and then set the system to this state.  Present state in the transaction response will not match the actual
     * state.  This is to prevent setting the system to a failure state when the intent is to fix a failure state.
     * @param connection The {@link com.fitbit.bluetooth.fbgatt.GattServerConnection} to perform this operation upon
     * @param successEndState The success end state, in this case {@link GattState#GATT_CONNECTION_STATE_SET_SUCCESSFULLY}
     * @param destinationState The state to set the connection to in the end.  Probably {@link GattState#IDLE} or {@link GattState#DISCONNECTED}
     */
    public SetServerConnectionStateTransaction(@Nullable GattServerConnection connection, GattState successEndState, GattState destinationState) {
        super(connection, successEndState);
        this.destinationState = destinationState;
    }

    public SetServerConnectionStateTransaction(@Nullable GattServerConnection connection, GattState successEndState, GattState destinationState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.destinationState = destinationState;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        GattState previousState = getGattServer().getGattState();
        getGattServer().setState(GattState.GATT_CONNECTION_STATE_SET_IN_PROGRESS);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(NAME);
        if(previousState.equals(destinationState)) {
            Timber.w("[%s] The system is already in this state, can't transition to it, failing tx and returning state to previous.", getDevice());
            builder.gattState(GattState.GATT_CONNECTION_STATE_SET_FAILURE)
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            getGattServer().setState(previousState);
        } else {
            Timber.v("[%s] Setting the state, changing from %s to %s", getDevice(), getGattServer().getGattState(), destinationState);
            getGattServer().setState(destinationState);
            // this should match the destination state
            Timber.v("[%s] State successfully set to %s", getDevice(), getGattServer().getGattState());
            builder.gattState(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY)
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
        }
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
    }

    @Override
    public String getName() {
        return NAME;
    }
}

/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class ImportKeysListEntry implements Serializable, Parcelable {
    private static final long serialVersionUID = -7797972103284992662L;

    public ArrayList<String> userIds;
    public long keyId;
    public String keyIdHex;
    public boolean revoked;
    public Date date; // TODO: not displayed
    public String fingerPrintHex;
    public int bitStrength;
    public String algorithm;
    public boolean secretKey;
    public String mPrimaryUserId;

    private boolean mSelected;

    private byte[] mBytes = new byte[]{};

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPrimaryUserId);
        dest.writeStringList(userIds);
        dest.writeLong(keyId);
        dest.writeByte((byte) (revoked ? 1 : 0));
        dest.writeSerializable(date);
        dest.writeString(fingerPrintHex);
        dest.writeString(keyIdHex);
        dest.writeInt(bitStrength);
        dest.writeString(algorithm);
        dest.writeByte((byte) (secretKey ? 1 : 0));
        dest.writeByte((byte) (mSelected ? 1 : 0));
        dest.writeInt(mBytes.length);
        dest.writeByteArray(mBytes);
    }

    public static final Creator<ImportKeysListEntry> CREATOR = new Creator<ImportKeysListEntry>() {
        public ImportKeysListEntry createFromParcel(final Parcel source) {
            ImportKeysListEntry vr = new ImportKeysListEntry();
            vr.mPrimaryUserId = source.readString();
            vr.userIds = new ArrayList<String>();
            source.readStringList(vr.userIds);
            vr.keyId = source.readLong();
            vr.revoked = source.readByte() == 1;
            vr.date = (Date) source.readSerializable();
            vr.fingerPrintHex = source.readString();
            vr.keyIdHex = source.readString();
            vr.bitStrength = source.readInt();
            vr.algorithm = source.readString();
            vr.secretKey = source.readByte() == 1;
            vr.mSelected = source.readByte() == 1;
            vr.mBytes = new byte[source.readInt()];
            source.readByteArray(vr.mBytes);

            return vr;
        }

        public ImportKeysListEntry[] newArray(final int size) {
            return new ImportKeysListEntry[size];
        }
    };

    public String getKeyIdHex() {
        return keyIdHex;
    }

    public byte[] getBytes() {
        return mBytes;
    }

    public void setBytes(byte[] bytes) {
        this.mBytes = bytes;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public void setKeyIdHex(String keyIdHex) {
        this.keyIdHex = keyIdHex;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getFingerPrintHex() {
        return fingerPrintHex;
    }

    public void setFingerPrintHex(String fingerPrintHex) {
        this.fingerPrintHex = fingerPrintHex;
    }

    public int getBitStrength() {
        return bitStrength;
    }

    public void setBitStrength(int bitStrength) {
        this.bitStrength = bitStrength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isSecretKey() {
        return secretKey;
    }

    public void setSecretKey(boolean secretKey) {
        this.secretKey = secretKey;
    }

    public ArrayList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = userIds;
    }

    public String getPrimaryUserId() {
        return mPrimaryUserId;
    }

    public void setPrimaryUserId(String uid) {
        mPrimaryUserId = uid;
    }

    /**
     * Constructor for later querying from keyserver
     */
    public ImportKeysListEntry() {
        // keys from keyserver are always public keys
        secretKey = false;
        // do not select by default
        mSelected = false;
        userIds = new ArrayList<String>();
    }

    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(Context context, PGPKeyRing pgpKeyRing) {
        // save actual key object into entry, used to import it later
        try {
            this.mBytes = pgpKeyRing.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException on pgpKeyRing.getEncoded()", e);
        }

        // selected is default
        this.mSelected = true;

        if (pgpKeyRing instanceof PGPSecretKeyRing) {
            secretKey = true;
        } else {
            secretKey = false;
        }
        PGPPublicKey key = pgpKeyRing.getPublicKey();

        userIds = new ArrayList<String>();
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            userIds.add(userId);
            for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignaturesForID(userId))) {
                if (sig.getHashedSubPackets() != null
                        && sig.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.PRIMARY_USER_ID)) {
                    try {
                        // make sure it's actually valid
                        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), key);
                        if (sig.verifyCertification(userId, key)) {
                            mPrimaryUserId = userId;
                        }
                    } catch (Exception e) {
                        // nothing bad happens, the key is just not considered the primary key id
                    }
                }

            }
        }
        // if there was no user id flagged as primary, use the first one
        if (mPrimaryUserId == null) {
            mPrimaryUserId = userIds.get(0);
        }

        this.keyId = key.getKeyID();
        this.keyIdHex = PgpKeyHelper.convertKeyIdToHex(keyId);

        this.revoked = key.isRevoked();
        this.fingerPrintHex = PgpKeyHelper.convertFingerprintToHex(key.getFingerprint());
        this.bitStrength = key.getBitStrength();
        final int algorithm = key.getAlgorithm();
        this.algorithm = PgpKeyHelper.getAlgorithmInfo(context, algorithm);
    }
}

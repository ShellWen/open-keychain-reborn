/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.ui;

import com.shellwen.keychainreborn.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


/**
 * Signs the specified public key with the specified secret master key
 */
public class CertifyKeyActivity extends BaseActivity {

    public static final String EXTRA_RESULT = "operation_result";
    // For sending masterKeyIds to MultiUserIdsFragment to display list of keys
    public static final String EXTRA_KEY_IDS = MultiUserIdsFragment.EXTRA_KEY_IDS ;
    public static final String EXTRA_CERTIFY_KEY_ID = "certify_key_id";

    @Override
    protected void initLayout() {
        setContentView(R.layout.certify_key_activity);
    }

}

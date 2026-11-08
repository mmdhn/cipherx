package com.cipherx.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CipherXNativePlugin.class);
        super.onCreate(savedInstanceState);
    }
}

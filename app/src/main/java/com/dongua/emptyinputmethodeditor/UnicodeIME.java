package com.dongua.emptyinputmethodeditor;

/**
 * Created by dongua on 17-9-6.
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.nio.charset.Charset;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;


import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.beetstra.jutf7.CharsetProvider;


public class UnicodeIME extends InputMethodService {
    private static final String TAG = "AppiumUnicodeIME";

    // encodings

    private static final Charset M_UTF7 = Charset.forName("x-IMAP-mailbox-name");
//    private static final Charset M_UTF7 = new CharsetProvider().charsetForName(UTF7_NAME);
    private static final Charset ASCII  = Charset.forName("US-ASCII");

    private static final CharsetDecoder decoder = M_UTF7.newDecoder();

    // markers of UTF-7 content
    private static final char M_UTF7_SHIFT   = '&';
    private static final char M_UTF7_UNSHIFT = '-';

    // local state
    private boolean isShifted;
    private long metaState;
    private StringBuilder unicodeString;


    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.i(TAG, "onStartInput");
        super.onStartInput(attribute, restarting);

        if (!restarting) {
            metaState = 0;
            isShifted = false;
        }
        unicodeString = null;
    }

    @Override
    public void onFinishInput() {
        Log.i(TAG, "onFinishInput");
        super.onFinishInput();
        unicodeString = null;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public boolean onEvaluateInputViewShown() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown (keyCode='" + keyCode + "', event.keyCode='" + event.getKeyCode() + "', metaState='" + event.getMetaState() + "')");
        int c = getUnicodeChar(keyCode, event);

        if (c == 0) {
            return super.onKeyDown(keyCode, event);
        }

        if (!isShifted) {
            if (c == M_UTF7_SHIFT) {
                shift();
                return true;
            } else if (isAsciiPrintable(c)) {
                commitChar(c);
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        } else {
            if (c == M_UTF7_UNSHIFT) {
                unshift();
            } else {
                appendChar(c);
            }
            return true;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp (keyCode='" + keyCode + "', event.keyCode='" + event.getKeyCode() + "', metaState='" + event.getMetaState() + "')");
        metaState = MetaKeyKeyListener.handleKeyUp(metaState, keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    private void shift() {
        isShifted = true;
        unicodeString = new StringBuilder();
        appendChar(M_UTF7_SHIFT);
    }

    private void unshift() {
        isShifted = false;
        unicodeString.append(M_UTF7_UNSHIFT);
        String decoded = decodeUtf7(unicodeString.toString());
        getCurrentInputConnection().commitText(decoded, 1);
        unicodeString = null;
    }

    private int getUnicodeChar(int keyCode, KeyEvent event) {
        metaState = MetaKeyKeyListener.handleKeyDown(metaState, keyCode, event);
        int c = event.getUnicodeChar(event.getMetaState());
        metaState = MetaKeyKeyListener.adjustMetaAfterKeypress(metaState);
        return c;
    }

    private void commitChar(int c) {
        getCurrentInputConnection().commitText(String.valueOf((char) c), 1);
    }

    private void appendChar(int c) {
        unicodeString.append((char) c);
    }

    private String decodeUtf7(String encStr) {
        ByteBuffer encoded = ByteBuffer.wrap(encStr.getBytes(ASCII));
        String decoded;
        try {
            CharBuffer buf = decoder.decode(encoded);
            decoded = buf.toString();
        } catch (CharacterCodingException e) {
            decoded = encStr;
        }
        return decoded;
    }

    private boolean isAsciiPrintable(int c) {
        return c >= 0x20 && c <= 0x7E;
    }
}

/*
 * Created on 21/03/2005
 *
 * JRandTest package
 *
 * Copyright (c) 2005, Zur Aougav, aougav@hotmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list 
 * of conditions and the following disclaimer. 
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this 
 * list of conditions and the following disclaimer in the documentation and/or 
 * other materials provided with the distribution. 
 * 
 * Neither the name of the JRandTest nor the names of its contributors may be 
 * used to endorse or promote products derived from this software without specific 
 * prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fasteasytrade.jrandtest.algo;

import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import com.fasteasytrade.jrandtest.io.FileAlgoRandomStream;

/**
 * Simple java TrippleDES algorithm as a random stream.
 * <p>
 * Use JCE DESEDE algorithm.
 * <p>
 * 
 * @author Zur Aougav
 */
public class TripleDES extends FileAlgoRandomStream {

    /**
     * actual cipher algorithm
     */
    javax.crypto.Cipher algo = null;

    /**
     * encrypt buffer full of 0x00's, only if no file exists
     */
    byte[] inAlgoBuffer = null;

    /**
     * ecrypted result, only if no file exists
     */
    byte[] outAlgoBuffer = null;

    /**
     * index in to encrypted result, outAlgoBuffer
     */
    int outAlgoBufferIx;

    public TripleDES() {
        super();
    }

    public TripleDES(String keyFileName) {
        super(keyFileName);
    }

    @Override
    public void setupKeys() {

        super.setupKeys();

    }

    @Override
    public void setup() {

        try {
            algo = javax.crypto.Cipher.getInstance("DESEDE/CFB8/NoPadding", "SunJCE"); ///);
            byte[] v = new byte[8 * 3];
            for (int i = 0; i < v.length && i < publicKey.length; i++) {
                v[i] = publicKey[i];
            }
            java.security.Key key = new SecretKeySpec(v, "DESEDE");
            algo.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
        } catch (Exception e) {
            System.out.println(e);
        }

        inAlgoBuffer = new byte[algo.getBlockSize()]; // always 0x00's
        outAlgoBufferIx = algo.getOutputSize(algo.getBlockSize());
        outAlgoBuffer = new byte[outAlgoBufferIx]; // output of encryption
    }

    @Override
    public boolean openInputStream() {

        if (filename != null) {
            super.openInputStream();
        }

        setup(); // make sure algorithm and keys/states are reset
        count = 0;
        countLastRead = SIZE;
        actualSize = SIZE;
        outAlgoBufferIx = outAlgoBuffer.length;
        open = true;
        return open;
    }

    @Override
    public byte readByte() {
        if (!isOpen()) {
            return -1;
        }

        if (filename == null && count > maxCount) {
            open = false;
            return -1;
        }

        byte prng = -1;

        /*
         * encrypt file if exists
         */
        if (filename != null) {
            if (countLastRead == actualSize) { // end of buffer?
                super.readByte(); // read & fill buffer from file
                if (!isOpen()) {
                    return -1;
                }
                countLastRead = 0;
                try {
                    algo.update(buffer, 0, actualSize, buffer);
                } catch (ShortBufferException e) {
                    throw new IllegalStateException(e);
                } // encrypt it
            }

            prng = buffer[countLastRead++];
            count++;
            return prng;
        }

        /*
         * we have no real filename to encrypt
         */
        if (outAlgoBufferIx == outAlgoBuffer.length) {
            outAlgoBuffer = algo.update(inAlgoBuffer);
            outAlgoBufferIx = 0;
        }

        prng = outAlgoBuffer[outAlgoBufferIx++];
        count++;
        return prng;
    }

    @Override
    public int readInt() {
        if (!isOpen()) {
            return -1;
        }

        if (filename == null && count > maxCount) {
            open = false;
            return -1;
        }

        int prng = 0xff & readByte();
        prng = (prng << 8) | (0xff & readByte());
        prng = (prng << 8) | (0xff & readByte());
        prng = (prng << 8) | (0xff & readByte());

        return prng;
    }

    @Override
    public long readLong() {
        if (!isOpen()) {
            return -1;
        }

        if (filename == null && count > maxCount) {
            open = false;
            return -1;
        }

        long prng = 0xff & readByte();
        for (int i = 0; i < 7; i++) {
            prng = (prng << 8) | (0xff & readByte());
        }

        return prng;
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            TripleDES algo = new TripleDES();
            algo.setup();
            try {
                algo.openInputStream();
                for (int i = 0; i < 100; i++) {
                    System.out.print(algo.readByte());
                    System.out.print(",");
                }
                System.out.println();
            } catch (Exception e) {
                System.out.println("" + e);
            }
        }
    }
}
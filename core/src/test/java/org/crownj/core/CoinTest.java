/*
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crownj.core;

import static org.crownj.core.Coin.*;
import static org.crownj.core.NetworkParameters.MAX_MONEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.math.BigDecimal;

public class CoinTest {

    @Test
    public void testParseCoin() {
        // String version
        assertEquals(CENT, parseCoin("0.01"));
        assertEquals(CENT, parseCoin("1E-2"));
        assertEquals(COIN.add(CENT), parseCoin("1.01"));
        assertEquals(COIN.negate(), parseCoin("-1"));
        try {
            parseCoin("2E-20");
            org.junit.Assert.fail("should not have accepted fractional satoshis");
        } catch (IllegalArgumentException expected) {
        } catch (Exception e) {
            org.junit.Assert.fail("should throw IllegalArgumentException");
        }
        assertEquals(1, parseCoin("0.00000001").value);
        assertEquals(1, parseCoin("0.000000010").value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseCoinOverprecise() {
        parseCoin("0.000000011");
    }

    @Test
    public void testParseCoinInexact() {
        assertEquals(1, parseCoinInexact("0.00000001").value);
        assertEquals(1, parseCoinInexact("0.000000011").value);
    }

    @Test
    public void testValueOf() {
        // int version
        assertEquals(CENT, valueOf(0, 1));
        assertEquals(SATOSHI, valueOf(1));
        assertEquals(NEGATIVE_SATOSHI, valueOf(-1));
        assertEquals(MAX_MONEY, valueOf(MAX_MONEY.value));
        assertEquals(MAX_MONEY.negate(), valueOf(MAX_MONEY.value * -1));
        valueOf(MAX_MONEY.value + 1);
        valueOf((MAX_MONEY.value * -1) - 1);
        valueOf(Long.MAX_VALUE);
        valueOf(Long.MIN_VALUE);

        try {
            valueOf(1, -1);
            fail();
        } catch (IllegalArgumentException e) {}
        try {
            valueOf(-1, 0);
            fail();
        } catch (IllegalArgumentException e) {}
    }


    @Test
    public void testCRWToSatoshi() {
        assertEquals(Long.MIN_VALUE, CRWToSatoshi(new BigDecimal("-92233720368.54775808")));
        assertEquals(0L, CRWToSatoshi(BigDecimal.ZERO));
        assertEquals(COIN.value, CRWToSatoshi(BigDecimal.ONE));
        assertEquals(Long.MAX_VALUE, CRWToSatoshi(new BigDecimal("92233720368.54775807")));
    }

    @Test(expected = ArithmeticException.class)
    public void testCRWToSatoshi_tooSmall() {
        CRWToSatoshi(new BigDecimal("-92233720368.54775809")); // .00000001 less than minimum value
    }

    @Test(expected = ArithmeticException.class)
    public void testCRWToSatoshi_tooBig() {
        CRWToSatoshi(new BigDecimal("92233720368.54775808")); // .00000001 more than maximum value
    }

    @Test(expected = ArithmeticException.class)
    public void testCRWToSatoshi_tooPrecise1() {
        CRWToSatoshi(new BigDecimal("0.000000001")); // More than SMALLEST_UNIT_EXPONENT precision
    }

    @Test(expected = ArithmeticException.class)
    public void testCRWToSatoshi_tooPrecise2() {
        CRWToSatoshi(new BigDecimal("92233720368.547758079")); // More than SMALLEST_UNIT_EXPONENT precision
    }

    @Test
    public void testSatoshiToCRW() {
        assertThat(new BigDecimal("-92233720368.54775808"),  Matchers.comparesEqualTo(satoshiToCRW(Long.MIN_VALUE)));
        assertThat(new BigDecimal("-0.00000001"), Matchers.comparesEqualTo(satoshiToCRW(NEGATIVE_SATOSHI.value)));
        assertThat(BigDecimal.ZERO, Matchers.comparesEqualTo(satoshiToCRW(0L)));
        assertThat(new BigDecimal("0.00000001"), Matchers.comparesEqualTo(satoshiToCRW(SATOSHI.value)));
        assertThat(BigDecimal.ONE,  Matchers.comparesEqualTo(satoshiToCRW(COIN.value)));
        assertThat(new BigDecimal(50),  Matchers.comparesEqualTo(satoshiToCRW(FIFTY_COINS.value)));
        assertThat(new BigDecimal("92233720368.54775807"),  Matchers.comparesEqualTo(satoshiToCRW(Long.MAX_VALUE)));
    }

    @Test
    public void testOfCRW() {
        assertEquals(Coin.valueOf(Long.MIN_VALUE), Coin.ofCRW(new BigDecimal("-92233720368.54775808")));
        assertEquals(ZERO, Coin.ofCRW(BigDecimal.ZERO));
        assertEquals(COIN, Coin.ofCRW(BigDecimal.ONE));
        assertEquals(Coin.valueOf(Long.MAX_VALUE), Coin.ofCRW(new BigDecimal("92233720368.54775807")));
    }

    @Test
    public void testOperators() {
        assertTrue(SATOSHI.isPositive());
        assertFalse(SATOSHI.isNegative());
        assertFalse(SATOSHI.isZero());
        assertFalse(NEGATIVE_SATOSHI.isPositive());
        assertTrue(NEGATIVE_SATOSHI.isNegative());
        assertFalse(NEGATIVE_SATOSHI.isZero());
        assertFalse(ZERO.isPositive());
        assertFalse(ZERO.isNegative());
        assertTrue(ZERO.isZero());

        assertTrue(valueOf(2).isGreaterThan(valueOf(1)));
        assertFalse(valueOf(2).isGreaterThan(valueOf(2)));
        assertFalse(valueOf(1).isGreaterThan(valueOf(2)));
        assertTrue(valueOf(1).isLessThan(valueOf(2)));
        assertFalse(valueOf(2).isLessThan(valueOf(2)));
        assertFalse(valueOf(2).isLessThan(valueOf(1)));
    }

    @Test(expected = ArithmeticException.class)
    public void testMultiplicationOverflow() {
        Coin.valueOf(Long.MAX_VALUE).multiply(2);
    }

    @Test(expected = ArithmeticException.class)
    public void testMultiplicationUnderflow() {
        Coin.valueOf(Long.MIN_VALUE).multiply(2);
    }

    @Test(expected = ArithmeticException.class)
    public void testAdditionOverflow() {
        Coin.valueOf(Long.MAX_VALUE).add(Coin.SATOSHI);
    }

    @Test(expected = ArithmeticException.class)
    public void testSubtractionUnderflow() {
        Coin.valueOf(Long.MIN_VALUE).subtract(Coin.SATOSHI);
    }

    @Test
    public void testToCRW() {
        assertThat(new BigDecimal("-92233720368.54775808"),  Matchers.comparesEqualTo(Coin.valueOf(Long.MIN_VALUE).toCRW()));
        assertThat(new BigDecimal("-0.00000001"), Matchers.comparesEqualTo(NEGATIVE_SATOSHI.toCRW()));
        assertThat(BigDecimal.ZERO, Matchers.comparesEqualTo(ZERO.toCRW()));
        assertThat(new BigDecimal("0.00000001"), Matchers.comparesEqualTo(SATOSHI.toCRW()));
        assertThat(BigDecimal.ONE,  Matchers.comparesEqualTo(COIN.toCRW()));
        assertThat(new BigDecimal(50),  Matchers.comparesEqualTo(FIFTY_COINS.toCRW()));
        assertThat(new BigDecimal("92233720368.54775807"),  Matchers.comparesEqualTo(Coin.valueOf(Long.MAX_VALUE).toCRW()));
    }

    @Test
    public void testToFriendlyString() {
        assertEquals("1.00 CRW", COIN.toFriendlyString());
        assertEquals("1.23 CRW", valueOf(1, 23).toFriendlyString());
        assertEquals("0.001 CRW", COIN.divide(1000).toFriendlyString());
        assertEquals("-1.23 CRW", valueOf(1, 23).negate().toFriendlyString());
    }

    /**
     * Test the crownValueToPlainString amount formatter
     */
    @Test
    public void testToPlainString() {
        assertEquals("0.0015", Coin.valueOf(150000).toPlainString());
        assertEquals("1.23", parseCoin("1.23").toPlainString());

        assertEquals("0.1", parseCoin("0.1").toPlainString());
        assertEquals("1.1", parseCoin("1.1").toPlainString());
        assertEquals("21.12", parseCoin("21.12").toPlainString());
        assertEquals("321.123", parseCoin("321.123").toPlainString());
        assertEquals("4321.1234", parseCoin("4321.1234").toPlainString());
        assertEquals("54321.12345", parseCoin("54321.12345").toPlainString());
        assertEquals("654321.123456", parseCoin("654321.123456").toPlainString());
        assertEquals("7654321.1234567", parseCoin("7654321.1234567").toPlainString());
        assertEquals("87654321.12345678", parseCoin("87654321.12345678").toPlainString());

        // check there are no trailing zeros
        assertEquals("1", parseCoin("1.0").toPlainString());
        assertEquals("2", parseCoin("2.00").toPlainString());
        assertEquals("3", parseCoin("3.000").toPlainString());
        assertEquals("4", parseCoin("4.0000").toPlainString());
        assertEquals("5", parseCoin("5.00000").toPlainString());
        assertEquals("6", parseCoin("6.000000").toPlainString());
        assertEquals("7", parseCoin("7.0000000").toPlainString());
        assertEquals("8", parseCoin("8.00000000").toPlainString());
    }
}

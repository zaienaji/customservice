package com.infinite.inventory.util;

import java.math.BigDecimal;

public class Util {
	
	public static void SleepInSecond(int second) {
		try {
			Thread.sleep(second*1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isZero(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO) == 0;
	}

	public static boolean isNegative(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO)<=0;
	}
	
	public static boolean isNonZero(BigDecimal number) {
		return number != null && number.compareTo(BigDecimal.ZERO)!=0;
	}
	
	public static boolean isNullOrZero(BigDecimal number) {
		return !isNonZero(number);
	}

}

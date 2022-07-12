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
	
	public static boolean isZero(BigDecimal movementQuantity) {
		return movementQuantity.compareTo(BigDecimal.ZERO) == 0;
	}

	public static boolean isNegative(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO)<=0;
	}

}

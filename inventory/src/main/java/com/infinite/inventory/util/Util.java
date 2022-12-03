package com.infinite.inventory.util;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.infinite.inventory.sharedkernel.MaterialTransaction;

public class Util {
	
	public static void SleepInSecond(int second) {
		try {
			Thread.sleep(second*1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void SleepInMilis(int milisecond) {
		try {
			Thread.sleep(milisecond);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isZero(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO) == 0;
	}

	public static boolean isZeroOrNegative(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO)<=0;
	}
	
	public static boolean isNegative(BigDecimal number) {
		return number.compareTo(BigDecimal.ZERO)<0;
	}
	
	public static boolean isNonZero(BigDecimal number) {
		return number != null && number.compareTo(BigDecimal.ZERO)!=0;
	}
	
	public static boolean isNullOrZero(BigDecimal number) {
		return !isNonZero(number);
	}
	
	public static Optional<Integer> findNode(LinkedList<MaterialTransaction> cache, MaterialTransaction key){
		
		int index = 0;
		for (MaterialTransaction materialTransaction : cache) {
			if (StringUtils.equals(materialTransaction.getCorrelationId(), key.getCorrelationId()))
				return Optional.of(index);
			
			index++;
		}
		
		
		return Optional.empty();
	}

}

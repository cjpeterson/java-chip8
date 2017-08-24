package main;

import java.awt.Toolkit;

public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		byte a = (byte) 0xFF;
		System.out.println((a << 7) & 0x80);
	}

}

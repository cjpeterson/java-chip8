package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Interpreter extends JApplet implements KeyListener, Runnable
{
	private short currentOpcode;
	private byte[] memory;
	private byte[] cpu;
	private short indexRegister;
	private short programCounter;
	private boolean[] gfx;
	private short[] stack;
	private short stackPointer;
	private boolean[] keyState;
	private byte delayTimer;
	private byte soundTimer;
	private int keyPressed;
	private boolean screenUpdated;
	private boolean soundChanged;
	
	public void init()
	{
		memory = new byte[4096];
		cpu = new byte[16];
		gfx = new boolean[64 * 32];
		stack = new short[16];
		stackPointer = 0;
		keyState = new boolean[16];
		this.addKeyListener(this);
		setFocusable(true);
        requestFocusInWindow();
		
		int[] fontSet = new int[]
				{ 
				  0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
				  0x20, 0x60, 0x20, 0x20, 0x70, // 1
				  0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
				  0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
				  0x90, 0x90, 0xF0, 0x10, 0x10, // 4
				  0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
				  0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
				  0xF0, 0x10, 0x20, 0x40, 0x40, // 7
				  0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
				  0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
				  0xF0, 0x90, 0xF0, 0x90, 0x90, // A
				  0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
				  0xF0, 0x80, 0x80, 0x80, 0xF0, // C
				  0xE0, 0x90, 0x90, 0x90, 0xE0, // D
				  0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
				  0xF0, 0x80, 0xF0, 0x80, 0x80  // F
				};
		for (int i = 80; i < 80 + fontSet.length; ++i)
			memory[i] = (byte) fontSet[i-80];
	}
	
	public void paint(Graphics g)
	{
//		g.setColor(Color.BLACK);
//		g.fillRect(0, 0, this.getWidth(), this.getHeight());
//		g.setColor(Color.WHITE);
		for (int i = 0; i < 64 * 32; ++i)
		{
			if (gfx[i])
				g.setColor(Color.WHITE);
			else
				g.setColor(Color.BLACK);
			g.fillRect(((i%64) * this.getWidth())/64,
					((i/64) * this.getHeight())/32,
					this.getWidth()/64 + 1,
					this.getHeight()/32 + 1);
		}
	}
	
	public void start()
	{
		JFileChooser fileChooser = new JFileChooser();
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File f = fileChooser.getSelectedFile();
		try
		{
			FileInputStream in = new FileInputStream(f);
			programCounter = 0x200;
			while (in.available() > 0)
			{
				memory[programCounter] = (byte) in.read();
				++programCounter;
			}
			programCounter = 0x200;
			in.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Couldn't find the file.");
			return;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Thread t = new Thread(this);
		t.start();
	}
	
	@Override
	public void run()
	{
		try {
	          Synthesizer synthesizer = MidiSystem.getSynthesizer();
	          synthesizer.open();
	          MidiChannel channel = synthesizer.getChannels()[0];

	          channel.programChange(synthesizer.getAvailableInstruments()[81].getPatch().getProgram());
	          
	          
		
		
		long lastSystemTime = System.currentTimeMillis();
		//for fps limiting
		int framecounter = 0;
		while(true)
		{
			//get opcode
			currentOpcode = (short) ((memory[programCounter] << 8) + (memory[programCounter+1] & 0xFF));
			programCounter += 2;
			
			if (System.currentTimeMillis() - lastSystemTime > 16)
			{
				lastSystemTime = System.currentTimeMillis();
				if (delayTimer > 0)
					--delayTimer;
				if (soundTimer > 0)
					--soundTimer;
			}
			if (soundTimer == 0)
				channel.noteOff(60);
			
			opcode();
			if (screenUpdated && framecounter >= 8)
			{
				screenUpdated = false;
				framecounter = 0;
				repaint();
			}
			if (soundChanged)
			{
				soundChanged = false;
				channel.noteOn(60, 50);
			}
			
			try
			{
				Thread.sleep(2);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			++framecounter;
			//System.out.println(programCounter + ", " + currentOpcode);
		}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void opcode()
	{
		if ((currentOpcode & 0xF000) == 0x0000)
		{
			if (currentOpcode == (short)0x00E0)
			{
				//clear screen
				for (int i = 0; i < gfx.length; ++i)
					gfx[i] = false;
				screenUpdated = true;
			}
			else if (currentOpcode == (short)0x00EE)
			{
				//return from subroutine
				stackPointer--;
				programCounter = stack[stackPointer];
			}
			else
				//Calls RCA 1802 program at address NNN
				//CONSIDERED DEPRECATED
				;
		}
		else if ((currentOpcode & 0xF000) == 0x1000)
		{
			//jump to address
			//i don't think we store the address on the stack, guide
			//stack[stackPointer] = programCounter;
			//++stackPointer;
			programCounter = (short) (0x0FFF & currentOpcode);
		}
		else if ((currentOpcode & 0xF000) == 0x2000)
		{
			//call subroutine
			stack[stackPointer] = programCounter;
			stackPointer++;
			programCounter = (short) (0x0FFF & currentOpcode);
		}
		else if ((currentOpcode & 0xF000) == 0x3000)
		{
			//skip instruction if VX == NN
			if (cpu[(0x0F00 & currentOpcode) >>> 8] == (byte) (0x00FF & currentOpcode))
				programCounter += 2;
		}
		else if ((currentOpcode & 0xF000) == 0x4000)
		{
			//skip instruction if VX != NN
			if (cpu[(0x0F00 & currentOpcode) >>> 8] != (byte) (0x00FF & currentOpcode))
				programCounter += 2;
		}
		else if ((currentOpcode & 0xF000) == 0x5000)
		{
			//skip instruction if VX == VY
			if (cpu[(0x0F00 & currentOpcode) >>> 8] == cpu[(0x00F0 & currentOpcode) >>> 4])
				programCounter += 2;
		}
		else if ((currentOpcode & 0xF000) == 0x6000)
			//set VX = NN
			cpu[(0x0F00 & currentOpcode) >>> 8] = (byte) (0x00FF & currentOpcode);
		else if ((currentOpcode & 0xF000) == 0x7000)
			//set VX += NN
			cpu[(0x0F00 & currentOpcode) >>> 8] += (byte) (0x00FF & currentOpcode);
		else if ((currentOpcode & 0xF000) == 0x8000)
		{
			if ((currentOpcode & 0xF) == 0)
				cpu[(0x0F00 & currentOpcode) >>> 8] = cpu[(0x00F0 & currentOpcode) >>> 4];
			else if ((currentOpcode & 0xF) == 1)
				cpu[(0x0F00 & currentOpcode) >>> 8] |= cpu[(0x00F0 & currentOpcode) >>> 4];
			else if ((currentOpcode & 0xF) == 2)
				cpu[(0x0F00 & currentOpcode) >>> 8] &= cpu[(0x00F0 & currentOpcode) >>> 4];
			else if ((currentOpcode & 0xF) == 3)
				cpu[(0x0F00 & currentOpcode) >>> 8] ^= cpu[(0x00F0 & currentOpcode) >>> 4];
			else if ((currentOpcode & 0xF) == 4)
			{
				//carry flag
				cpu[15] = 0;
				byte definiteCarries = (byte) (((0x0F00 & currentOpcode) >>> 8) & ((0x00F0 & currentOpcode) >>> 4));
				byte canCarries = (byte) (((0x0F00 & currentOpcode) >>> 8) | ((0x00F0 & currentOpcode) >>> 4));
				while ((canCarries & 0x80) == 128)
				{
					if ((definiteCarries & 0x80) == 128)
					{
						cpu[15] = 1;
						break;
					}
					definiteCarries = (byte) (definiteCarries << 1);
					canCarries = (byte) (canCarries << 1);
				}
				// add VY to VX
				cpu[(0x0F00 & currentOpcode) >>> 8] += cpu[(0x00F0 & currentOpcode) >>> 4];
			}
			else if ((currentOpcode & 0xF) == 5)
			{
				//borrow flag
				/*
				if (cpu[(0x0F00 & currentOpcode) >>> 8] < 0)
				{
					if (cpu[(0x00F0 & currentOpcode) >>> 4] < 0)
					{
						if (cpu[(0x0F00 & currentOpcode) >>> 8] < cpu[(0x00F0 & currentOpcode) >>> 4])
							cpu[15] = 0;
						else
							cpu[15] = 1;
					}
					else
						cpu[15] = 1;
				}
				else
				{
					if (cpu[(0x00F0 & currentOpcode) >>> 4] < 0)
						cpu[15] = 0;
					else if (cpu[(0x0F00 & currentOpcode) >>> 8] < cpu[(0x00F0 & currentOpcode) >>> 4])
						cpu[15] = 0;
					else
						cpu[15] = 1;
				}
				**/
				if ((cpu[(0x00F0 & currentOpcode) >>> 4] & 0xFF) > (cpu[(0x0F00 & currentOpcode) >>> 8] & 0xFF))
					cpu[15] = 0;
				else
					cpu[15] = 1;
				// subtract VY from VX
				cpu[(0x0F00 & currentOpcode) >>> 8] -= cpu[(0x00F0 & currentOpcode) >>> 4];
			}
			else if ((currentOpcode & 0xF) == 6)
			{
				//shift VX right, store lost bit
				cpu[15] = (byte) (cpu[(0x0F00 & currentOpcode) >>> 8] & 1);
				cpu[(0x0F00 & currentOpcode) >>> 8] = (byte) ((cpu[(0x0F00 & currentOpcode) >>> 8] >>> 1) & 0x7F);
			}
			else if ((currentOpcode & 0xF) == 7)
			{
				//borrow flag
				/*
				if (cpu[(0x00F0 & currentOpcode) >>> 4] < 0)
				{
					if (cpu[(0x0F00 & currentOpcode) >>> 8] < 0)
					{
						if (cpu[(0x00F0 & currentOpcode) >>> 4] < cpu[(0x0F00 & currentOpcode) >>> 8])
							cpu[15] = 0;
						else
							cpu[15] = 1;
					}
					else
						cpu[15] = 1;
				}
				else
				{
					if (cpu[(0x0F00 & currentOpcode) >>> 8] < 0)
						cpu[15] = 0;
					else if (cpu[(0x00F0 & currentOpcode) >>> 4] < cpu[(0x0F00 & currentOpcode) >>> 8])
						cpu[15] = 0;
					else
						cpu[15] = 1;
				}
				**/
				if ((cpu[(0x0F00 & currentOpcode) >>> 8] & 0xFF) > (cpu[(0x00F0 & currentOpcode) >>> 4] & 0xFF))
					cpu[15] = 0;
				else
					cpu[15] = 1;
				// subtract VX from VY and store in VX
				cpu[(0x0F00 & currentOpcode) >>> 8] = (byte) (cpu[(0x00F0 & currentOpcode) >>> 4] - cpu[(0x0F00 & currentOpcode) >>> 8]);
			}
			else if ((currentOpcode & 0xF) == 0xE)
			{
				// keep lost bit
				cpu[15] = (byte) (cpu[(0x0F00 & currentOpcode) >>> 8] < 0 ? 1 : 0);
				//shift VX left 1
				cpu[(0x0F00 & currentOpcode) >>> 8] <<= 1;
			}
		}
		else if ((currentOpcode & 0xF000) == 0x9000)
		{
			// if VX != VY, skip instruction
			if (cpu[(0x0F00 & currentOpcode) >>> 8] != cpu[(0x00F0 & currentOpcode) >>> 4])
				programCounter += 2;
		}
		else if ((currentOpcode & 0xF000) == 0xA000)
			indexRegister = (short) (0xFFF & currentOpcode);
		else if ((currentOpcode & 0xF000) == 0xB000)
		{
			//jump to address
			//again, i don't think i need to jump
			//stack[stackPointer] = programCounter;
			//stackPointer++;
			programCounter = (short) ((0x0FFF & currentOpcode) + cpu[0]);
		}
		else if ((currentOpcode & 0xF000) == 0xC000)
			// set VX to random number & NN
			cpu[(0x0F00 & currentOpcode) >>> 8] = (byte) (((int) (Math.random()*256)) & (0xFF & currentOpcode));
		else if ((currentOpcode & 0xF000) == 0xD000)
		{
			//Draw sprite and set flag
			cpu[15] = 0;
			int height = currentOpcode & 0xF;
			int startX = cpu[(0x0F00 & currentOpcode) >>> 8];
			int startY = cpu[(0x00F0 & currentOpcode) >>> 4];
			boolean[] sprite = new boolean[8*height];
			for (int i = 0; i < sprite.length; ++i)
			{
				sprite[i] = ((memory[indexRegister + (i/8)] << (i%8)) & 0x80) == 0x80;
				if ((startY + (i/8)) * 64 + (startX + (i%8)) >= 64 * 32 || (startY + (i/8)) * 64 + (startX + (i%8)) < 0)
					continue;
				if (gfx[(startY + (i/8)) * 64 + (startX + (i%8))] && sprite[i])
					cpu[15] = 1;
				gfx[(startY + (i/8)) * 64 + (startX + (i%8))] ^= sprite[i];
			}
			screenUpdated = true;
		}
		else if ((currentOpcode & 0xF000) == 0xE000)
		{
			boolean isPressed = keyState[cpu[(0x0F00 & currentOpcode) >>> 8]];
			
			if ((currentOpcode & 0xFF) == 0x9E && isPressed)
				programCounter += 2;
			else if ((currentOpcode & 0xFF) == 0xA1 && !isPressed)
				programCounter += 2;
		}
		else if ((currentOpcode & 0xF000) == 0xF000)
		{
			if ((currentOpcode & 0xFF) == 0x07)
				cpu[(0x0F00 & currentOpcode) >>> 8] = delayTimer;
			else if ((currentOpcode & 0xFF) == 0x0A)
			{
				//gather keypress to VX
				keyPressed = -1;
				while (keyPressed < 0)
				{
					try
					{
						Thread.sleep(100);
					}
					catch (Exception e)
					{
						System.err.println("I caught an exception. Sorry I'm not well-documented!");
					}
				}
				cpu[(0x0F00 & currentOpcode) >>> 8] = (byte) keyPressed;
			}
			else if ((currentOpcode & 0xFF) == 0x15)
				delayTimer = cpu[(0x0F00 & currentOpcode) >>> 8];
			else if ((currentOpcode & 0xFF) == 0x18)
			{
				boolean wasZero = soundTimer == 0;
				soundTimer = cpu[(0x0F00 & currentOpcode) >>> 8];
				soundChanged = wasZero && soundTimer > 0;
			}
			else if ((currentOpcode & 0xFF) == 0x1E)
				indexRegister += cpu[(0x0F00 & currentOpcode) >>> 8];
			else if ((currentOpcode & 0xFF) == 0x29)
			{
				//set indexRegister to location of char in VX
				indexRegister = (short) (80 + 5 * cpu[(0x0F00 & currentOpcode) >>> 8]);
			}
			else if ((currentOpcode & 0xFF) == 0x33)
			{
				//convert VX to decimal representation,
				//and store hundreds at indexRegister, tens at
				//indexRegister + 1 and ones at indexRegister + 2
				int n = cpu[(0x0F00 & currentOpcode) >>> 8];
				if (n < 0)
					n += 256;
				for (int x = 0; x < 3; ++x)
				{
					memory[indexRegister + 2 - x] = (byte) (n%10);
					n /= 10;
				}
			}
			else if ((currentOpcode & 0xFF) == 0x55)
				for (int i = 0; i <= (0x0F00 & currentOpcode) >>> 8; ++i)
					memory[indexRegister + i] = cpu[i];
			else if ((currentOpcode & 0xFF) == 0x65)
				for (int i = 0; i <= (0x0F00 & currentOpcode) >>> 8; ++i)
					cpu[i] = memory[indexRegister + i];
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0)
	{
		if (arg0.getKeyChar() == '1')
		{
			keyState[1] = true;
			keyPressed = 1;
		}
		else if (arg0.getKeyChar() == '2')
		{
			keyState[2] = true;
			keyPressed = 2;
		}
		else if (arg0.getKeyChar() == '3')
		{
			keyState[3] = true;
			keyPressed = 3;
		}
		else if (arg0.getKeyChar() == '4')
		{
			keyState[12] = true;
			keyPressed = 12;
		}
		else if (arg0.getKeyChar() == 'q')
		{
			keyState[4] = true;
			keyPressed = 4;
		}
		else if (arg0.getKeyChar() == 'w')
		{
			keyState[5] = true;
			keyPressed = 5;
		}
		else if (arg0.getKeyChar() == 'e')
		{
			keyState[6] = true;
			keyPressed = 6;
		}
		else if (arg0.getKeyChar() == 'r')
		{
			keyState[13] = true;
			keyPressed = 13;
		}
		else if (arg0.getKeyChar() == 'a')
		{
			keyState[7] = true;
			keyPressed = 7;
		}
		else if (arg0.getKeyChar() == 's')
		{
			keyState[8] = true;
			keyPressed = 8;
		}
		else if (arg0.getKeyChar() == 'd')
		{
			keyState[9] = true;
			keyPressed = 9;
		}
		else if (arg0.getKeyChar() == 'f')
		{
			keyState[14] = true;
			keyPressed = 14;
		}
		else if (arg0.getKeyChar() == 'z')
		{
			keyState[10] = true;
			keyPressed = 10;
		}
		else if (arg0.getKeyChar() == 'x')
		{
			keyState[0] = true;
			keyPressed = 0;
		}
		else if (arg0.getKeyChar() == 'c')
		{
			keyState[11] = true;
			keyPressed = 11;
		}
		else if (arg0.getKeyChar() == 'v')
		{
			keyState[15] = true;
			keyPressed = 15;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0)
	{
		if (arg0.getKeyChar() == '1')
		{
			keyState[1] = false;
		}
		else if (arg0.getKeyChar() == '2')
		{
			keyState[2] = false;
		}
		else if (arg0.getKeyChar() == '3')
		{
			keyState[3] = false;
		}
		else if (arg0.getKeyChar() == '4')
		{
			keyState[12] = false;
		}
		else if (arg0.getKeyChar() == 'q')
		{
			keyState[4] = false;
		}
		else if (arg0.getKeyChar() == 'w')
		{
			keyState[5] = false;
		}
		else if (arg0.getKeyChar() == 'e')
		{
			keyState[6] = false;
		}
		else if (arg0.getKeyChar() == 'r')
		{
			keyState[13] = false;
		}
		else if (arg0.getKeyChar() == 'a')
		{
			keyState[7] = false;
		}
		else if (arg0.getKeyChar() == 's')
		{
			keyState[8] = false;
		}
		else if (arg0.getKeyChar() == 'd')
		{
			keyState[9] = false;
		}
		else if (arg0.getKeyChar() == 'f')
		{
			keyState[14] = false;
		}
		else if (arg0.getKeyChar() == 'z')
		{
			keyState[10] = false;
		}
		else if (arg0.getKeyChar() == 'x')
		{
			keyState[0] = false;
		}
		else if (arg0.getKeyChar() == 'c')
		{
			keyState[11] = false;
		}
		else if (arg0.getKeyChar() == 'v')
		{
			keyState[15] = false;
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0){}

}

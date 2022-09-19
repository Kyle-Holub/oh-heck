package util;

import java.awt.Font;

/**
 * Frame rate counter
 * 
 * @author Kyle Holub
 */
public class FrameRateCounter {
	   
	   private String frameRate;
	   private long lastTime;
	   private long delta;
	   private int frameCount;
	   private Font font;

	   public void initialize() {
	      lastTime = System.currentTimeMillis();
	      frameRate = "FPS 0";
	      font = new Font("Monospaced", Font.BOLD, 12);
	   }
	   
	   public void calculateFPS() {
	      long current = System.currentTimeMillis();
	      delta += current - lastTime;
	      lastTime = current;
	      frameCount++;
	      if( delta > 1000 ) {
	         delta -= 1000;
	         frameRate = String.format( "FPS %s", frameCount );
	         frameCount = 0;
	      }
	   }
	   
	   public String getFPS() {
	      return frameRate;
	   }
	   
	   public Font getFont() {
		   return font;
	   }
	}

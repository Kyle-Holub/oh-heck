 package core.player;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import game.gameState.MenuState;
import game.main.MainCanvas;
import util.Transparency;

/**
 * The user interface for players
 * 
 * @author Kyle Holub
 * @version 1.0
 * @since 1.0
 */
public class PlayerUI {
	
	// the total number of players
	private static final int NUM_PLAYERS = 3;
	public static final int TAG_W = 133;
	public static final int TAG_H = 18;
	private static final byte TAG_BASE_Y = 17;
	private static final Color TURN_BORDER = new Color(0, 244, 128);
	private static final Color TURN_FILL = new Color(0, 128, 77);
	private static final Color BORDER = Color.BLUE;
	
	private static BufferedImage trickIcon;
	private static BufferedImage betIcon;
	private static BufferedImage underBidIcon;
	private static BufferedImage dealerIcon;
	private static Font font;
	
	private String name;
	private int bet;
	private int tricks;
	
	private int tagPad;
	private int posX;
	private int posY;
	
	private Rectangle tag;
	
	private Player player;
	
	public PlayerUI(String name, int i, Player player) {
		
		i -= 1;
		this.name = name;
		
		this.player = player;
		try {
			trickIcon = ImageIO.read(getClass().getResourceAsStream("/images/trickIcon.png"));
			betIcon = ImageIO.read(getClass().getResourceAsStream("/images/trickIcon.png"));
			Image temp = Transparency.makeImageTranslucent(betIcon, .7f);
			betIcon = Transparency.toBufferedImage(temp);
			underBidIcon = ImageIO.read(getClass().getResourceAsStream("/images/underBidIcon.png"));
			dealerIcon = ImageIO.read(getClass().getResourceAsStream("/images/dealerIcon.png"));
			font = new Font("monospaced", Font.BOLD, 14);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		if (i == -1) {
			posX = MainCanvas.SCREEN_W / 2 - (TAG_W / 2);
			posY = HumanPlayer.TAG_Y;
		} else {
			tagPad = (MainCanvas.SCREEN_W - (TAG_W * NUM_PLAYERS + 1)) / (NUM_PLAYERS + 2);
			posX = (i * TAG_W) + (tagPad * (i + 1));
			posY = TAG_BASE_Y;
		}
		tag = new Rectangle(posX, posY, TAG_W, TAG_H);
	}
	
	public int getTagPad() {
		return tagPad;
	}
	
	public void setPos(int x, int y) {
		posX = x;
		posY = y;
	}
	
	public void drawTag(Graphics g) {
		// set font
		g.setFont(font);
		
		// draw background of ui
		if (player.isTurn()) {
			g.setColor(TURN_FILL);
		} else {
			g.setColor(MenuState.TEXT_COLOR);
		}
		g.fillRect(tag.x, tag.y, tag.width, tag.height);
		if (player.isTurn()) {
			g.setColor(TURN_BORDER);
		} else {
			g.setColor(BORDER);
		}
		g.drawRect(tag.x -1, tag.y - 1, tag.width + 1, tag.height + 1);
		
		// draw name
		FontMetrics fm = g.getFontMetrics();
		int nameWidth = (fm.stringWidth(name));
		int x = tag.x + (TAG_W / 2) - (nameWidth / 2); //tag.x - nameWidth - 3;
        int y = tag.y - 4;
        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString(name, x, y);
        if (player.isDealer()) {
        	g.drawImage(dealerIcon, tag.x - dealerIcon.getWidth() - 2, tag.y, null);
        }
        
        
        // draw tricks
        int offset = 0;
        int betCount = bet;
        int trickCount = tricks;
        for (int i = 0; i < getNumIconsToDraw(); i++, offset += trickIcon.getWidth() + 3) {
        	// if underbid
        	if (tricks > bet) {
        		// draw books correctly bid
        		if (betCount > 0) {
        			g.drawImage(trickIcon, tag.x + offset, tag.y, null);
        			betCount--;
        			trickCount--;
        		} else {
        			// then draw overbid books
        			g.drawImage(underBidIcon, tag.x + offset, tag.y, null);
        			trickCount--;
        		}
        	} else {
        		if (trickCount > 0) {
        			g.drawImage(trickIcon, tag.x + offset, tag.y, null);
        			trickCount--;
        			betCount--;
        		} else {
        			g.drawImage(betIcon, tag.x + offset, tag.y, null);
        			betCount--;
        		}
        	}
        }
        
        // draw score
        g.drawString(player.getScore() + "", tag.x + TAG_W + 3, tag.y + 14);
	}
	
	private int getNumIconsToDraw() {
		if (bet > tricks) {
			return bet;
		} else {
			return tricks;
		}
	}
	
	public void reset() {
		tricks = 0;
		bet = 0;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setBet(int bet) {
		this.bet = bet;
	}
	
	public void setTricks(int tricks) {
		this.tricks = tricks;
	}
	
	public int getPosX() {
		return posX;
	}
	
	public int getPosY() {
		return posY;
	}

	public int getWidth() {
		return TAG_W;
	}
}

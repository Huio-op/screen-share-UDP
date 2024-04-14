package org.example.old;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Screen extends JFrame {

  final static int X = 1090;
  final static int Y = 1920;
  final static int FRAMES_X = 10;
  final static int FRAMES_Y = 10;

  private final PositionedImage[] posImages = new PositionedImage[30000];
  private PositionedImage image;

  int count = 0;
  private JLabel label;

  public Screen() {
    setSize(1920, 1080);
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    g.drawImage(image.image(), image.x(), image.y(), this);

//    for (int i = 0; i < 30000; i++) {
//      PositionedImage posImage = posImages[i];
//
//      g.drawImage(posImage.image(), posImage.x(), posImage.y(), this);
//    }
  }

  public void addImage(BufferedImage image, int x, int y) {
//    posImages[count] = new PositionedImage(image, x, y);
//
//    count++;
//    if (count >= posImages.length) {
//      count = 0;
//    }

    this.image = new PositionedImage(image, x, y);

    repaint();
  }

  record PositionedImage(BufferedImage image, int x, int y) {}

}

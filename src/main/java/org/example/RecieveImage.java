package org.example;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import static org.example.SendImage.BYTE_OVERHEAD;

public class RecieveImage extends JFrame {

  JLabel imageLabel;


  public RecieveImage() {
    setSize(Toolkit.getDefaultToolkit().getScreenSize());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    imageLabel = new JLabel();
    getContentPane().add(imageLabel, BorderLayout.CENTER);
  }

  public void displayImage(byte[] imageData) throws IOException {
    if (imageData.length == 0) {
      System.out.println("Dados de imagem vazios");
      return;
    }

    BufferedImage image = toBufferedImage(imageData);
    if (image != null) {
      imageLabel.setIcon(new ImageIcon(image));
    } else {
      System.out.println("Erro ao criar BufferedImage");
    }
  }

  public static void main(String[] args) {
    RecieveImage receiver = new RecieveImage();
    receiver.setVisible(true);
    final List<byte[]> imageBlocks = new ArrayList<>();
    int fullLenght = 0;
    int currentImageId = -1;
    try {
      DatagramSocket socket = new DatagramSocket(SendImage.PORT);

      // Recebe os pacotes em loop
      while (true) {
        byte[] buffer = new byte[SendImage.MAX_DATAGRAM_SIZE]; // Tamanho máximo do datagrama
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        // Se não houver dados, saia do loop
        if (packet.getLength() == 0) {
          break;
        }

        // Cria um novo array de bytes com o tamanho do pacote recebido
        byte[] packetData = new byte[packet.getLength()];


        System.arraycopy(packet.getData(), 0, packetData, 0, packet.getLength());
        int currentFrameIdx= packetData[0];
        int maxFrameIdx = packetData[1];
        int imgId = packetData[2];

        // Exibe a imagem
        System.out.println(imgId + ": Recebido pacote de " + packet.getLength() + " bytes, idx atual: " + currentFrameIdx + ", max: " + maxFrameIdx);

        if (imgId == currentImageId || currentImageId == -1) {
          System.out.println("Entrou no if");
          byte[] imageData = new byte[packetData.length - BYTE_OVERHEAD];
          System.arraycopy(packetData, BYTE_OVERHEAD, imageData, 0, imageData.length);
          imageBlocks.add(imageData);
          fullLenght += imageData.length;


        }

        if (currentFrameIdx == maxFrameIdx) {

          System.out.println("Montando imagem...");

          byte[] fullImage = new byte[fullLenght];
          int lastPos = 0;
          for(byte[] imgBlock : imageBlocks) {
            System.arraycopy(imgBlock, 0, fullImage, lastPos, imgBlock.length);
            lastPos += imgBlock.length;
          }
          receiver.displayImage(fullImage);

          imageBlocks.clear();
          fullLenght = 0;
          currentImageId = -1;
        } else {
        currentImageId = imgId;
        }

      }

      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static BufferedImage toBufferedImage(byte[] bytes) {
    try {
      InputStream is = new ByteArrayInputStream(bytes);
      return ImageIO.read(is);
    } catch (Exception e) {
//      Ignored
    }
    return null;
  }

}
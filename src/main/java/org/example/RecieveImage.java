package org.example;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static org.example.SendImage.*;

public class RecieveImage extends JFrame {

  public JLabel imageLabel;
  public DatagramSocket socket = new DatagramSocket(PORT);
  public InetAddress currentAdress;


  public RecieveImage() throws SocketException {
    setSize(Toolkit.getDefaultToolkit().getScreenSize());
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    imageLabel = new JLabel();
    imageLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {

        if (currentAdress == null) {
          return;
        }

        final int x = me.getX();
        final int y = me.getY();

        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        final DataOutputStream dataOut = new DataOutputStream(byteOut);
        try {
          dataOut.writeInt(x);
          dataOut.writeInt(y);
          dataOut.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        byte[] mouseData = byteOut.toByteArray();

        DatagramPacket packet = new DatagramPacket(mouseData, mouseData.length, currentAdress, MOUSE_EVENT_PORT);
        try {
          socket.send(packet);
          System.out.println("Enviou evento do mouse para X=" + x + " Y=" + y);
        } catch (IOException e) {
          System.out.println("Não conseguiu enviar evento do mouse!");
        }
      }
    });
    getContentPane().add(imageLabel, BorderLayout.PAGE_START);
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

  public static void main(String[] args) throws SocketException {
    new Thread(() -> {
      try {
        recieveImageEvent();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

    new Thread(() -> {
      try {
        recieveAudioEvent();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

  }

  public static void recieveImageEvent() throws SocketException {
    RecieveImage receiver = new RecieveImage();
    receiver.setVisible(true);
    final List<byte[]> imageBlocks = new ArrayList<>();
    int fullLenght = 0;
    int currentImageId = -1;
    try {

      // Recebe os pacotes em loop
      while (true) {
        byte[] buffer = new byte[SendImage.MAX_DATAGRAM_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiver.socket.receive(packet);
        receiver.currentAdress = packet.getAddress();

        // Se não houver dados, saia do loop
        if (packet.getLength() == 0) {
          break;
        }

        // Cria um novo array de bytes com o tamanho do pacote recebido
        byte[] packetData = new byte[packet.getLength()];


        System.arraycopy(packet.getData(), 0, packetData, 0, packet.getLength());
        int currentFrameIdx = packetData[0];
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
          for (byte[] imgBlock : imageBlocks) {
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

      receiver.socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void recieveAudioEvent() throws LineUnavailableException {
    AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, true);
    TargetDataLine microphone;
    SourceDataLine speakers;
    microphone = AudioSystem.getTargetDataLine(format);

    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    microphone = (TargetDataLine) AudioSystem.getLine(info);
    microphone.open(format);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    microphone.start();

    DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
    speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
    speakers.open(format);
    speakers.start();


    try {

      DatagramSocket serverSocket = new DatagramSocket(AUDIO_PORT);

      while (true) {

        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(response);

        out.write(response.getData(), 0, response.getData().length);
        speakers.write(response.getData(), 0, response.getData().length);
        String quote = new String(buffer, 0, response.getLength());

        System.out.println(quote);
        System.out.println();

        //Thread.sleep(10000);
      }

    } catch (SocketTimeoutException ex) {
      System.out.println("Timeout error: " + ex.getMessage());
      ex.printStackTrace();
    } catch (IOException ex) {
      System.out.println("Client error: " + ex.getMessage());
      ex.printStackTrace();
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
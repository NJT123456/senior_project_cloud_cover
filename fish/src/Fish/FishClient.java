package Fish;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class FishClient extends JFrame implements Serializable {
	private static final long serialVersionUID = 1L;
	private ArrayList<Fish> fishs;
	private JPanel fishPanel;
	public Timer timer;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private int id;
	private Fish fishRemoved = null;

	public FishClient(String serverAddress, int serverPort) {

		setSize(500, 400);
		setVisible(true);

		fishs = new ArrayList<>();
		fishPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				ArrayList<Fish> fishsCopy = new ArrayList<>(fishs);
				for (Fish fish : fishsCopy) {
					fish.draw(g);
				}
			}
		};
		add(fishPanel);

		try {
			socket = new Socket(serverAddress, serverPort);
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			try {
				id = (Integer) in.readObject();
				setTitle("Bouncing fish (client id: " + id + ")");
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true) {
							Fish fish = (Fish) in.readObject();
							if (fish != null) {
								if (fish.deleted) {
									fishRemoved = fish;
								} else {
									boolean found = false;
									for (int i = 0; i < fishs.size(); i++) {
										if (fish.fishId.equals(fishs.get(i).fishId)) {
											found = true;
										}
									}
									if (!found) {
										Fish fishAdded;
										if (fish.x + fish.size > fishPanel.getWidth() && fish.speedX > 0) {
											fishAdded = new Fish(fish.fishId, fish.clientId, 0, fish.y);
										} else {
											fishAdded = new Fish(fish.fishId, fish.clientId, fishPanel.getWidth(),
													fish.y);
										}
										/*
										 * if (fish.x + fish.size > fishPanel.getWidth() && fish.speedX>0) { fishAdded =
										 * new Fish(fish.fishId,fish.clientId,0, fish.y); }else if(fish.x < 0 &&
										 * fish.speedX<0){ fishAdded = new
										 * Fish(fish.fishId,fish.clientId,fishPanel.getWidth(), fish.y); }else{
										 * fishAdded = new Fish(fish.fishId,fish.clientId,fish.x, fish.y); }
										 */
										fishAdded.speedX = fish.speedX;
										fishs.add(fishAdded);
									}
								}
							}
						}
					} catch (IOException | ClassNotFoundException ex) {
						ex.printStackTrace();
					}
				}
			}).start();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		fishPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					Fish fish = new Fish(id, e.getX(), e.getY());
					fishs.add(fish);
				} else if (e.getButton() == MouseEvent.BUTTON3 && !fishs.isEmpty()) {
					try {
						Fish fish = new Fish(id, e.getX(), e.getY());
						fish.deleted = true;
						out.writeObject(fish);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}

		});
		timer = new Timer(10, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Fish fishMoved = null;
				for (Fish fish : fishs) {
					fish.update();
					// if (fish.x + fish.size > fishPanel.getWidth() || fish.x < 0) {
					if (fish.x + fish.size > fishPanel.getWidth()) {
						try {
							out.writeObject(fish);
							fishMoved = fish;
							out.flush();
							out.reset();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
				if (fishMoved != null) {
					fishs.remove(fishMoved);
				}
				if (fishRemoved != null) {
					int index = -1;
					for (int i = 0; i < fishs.size(); i++) {
						if (fishRemoved.fishId.equals(fishs.get(i).fishId)) {
							fishRemoved = fishs.get(i);
							index = i;
						}
					}
					if (index != -1) {
						fishs.remove(index);
					}
					fishRemoved = null;
				}
				fishPanel.repaint();
			}
		});
		timer.start();

	}

	public static void main(String[] args) {
		FishClient client = new FishClient("localhost", 9000);
		client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

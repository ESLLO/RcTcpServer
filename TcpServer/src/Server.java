import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.pi4j.component.servo.ServoDriver;
import com.pi4j.component.servo.ServoProvider;
import com.pi4j.component.servo.impl.RPIServoBlasterProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

public class Server {
	private ServerSocket socket;
	private int lastL = 0, lastR = 0, lastB = 0, lastA = 0;
	private int lastSpeed = 0;
	private long lastSpeedModify = System.currentTimeMillis();
	private long lastAngleModify = System.currentTimeMillis();
	private ServoDriver sd = null;

	public Server() {

		Gpio.wiringPiSetup();
		ServoProvider sp;
/*		try {
			sp = new RPIServoBlasterProvider();
			 sd = sp.getServoDriver(sp.getDefinedServoPins().get(6));
			 sd.getServo
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			sd = null;
		}*/
		SoftPwm.softPwmCreate(1, 17, 200);		// SERVO
		SoftPwm.softPwmCreate(5, 0, 255);		// DC
		Gpio.pinMode(0, Gpio.OUTPUT);// l
		Gpio.pinMode(2, Gpio.OUTPUT);// r
		Gpio.pinMode(3, Gpio.OUTPUT);// b
		Gpio.pinMode(4, Gpio.OUTPUT);
		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (true) {
						int speed = lastSpeed;
						if (!(lastA == 1 && lastB == 1)) {
							if (lastA == 1 && speed < 200) {
								if (speed < 30)
									speed = 30;
								else
									speed += 5;
							}
							if (lastA == 0)
								speed -= 2;
							if (lastB == 1 && speed > 0)
								speed -= 20;
						}
						if (speed < 0)
							speed = 0;
						if (speed > 200)
							speed = 200;
						if (speed != lastSpeed) {
							lastSpeed = speed;
							System.out.println("speed changed to : " + speed);
							SoftPwm.softPwmWrite(5, speed);
						}
						Thread.sleep(100);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		th.setDaemon(true);
		th.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					socket = new ServerSocket(5126);
					while (true) {
						Socket sc = socket.accept();
						OutputStream out = sc.getOutputStream();
						InputStream in = sc.getInputStream();
						int readed = 0;
						while ((readed = in.read()) != -1) {
							// 5 ~ 10
							int angle = (int)((((double)readed/90d)*10)+12d);
							if (System.currentTimeMillis() - lastAngleModify > 30) {
								SoftPwm.softPwmWrite(1, angle);
								lastAngleModify = System.currentTimeMillis();
							}
							int bit8 = in.read();
							if (bit8 == -1)
								break;
							int nowR = bit8 & 1;
							bit8 = bit8 >> 1;
							int nowL = bit8 & 1;
							bit8 = bit8 >> 1;
							int nowB = bit8 & 11;
							bit8 = bit8 >> 2;
							lastA = bit8 & 11;
							if ((lastB & 1) != (nowB & 1))
								Gpio.digitalWrite(3, nowB == 0 ? Gpio.LOW : Gpio.HIGH);
							if (lastL != nowL)
								Gpio.digitalWrite(0, nowL == 0 ? Gpio.LOW : Gpio.HIGH);
							if (lastR != nowR)
								Gpio.digitalWrite(2, nowR == 0 ? Gpio.LOW : Gpio.HIGH);
							lastR = nowR;
							lastL = nowL;
							lastB = nowB;
							System.out.println("readed : \nangle:" + angle + "\nAcc:" + lastA + " Brk:" + lastB
									+ " Left" + lastL + " Right" + lastR);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].equals("take")) {
				new PiCam().save();
			}
		} else {
			new Server();
		}
	}
}

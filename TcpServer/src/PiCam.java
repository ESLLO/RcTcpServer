import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.hopding.jrpicam.RPiCamera;
import com.hopding.jrpicam.enums.AWB;
import com.hopding.jrpicam.enums.Exposure;

public class PiCam {
	private RPiCamera cam;

	public PiCam() {
		try {
			cam = new RPiCamera("/home/pi/RC");
			cam.setWidth(640).setHeight(360).setBrightness(75).setExposure(Exposure.AUTO).setTimeout(100)
					.setAddRawBayer(true).setAWB(AWB.AUTO);
			cam.setToDefaults();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BufferedImage take() {
		try {
			return cam.takeBufferedStill(640, 360);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void save() {
		try {
			BufferedImage img = take();
			File file = new File(System.currentTimeMillis() + ".png");
			if (img != null)
				ImageIO.write(img, "png", file);
			System.out.println(file.getAbsolutePath()+" saved.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

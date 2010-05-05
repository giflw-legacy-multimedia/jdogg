package nl.weeaboo.ogg.theora;


public class VideoFormat {

	private int width;
	private int height;
	
	private int aspectNum;
	private int aspectDenom;
	private double aspectRatio;
	
	private int fpsNum;
	private int fpsDenom;
	private double frameTime;
	
	public VideoFormat(int w, int h, int anum, int adenom, int fpsnum, int fpsdenom) {
		width = w;
		height = h;
		
		aspectNum = anum;
		aspectDenom = adenom;
		aspectRatio = aspectNum / (double)aspectDenom;
		
		fpsNum = fpsnum;
		fpsDenom = fpsdenom;
		if (fpsDenom != 0) {
			frameTime = 1.0 * fpsDenom / fpsNum;
		}
	}
	
	//Functions
	@Override
	public int hashCode() {
		return width ^ (height << 16) ^ fpsNum;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof VideoFormat) {
			return equals((VideoFormat)other);
		}
		return false;
	}
	
	protected boolean equals(VideoFormat f) {
		return width == f.width && height == f.height
			&& aspectNum == f.aspectNum && aspectDenom == f.aspectDenom
			&& fpsNum == f.aspectNum && fpsDenom == f.fpsDenom;
	}
	
	@Override
	public String toString() {
		return String.format("%s[size=%dx%d, aspect=%d:%d, fps=%d:%d]",
				getClass().getName(), width, height, aspectNum, aspectDenom,
				fpsNum, fpsDenom);
	}
	
	//Getters
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public double getAspectRatio() {
		return aspectRatio;
	}
	
	public int getFPSNumerator() {
		return fpsNum;
	}
	public int getFPSDenominator() {
		return fpsDenom;
	}
	
	public double getFrameDuration() {
		return frameTime;
	}
	
	//Setters
	
}

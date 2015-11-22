package art2;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * A collection of MovingThings, all following a common target.
 * 
 * @author Simon Ever-Hale
 *
 */
public class Flock {
	// Seed for the oscillation noise
	private float oscSeed;

	// X and Y positional seeds for the location of the focus
	private float focusSeedX;
	private float focusSeedY;

	// Rate which the focus moves by. Actually the change in the seed of the
	// noise function computing the position of the focus
	private float speed = .003f;

	// Spring length and constant ranges
	private float springLengthMin = 30;
	private float springLengthMax = 60;

	private float springConstantMin = .005f;
	private float springConstantMax = .02f;

	// Drawing values
	private int mode = MovingThing.ELLIPSE;
	private float drawSize = 10;

	// Movement values
	private float size = 3f; // used in weight calculations with forces and
								// stuff
	private float maxSpeed = 9;
	private float speedThreshold = 0;

	// Flag to determine whether the focus will follow the mouse or not
	private boolean followMouse = false;

	// The MovingThing that all Things in the Flock will follow
	private MovingThing focus;
	// List of all MovingThings and a counter to keep track of how many there
	// are
	private ArrayList<MovingThing> allThings;
	private int numThings;

	// Drawing window
	private PApplet p;

	// Dimensions of the drawing window
	private int width;
	private int height;

	// Default color ranges for a flock - blue to pink
	private int redMin = 0;
	private int redMax = 255;

	private int greenMin = 90;
	private int greenMax = 110;

	private int blueMin = 118;
	private int blueMax = 138;

	private int alphMin = 230;
	private int alphMax = 180;

	/**
	 * Create a new Flock with random seeds
	 * 
	 * @param p
	 *            The PApplet which this Flock will draw to
	 */
	public Flock(PApplet p) {
		this.p = p;
		width = p.width;
		height = p.height;
		focusSeedX = p.random(-1000, 1000);
		focusSeedY = p.random(-1000, 1000);
		this.speed = .003f;
		this.oscSeed = p.random(-1000, 1000);
		allThings = new ArrayList<MovingThing>();
		focus = new MovingThing(new PVector(width / 2, height / 2), new PVector(0, 0), new PVector(0, 0), size,
				drawSize, maxSpeed, p);
		numThings = 0;
	}

	/**
	 * Create a new Flock with the given seeds and focus speeds.
	 * 
	 * @param p
	 *            PApplet which this Flock will be drawn in
	 * @param fSeedX
	 * @param fSeedY
	 * @param speed
	 * @param oscSeed
	 */
	public Flock(PApplet p, float fSeedX, float fSeedY, float speed, float oscSeed) {
		this.p = p;
		width = p.width;
		height = p.height;
		focusSeedX = fSeedX;
		focusSeedY = fSeedY;
		this.speed = speed;
		this.oscSeed = oscSeed;
		allThings = new ArrayList<MovingThing>();
		focus = new MovingThing(new PVector(width / 2, height / 2), new PVector(0, 0), new PVector(0, 0), size,
				drawSize, maxSpeed, p);
		numThings = 0;
	}

	/**
	 * Create a new MovingThing with specified size at (x,y) and put a spring
	 * between it and the focus.
	 * 
	 * @param x
	 *            X-coordinate of the new MovingThing
	 * @param y
	 *            Y-coordinate of the new MovingThing
	 * @param connectToFocus
	 *            Whether or not the new object will be connected to the focus
	 * @return The new MovingThing created
	 */
	public MovingThing addConnection(float x, float y, boolean connectToFocus) {
		MovingThing m2 = new MovingThing(new PVector(x, y), new PVector(0, 0), new PVector(0, 0), size, drawSize,
				maxSpeed, p);
		if (connectToFocus) {
			m2.addConnection(focus);
		}
		float frac = p.random(0, 1);
		m2.setSpringLength(PApplet.sqrt(frac) * (springLengthMax - springLengthMin) + springLengthMin);
		m2.setSpringConstant(p.random(springConstantMin, springConstantMax));
		allThings.add(m2);
		numThings++;
		return m2;
	}

	/**
	 * Draw this Flock to the screen at its current location without updating
	 * anything
	 */
	public void draw() {
		for (int i = 0; i < numThings; i++) {
			MovingThing mi = allThings.get(i);
			drawThing(mi);
		}
	}

	/**
	 * Move the focus one step.
	 */
	public void moveFocus() {
		setWithinWindow(focus, 3 * width * p.noise(focusSeedX) - width, 3 * height * p.noise(focusSeedY) - height);
		focusSeedX += speed;
		focusSeedY += speed;
	}

	/**
	 * Set the location of the focus.
	 * 
	 * @param x
	 *            X-coordinate of destination
	 * @param y
	 *            Y-coordinate of destination
	 */
	public void moveFocus(int x, int y) {
		focus.setPos(x, y);
	}

	/**
	 * Calculate updated positions for the focus and all MovingThings in the
	 * Flock
	 */
	public void update() {
		if (followMouse) {
			focus.setPos(p.mouseX, p.mouseY);
		}
		for (int i = 0; i < numThings; i++) {
			MovingThing mi = allThings.get(i);
			mi.oscillate(2 * p.noise(oscSeed + i) - 1);
			mi.update();
			oscSeed += .01;
		}
	}

	/**
	 * Update positions and then draw to the screen.
	 */
	public void updateAndDraw() {
		if (followMouse) {
			focus.setPos(p.mouseX, p.mouseY);
		}
		for (int i = 0; i < numThings; i++) {
			MovingThing mi = allThings.get(i);
			mi.setDrawSize(drawSize);
			mi.oscillate(2 * p.noise(oscSeed + i) - 1);
			mi.update();
			drawThing(mi);
			oscSeed += .01;
		}
	}

	/**
	 * Draw the specified MovingThing
	 * 
	 * @param mi
	 *            MovingThing to be drawn
	 */
	public void drawThing(MovingThing mi) {
		float speed = mi.getVelocity().mag();
		if (speed >= speedThreshold) {
			float hue = speed / mi.getMaxSpeed();
			int red = (int) (hue * (redMax - redMin)) + redMin;
			int green = (int) (hue * (greenMax - greenMin)) + greenMin;
			int blue = (int) (hue * (blueMax - blueMin)) + blueMin;
			int alpha = (int) (hue * (alphMax - alphMin)) + alphMin;
			mi.draw(red, green, blue, alpha, mode);
		}
	}

	/**
	 * Apply magnetic repulsion to each MovingThing from the specified location
	 * vector.
	 * 
	 * @param thing
	 *            PVector representing location of magnetic force
	 */
	public void runAwayFrom(PVector thing) {
		for (int i = 0; i < numThings; i++) {
			MovingThing mi = allThings.get(i);
			PVector dist = mi.getPos().copy().sub(thing);
			float dMag = dist.mag() / 10;
			if (dMag < 2) {
				float magnitude = 1 / (dMag * dMag);
				dist.mult(magnitude / dMag);
				mi.applyForce(dist);
			}
		}
	}

	/**
	 * Apply magnetic repulsion to each MovingThing in this Flock proportional
	 * to the magnetic coefficient of the MovingThing thing.
	 * 
	 * @param thing
	 *            The MovingThing to run away from.
	 */
	public void runAwayFrom(MovingThing thing) {
		for (int i = 0; i < numThings; i++) {
			MovingThing mi = allThings.get(i);
			PVector dist = mi.getPos().copy().sub(thing.getPos());
			float dMag = dist.mag() / 10;
			float mForce = thing.getMagForce();
			if (dMag < mForce * 2) {
				float magnitude = 1 / (dMag * dMag);
				dist.mult(mForce * magnitude / dMag);
				mi.applyForce(dist);
			}
		}
	}

	/**
	 * Set the magnetic coefficient of every MovingThing in this Flock
	 * 
	 * @param m
	 *            Magnetic coefficient
	 */
	public void setMagForce(float m) {
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).setMagForce(m);
		}
	}

	/**
	 * Sanitize the coordinates (x,y) so that they are within the window using
	 * modulo arithmetic and then move the specified MovingThing to those
	 * coordinates.
	 * 
	 * @param f
	 *            MovingThing to move
	 * @param x
	 *            X-coordinate of target location
	 * @param y
	 *            Y-coordinate of target location
	 */
	public void setWithinWindow(MovingThing f, float x, float y) {
		if (x < 0) {
			x = -1 * x;
		}
		if (x > width) {
			x = width - (x % width);
		}
		if (y < 0) {
			y = -1 * y;
		}
		if (y > height) {
			y = height - (y % height);
		}
		f.setPos(x, y);
	}

	/**
	 * Change the draw mode of this Flock to a different mode.
	 */
	public void toggleMode() {
		if (mode == MovingThing.TAIL) {
			mode = 0;
		} else {
			mode++;
		}
	}

	/**
	 * Set the draw mode of this Flock to the specified mode
	 * 
	 * @param m
	 *            Mode to change to
	 */
	public void setMode(int m) {
		mode = m;
	}

	/**
	 * Get the lower bound of the spring constant range for this Flock
	 * 
	 * @return Minimum spring constant
	 */
	public float getSpringConstantMin() {
		return springConstantMin;
	}

	/**
	 * Get the upper bound of the spring constant range for this Flock
	 * 
	 * @return Maximum spring constant
	 */
	public float getSpringConstantMax() {
		return springConstantMax;
	}

	/**
	 * Increase the range of spring constants by decreasing the lower bound and
	 * maintaining the upper bound.
	 */
	public void increaseSpringConstRange() {
		springConstantMin /= 1.1;
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).setSpringConstant(p.random(springConstantMin, springConstantMax));
		}
	}

	/**
	 * Decrease the range of spring constants by increasing the lower bound and
	 * mainting the upper bound.
	 */
	public void decreaseSpringConstRange() {
		springConstantMin *= 1.1;
		if (springConstantMin > springConstantMax) {
			springConstantMin = springConstantMax;
		}
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).setSpringConstant(p.random(springConstantMin, springConstantMax));
		}
	}

	/**
	 * Set the lower bound for spring constants in this Flock.
	 * 
	 * @param s
	 *            Minimum spring constant
	 */
	public void setSpringConstantMin(float s) {
		springConstantMin = s;
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).setSpringConstant(p.random(springConstantMin, springConstantMax));
		}
	}

	/**
	 * Set the upper bound for spring constants in this Flock.
	 * 
	 * @param s
	 *            Maximum spring constant
	 */
	public void setSpringConstantMax(float s) {
		springConstantMax = s;
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).setSpringConstant(p.random(springConstantMin, springConstantMax));
		}
	}

	/**
	 * Set the lower bound for spring lengths in this Flock.
	 * 
	 * @param d
	 *            Minimum spring length
	 */
	public void setSpringLengthMin(float d) {
		springLengthMin = d;
	}

	/**
	 * Set the upper bound for spring lengths in this Flock.
	 * 
	 * @param d
	 *            Maximum spring length
	 */
	public void setSpringLengthMax(float d) {
		springLengthMax = d;
	}

	/**
	 * Return the average position of all MovingThings in this flock. If the
	 * flock is empty, return the position of the focus.
	 * 
	 * @return PVector representing average location of the Flock
	 */
	public PVector getAveragePos() {
		PVector avg = new PVector(0, 0);
		if (numThings >= 1) {
			for (int i = 0; i < numThings; i++) {
				avg.add(allThings.get(i).getPos());
			}
			avg.div(numThings);
		} else {
			avg.set(focus.getPos());
		}
		return avg;
	}

	/**
	 * Get the lower bound for all spring lengths in this Flock.
	 * 
	 * @return Minimum spring length
	 */
	public float getSpringLengthMin() {
		return springLengthMin;
	}

	/**
	 * Get the upper bound for all spring lengths in this Flock.
	 * 
	 * @return Maximum spring length
	 */
	public float getSpringLengthMax() {
		return springLengthMax;
	}

	/**
	 * Recalculate the lengths of all springs connecting MovingThings in this
	 * Flock to the focus. Spring lengths are distributed uniformly along the
	 * interval [springLengthMin, springLengthMax].
	 */
	public void reSpring() {
		for (int i = 0; i < numThings; i++) {
			float frac = p.random(0, 1);
			allThings.get(i)
					.setSpringLength(PApplet.sqrt(frac) * (springLengthMax - springLengthMin) + springLengthMin);
		}
	}

	/**
	 * Increase the range of spring lengths in this Flock and reassign spring
	 * lengths to each MovingThing.
	 */
	public void increaseSpringLengthRange() {
		springLengthMin -= 10;
		if (springLengthMin < 0) {
			springLengthMin = 0;
		}
		springLengthMax += 10;
		reSpring();
	}

	/**
	 * Decrease the range of spring lengths in this Flock and reassign spring
	 * lengths to each MovingThing.
	 */
	public void decreaseSpringLengthRange() {
		if (springLengthMax - springLengthMin < 20) {
			springLengthMin -= 10;
			springLengthMax -= 10;
			if (springLengthMin < 0) {
				springLengthMin = 0;
			}
			if (springLengthMax < 0) {
				springLengthMax = 0;
			}
		} else {
			springLengthMin += 10;
			springLengthMax -= 10;
		}
		reSpring();
	}

	/**
	 * Remove the last MovingThing added to this Flock.
	 */
	public void removeLastThing() {
		if (numThings > 0) {
			allThings.remove(numThings - 1);
			numThings--;
		}
	}

	/**
	 * Set the draw size of each MovingThing in this flock
	 * 
	 * @param s
	 *            Desired radius of each MovingThing
	 */
	public void setDrawSize(float s) {
		drawSize = s;
	}

	/**
	 * Get the draw size of each MovingThing in this flock
	 * 
	 * @return Radius in pixels
	 */
	public float getDrawSize() {
		return drawSize;
	}

	/**
	 * Toggle whether or not the focus of this Flock follows the mouse.
	 */
	public void toggleFollowMouse() {
		followMouse = !followMouse;
	}

	/**
	 * Set the speed which the focus moves at.
	 * 
	 * @param s
	 *            The amount by which the seed for the noise function
	 *            determining the position of the focus changes each update
	 */
	public void setFocusSpeed(float s) {
		speed = s;
	}

	/**
	 * Get the speed which the focus moves at.
	 * 
	 * @return The amount by which the seed for the noise function determining
	 *         the position of the focus changes each update
	 */
	public float getFocusSpeed() {
		return speed;
	}

	/**
	 * Set the focus to the specified MovingThing.
	 * 
	 * @param m
	 *            MovingThing to become the focus
	 */
	public void setFocus(MovingThing m) {
		// allThings.set(focus, m);
		focus = m;
		for (int i = 0; i < numThings; i++) {
			allThings.get(i).addConnection(focus);
		}
	}

	/**
	 * Get a MovingThing from the Flock.
	 * 
	 * @param i
	 *            Index of the MovingThing
	 * @return The MovingThing at the specified index
	 */
	public MovingThing getMovingThing(int i) {
		return allThings.get(i);
	}

	/**
	 * Set the range of colors which MovingThings in this Flock will be drawn
	 * at.
	 * 
	 * @param rMin
	 *            Red value when not moving
	 * @param rMax
	 *            Red value at max speed
	 * @param gMin
	 *            Green value when not moving
	 * @param gMax
	 *            Green value at max speed
	 * @param bMin
	 *            Blue value when not moving
	 * @param bMax
	 *            Blue value at max speed
	 * @param aMin
	 *            Alpha value when not moving
	 * @param aMax
	 *            Alpha value at max speed
	 */
	public void setColorRangeRGBA(int rMin, int rMax, int gMin, int gMax, int bMin, int bMax, int aMin, int aMax) {
		redMin = rMin;
		redMax = rMax;
		greenMin = gMin;
		greenMax = gMax;
		blueMin = bMin;
		blueMax = bMax;
		alphMin = aMin;
		alphMax = aMax;
	}

	/**
	 * Set the range of alpha values for each MovingThing in this Flock.
	 * 
	 * @param min
	 *            Alpha value when not moving
	 * @param max
	 *            Alpha value at max speed
	 */
	public void setAlphaRange(int min, int max) {
		alphMin = min;
		alphMax = max;
	}

	/**
	 * Return the minimum speed a MovingThing must be moving at to be drawn on
	 * the screen.
	 * 
	 * @return Minimum speed threshold
	 */
	public float getSpeedThreshold() {
		return speedThreshold;
	}

	/**
	 * Set the minimum speed a MovingThing must be moving at to be drawn on the
	 * screen.
	 * 
	 * @param s
	 *            Minimum speed threshold
	 */
	public void setSpeedThreshold(float s) {
		speedThreshold = s;
	}

	/**
	 * Get the size of this Flock.
	 * 
	 * @return The number of MovingThings in this Flock
	 */
	public int size() {
		return numThings;
	}
}

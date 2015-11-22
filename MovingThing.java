package art2;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * A moving object which can be acted upon by different forces including wind
 * resistance, springs, magnets or a Perlin-noise distributed perpendicular
 * force.
 * 
 * @author Simon Ever-Hale
 *
 */
public class MovingThing {
	/*
	 * Draw this MovingThing as a simple sphere.
	 */
	public static final int DOT = 0;

	/*
	 * Draw this MovingThing this an ellipse which points in the direction of
	 * movement and gets longer with increased speed
	 */
	public static final int ELLIPSE = 1;

	/*
	 * Draw this MovingThing as a set of lines pointing from the current
	 * position to a handful of previous positions.
	 */
	public static final int TAIL = 2;

	// Constants defining the physical and positional properties of the thing
	// itself
	private PVector pos;
	private PVector accel;
	private PVector velocity;
	private float mass;
	private float area;

	// Array of previous positions which will have size numSavedPos, used in
	// position averaging and tail drawing. Last element is most recent.
	private PVector[] prevPos;
	private int numSavedPos = 10;

	// Coefficient of magnetic force.
	private float magForce = 1;

	// Constants for air resistance
	public static final float drag = .05f;
	public static final float rho = 1f;

	// Spring values for calculating spring force from other MovingThings
	private float springConstant = .052f;
	private float springLength = 30;

	// Determines what size the thing will be drawn on the screen
	private float drawSize;

	// Used to make sure each new MovingThing gets a few frames to move before
	// drawing to avoid uneven opacity in TAIL mode
	private int updateCount;

	// Reference to the window being drawn in
	private PApplet p;

	// List of all things which this thing is connected to via spring
	private ArrayList<MovingThing> connections;
	private int nConnections;

	// Hard upper limit on the speed
	private float maxSpeed;

	// Toggle which determines whether or not to factor in air resistance.
	private boolean airResistance;

	/**
	 * Constructor to create a new MovingThing.
	 * 
	 * @param pos
	 *            The starting position of the Thing
	 * @param accel
	 *            The initial acceleration of the Thing
	 * @param velocity
	 *            The initial velocity of the Thing
	 * @param size
	 *            Size of the Thing
	 * @param drawSize
	 *            Radius in pixels of the Thing as it is drawn on the screen
	 * @param maxSpeed
	 *            Maximum speed of the Thing
	 * @param p
	 *            Reference to the PApplet which will be drawn on
	 */
	public MovingThing(PVector pos, PVector accel, PVector velocity, float size, float drawSize, float maxSpeed,
			PApplet p) {
		prevPos = new PVector[numSavedPos];
		updateCount = 0;
		this.drawSize = drawSize;
		for (int i = 0; i < numSavedPos; i++) {
			prevPos[i] = pos.copy();
		}
		connections = new ArrayList<MovingThing>();
		airResistance = true;
		nConnections = 0;
		this.pos = pos;
		this.accel = accel;
		this.velocity = velocity;
		this.maxSpeed = maxSpeed;
		// For this program, assume each Thing is a sphere
		area = size * size / 4 * (float) Math.PI;
		mass = area;
		this.p = p;
	}

	/**
	 * Creates a spring connecting this MovingThing and m.
	 * 
	 * @param m
	 */
	public void addConnection(MovingThing m) {
		connections.add(m);
		nConnections++;
	}

	/**
	 * Applies force f to the MovingThing.
	 * 
	 * @param f
	 */
	public void applyForce(PVector f) {
		accel.add(f.copy().div(mass));
	}

	/**
	 * Calculate air resistance for a given velocity vector v
	 * 
	 * @param v
	 *            Velocity of the MovingThing
	 * @return Force of the corresponding air resistance
	 */
	public PVector calcAirResistance(PVector v) {
		float vmag = v.mag();
		float fmag = -.5f * drag * rho * area * vmag * vmag;
		return v.copy().normalize().mult(fmag);
	}

	/**
	 * Calculate the air resistance and apply it.
	 * 
	 * @param v
	 *            Current Velocity of the MovingThing
	 */
	public void applyAirResistance(PVector v) {
		PVector resistance = calcAirResistance(velocity);
		applyForce(resistance);
	}

	/**
	 * Set whether or not to apply air resistance to the MovingThing
	 * 
	 * @param toggle
	 *            Determines state of air resistance
	 */
	public void setAirResistance(boolean toggle) {
		airResistance = toggle;
	}

	/**
	 * Calculate the new position of the MovingThing based on current velocity
	 * and acceleration. Smooths position by averaging over previous positions.
	 */
	public void update() {
		updateCount++;
		velocity.add(accel);
		if (velocity.mag() > maxSpeed) {
			velocity.normalize().mult(maxSpeed);
		}

		// Update saved positions
		for (int i = 0; i < numSavedPos - 1; i++) {
			prevPos[i].set(prevPos[i + 1]);
		}
		prevPos[numSavedPos - 1].set(pos);

		// Update position, reset acceleration
		pos.add(velocity);
		accel.set(0, 0);
		if (airResistance) {
			applyAirResistance(velocity);
		}
		for (int i = 0; i < nConnections; i++) {
			PVector tmp = pos.copy();
			tmp.sub(connections.get(i).pos);
			float distance = tmp.mag() - springLength;
			tmp.normalize().mult(distance * -springConstant);
			applyForce(tmp);
		}
	}

	/**
	 * Draw the MovingThing at its current position.
	 * 
	 * @param r
	 *            Red value
	 * @param g
	 *            Green value
	 * @param b
	 *            Blue value
	 * @param a
	 *            Alpha value
	 * @param mode
	 *            Determines what shape to draw
	 */
	public void draw(int r, int g, int b, int a, int mode) {
		// 5 frame buffer to allow the positional averaging to catch up
		if (updateCount < 5) {
			return;
		}
		if (mode == TAIL) {
			p.stroke(r, g, b, a / numSavedPos);
			p.strokeWeight(drawSize);
			for (int i = 0; i < numSavedPos; i++) {
				p.line(pos.x, pos.y, prevPos[i].x, prevPos[i].y);
			}
		} else {
			// Calculate two average positions, corresponding to a current
			// position and one previous position
			PVector avg1 = new PVector(0, 0);
			// If the number of points saved in prevPos is more than 10, average
			// 10 points. If it's less, average that amount minus 1.
			int aLength = (numSavedPos > 10 ? 10 : numSavedPos - 1);
			// If 10 points are saved, average points 0-8 for avg1 and 1-9 for
			// avg2.
			for (int i = 0; i < aLength; i++) {
				avg1.add(prevPos[i]);
			}
			avg1.div(aLength);
			PVector avg2 = new PVector(0, 0);
			for (int i = 1; i < aLength + 1; i++) {
				avg2.add(prevPos[i]);
			}
			avg2.div(aLength);

			if (mode == DOT) {
				p.strokeWeight(drawSize);
				p.stroke(r, g, b, a);
				p.point(avg1.x, avg1.y);
			}
			if (mode == ELLIPSE) {
				p.noStroke();
				p.strokeWeight(0);
				p.fill(r, g, b, a);

				p.pushMatrix();
				p.translate(avg1.x, avg1.y);
				p.rotate(PApplet.atan2(avg2.y - avg1.y, avg2.x - avg1.x));
				p.ellipse(0, 0, drawSize * avg1.dist(avg2), drawSize);
				p.popMatrix();
			}
		}
	}

	/**
	 * Set the length of the spring that pulls this thing to each of its connections.
	 * @param l The spring length
	 */
	public void setSpringLength(float l) {
		springLength = l;
	}

	/**
	 * 
	 * @return The length of the spring that pulls this thing to each of its connections.
	 */
	public float getSpringLength() {
		return springLength;
	}

	/**
	 * Apply a perpendicular force to this MovingThing proportional to the constant c.
	 * @param c
	 */
	public void oscillate(float c) {
		PVector perp = velocity.copy().set(-velocity.y, velocity.x);
		perp.mult(c);
		applyForce(perp);
	}

	/**
	 * 
	 * @return Current position vector 
	 */
	public PVector getPos() {
		return pos;
	}

	/**
	 * 
	 * @return Area of this MovingThing
	 */
	public float getArea() {
		return area;
	}

	/**
	 * 
	 * @return Current velocity
	 */
	public PVector getVelocity() {
		return velocity;
	}

	/**
	 * 
	 * @return Current acceleration
	 */
	public PVector getAcceleration() {
		return accel;
	}

	/**
	 * Get the maximum speed this MovingThing can move at
	 * @return Max speed
	 */
	public float getMaxSpeed() {
		return maxSpeed;
	}

	/**
	 * Remove the last connection created between this MovingThing and another.
	 */
	public void removeLastConnection() {
		if (nConnections > 0) {
			nConnections--;
			connections.remove(nConnections);
		}
	}

	/**
	 * 
	 * @return The number of connected MovingThings to this object.
	 */
	public int getNumConnections() {
		return nConnections;
	}

	/**
	 * Return the specified MovingThing connected to this object.
	 * @param i The index of the MovingThing you want to retrieve
	 * @return The MovingThing at index i.
	 */
	public MovingThing getConnected(int i) {
		return connections.get(i);
	}
	
	/**
	 * Remove the specified MovingThing from this object's connections.
	 * @param m MovingThing to remove
	 */
	public void removeConnection(MovingThing m) {
		connections.remove(m);
		nConnections--;
	}

	/**
	 * Set the position of this MovingThing
	 * @param x Horizontal coordinate
	 * @param y Vertical coordinate
	 */
	public void setPos(float x, float y) {
		pos.x = x;
		pos.y = y;
	}

	/**
	 * Set the spring constant of the spring that pulls this MovingThing to its connections.
	 * @param s Spring constant
	 */
	public void setSpringConstant(float s) {
		springConstant = s;
	}

	/**
	 * 
	 * @return The spring constant associated with this object.
	 */
	public float getSpringConstant() {
		return springConstant;
	}

	/**
	 * Set the radius with which this object will be drawn on the screen
	 * @param d Radius in pixels
	 */
	public void setDrawSize(float d) {
		drawSize = d;
	}

	/**
	 * Set the magnetic coefficient of the repulsive magnetic force this object emits.
	 * @param m Coefficient of magnetic force
	 */
	public void setMagForce(float m) {
		magForce = m;
	}

	/**
	 * 
	 * @return The coefficient of magnetic force associated with this object.
	 */
	public float getMagForce() {
		return magForce;
	}
}

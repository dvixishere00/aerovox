package com.aerovox.vehicle;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Force and torque integration: lift with stall, drag, thrust, rotors, control surfaces, fin, and sails.
 * Angular momentum lives in the body frame and orientation is a quaternion.
 * Units are blocks, ticks, and force units. The constants are tuning values, not real units.
 */
public final class FlightPhysics {
    public static final double GRAVITY = 0.04;

    private static final double LIFT_K = 0.5;
    private static final double DRAG_K = 0.05;
    private static final double SIDE_K = 0.2;
    private static final double CONTROL_K = 0.4;
    private static final double SAIL_K = 3.0;
    private static final double THRUST_PER_ENGINE = 1.0;
    private static final double ROTOR_THRUST = 0.8;
    private static final double STALL_ANGLE = 0.26; // about 15 degrees
    private static final double STALL_DRAG_MULT = 2.0;
    private static final double ANGULAR_DAMPING = 0.96;

    public static final class State {
        public final Quaternionf orientation = new Quaternionf();
        public Vec3 velocity = Vec3.ZERO;
        public Vec3 angularVelocity = Vec3.ZERO;
        public double airspeed;
        public double alpha;
        public boolean stalled;
    }

    private FlightPhysics() {}

    public static void step(FlightModel m, State s, Vec3 wind, double throttle,
                            double pitchCmd, double rollCmd, boolean rotorsActive) {
        Quaternionf q = s.orientation;
        Vec3 airLocal = toLocal(q, s.velocity.subtract(wind));
        double speed = airLocal.length();

        Vec3 fLocal = Vec3.ZERO;
        Vec3 tau = Vec3.ZERO;
        s.airspeed = speed;
        s.alpha = 0.0;
        s.stalled = false;

        if (speed > 1e-4) {
            Vec3 vh = airLocal.scale(1.0 / speed);
            double qd = 0.5 * speed * speed;
            double alpha = Math.atan2(-airLocal.y, airLocal.z);
            double beta = Math.atan2(airLocal.x, airLocal.z);
            s.alpha = alpha;
            s.stalled = Math.abs(alpha) > STALL_ANGLE;
            double cl = liftCoefficient(alpha);

            Vec3 liftDir = new Vec3(0.0, -vh.z, vh.y);
            double ln = liftDir.length();
            if (ln > 1e-6) {
                liftDir = liftDir.scale(1.0 / ln);

                if (m.liftArea > 0) {
                    Vec3 lf = liftDir.scale(LIFT_K * qd * m.liftArea * cl);
                    fLocal = fLocal.add(lf);
                    tau = tau.add(m.centerOfLift.cross(lf));
                }

                for (FlightModel.ControlSurface cs : m.controlSurfaces) {
                    double cmd = switch (cs.role()) {
                        case ELEVATOR -> -pitchCmd;
                        case AILERON -> rollCmd * Math.signum(cs.pos().x);
                        default -> 0.0;
                    };
                    if (cmd == 0.0) continue;
                    Vec3 f = liftDir.scale(CONTROL_K * qd * cmd);
                    fLocal = fLocal.add(f);
                    tau = tau.add(cs.pos().cross(f));
                }
            }

            double frontal = Math.abs(vh.x) * m.sizeY * m.sizeZ
                    + Math.abs(vh.y) * m.sizeX * m.sizeZ
                    + Math.abs(vh.z) * m.sizeX * m.sizeY;
            double dragMult = s.stalled ? STALL_DRAG_MULT : 1.0;
            fLocal = fLocal.add(vh.scale(-DRAG_K * qd * frontal * dragMult));

            if (m.finArea > 0) {
                Vec3 sf = new Vec3(-SIDE_K * qd * m.finArea * Math.sin(beta), 0.0, 0.0);
                fLocal = fLocal.add(sf);
                tau = tau.add(m.finPosition.cross(sf));
            }
        }

        // Sails are pushed by air moving relative to them, even when the aircraft is still.
        Vec3 flow = toLocal(q, wind.subtract(s.velocity));
        for (FlightModel.Sail sail : m.sails) {
            Vec3 n = sail.normal();
            double d = flow.dot(n);
            Vec3 sf = n.scale(SAIL_K * d * Math.abs(d));
            fLocal = fLocal.add(sf);
            tau = tau.add(sail.pos().cross(sf));
        }

        double thrust = THRUST_PER_ENGINE * throttle;
        for (Vec3 e : m.engines) {
            Vec3 tf = new Vec3(0.0, 0.0, thrust);
            fLocal = fLocal.add(tf);
            tau = tau.add(e.cross(tf));
        }

        // Motor rotators push only while motors are on. Reversed rotators push backward.
        if (rotorsActive) {
            for (FlightModel.Rotor rotor : m.rotors) {
                Vec3 tf = new Vec3(0.0, 0.0, ROTOR_THRUST * throttle * rotor.direction());
                fLocal = fLocal.add(tf);
                tau = tau.add(rotor.pos().cross(tf));
            }
        }

        Vec3 accWorld = toWorld(q, fLocal.scale(1.0 / m.mass));
        s.velocity = s.velocity.add(accWorld).add(0.0, -GRAVITY, 0.0);

        Vec3 angAcc = new Vec3(tau.x / m.inertia[0], tau.y / m.inertia[1], tau.z / m.inertia[2]);
        s.angularVelocity = s.angularVelocity.add(angAcc).scale(ANGULAR_DAMPING);
        Vec3 w = s.angularVelocity;
        q.mul(new Quaternionf((float) (w.x * 0.5), (float) (w.y * 0.5), (float) (w.z * 0.5), 1.0f)).normalize();
    }

    public static double airspeed(Quaternionf q, Vec3 velocity, Vec3 wind) {
        return toLocal(q, velocity.subtract(wind)).length();
    }

    /** {heading, pitch, roll} in degrees. Pitch is positive nose-up, roll positive right-wing-down. */
    public static double[] attitude(Quaternionf q) {
        Vector3f fwd = new Vector3f(0f, 0f, 1f);
        q.transform(fwd);
        Vector3f right = new Vector3f(-1f, 0f, 0f);
        q.transform(right);
        double heading = Math.toDegrees(Math.atan2(-fwd.x, fwd.z));
        double pitch = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, fwd.y))));
        double roll = Math.toDegrees(-Math.asin(Math.max(-1.0, Math.min(1.0, right.y))));
        return new double[]{heading, pitch, roll};
    }

    static double liftCoefficient(double alpha) {
        double a = Math.abs(alpha);
        if (a <= STALL_ANGLE) return 2.0 * Math.PI * alpha;
        double sign = Math.signum(alpha);
        double peak = 2.0 * Math.PI * STALL_ANGLE;
        return sign * peak * Math.exp(-(a - STALL_ANGLE) * 4.0);
    }

    static Vec3 toLocal(Quaternionf q, Vec3 v) {
        Quaternionf inv = new Quaternionf(q).conjugate();
        Vector3f r = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        inv.transform(r);
        return new Vec3(r.x, r.y, r.z);
    }

    static Vec3 toWorld(Quaternionf q, Vec3 v) {
        Vector3f r = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        q.transform(r);
        return new Vec3(r.x, r.y, r.z);
    }
}

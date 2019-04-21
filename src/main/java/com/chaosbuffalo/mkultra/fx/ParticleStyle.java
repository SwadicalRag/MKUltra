package com.chaosbuffalo.mkultra.fx;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

public class ParticleStyle {
    private int particleID;
    private int motionType;
    private int count;
    private int data;
    private float speed;
    private Vec3d radius;
    private Vec3d offset;

    public ParticleStyle(EnumParticleTypes particleID, int motionType, int count, int data, float speed, Vec3d radius, Vec3d offset) {
        this.particleID = particleID.getParticleID();
        this.motionType = motionType;
        this.count = count;
        this.data = data;
        this.speed = speed;
        this.radius = radius;
        this.offset = offset;
    }

    public int getParticleID() {
        return particleID;
    }

    public int getMotionType() {
        return motionType;
    }

    public int getCount() {
        return count;
    }

    public int getData() {
        return data;
    }

    public float getSpeed() {
        return speed;
    }

    public Vec3d getRadius() {
        return radius;
    }

    public Vec3d getOffset() {
        return offset;
    }

    public static class Builder {
        private EnumParticleTypes particleID;
        private int motionType;
        private int count;
        private int data;
        private float speed;
        private Vec3d radius;
        private Vec3d offset;

        public Builder(EnumParticleTypes particleID) {
            this.particleID = particleID;
        }

        public Builder motion(int motionType, int data) {
            this.motionType = motionType;
            this.data = data;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder speed(float speed) {
            this.speed = speed;
            return this;
        }

        public Builder radius(Vec3d radius) {
            this.radius = radius;
            return this;
        }

        public Builder offset(Vec3d offset) {
            this.offset = offset;
            return this;
        }

        public ParticleStyle build() {
            return new ParticleStyle(particleID, motionType, count, data, speed, radius, offset);
        }
    }
}

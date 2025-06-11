package com.remindme.components;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.time.LocalTime;

public class AnalogClock extends Group {

    private static final double CLOCK_RADIUS = 100.0;
    private static final double CENTER_X = CLOCK_RADIUS;
    private static final double CENTER_Y = CLOCK_RADIUS;

    // Clock components
    private Circle clockFace;
    private Circle centerDot;
    private Line hourHand;
    private Line minuteHand;
    private Line secondHand;
    private Rotate hourRotate;
    private Rotate minuteRotate;
    private Rotate secondRotate;

    public AnalogClock() {
        createClockFace();
        createHourMarkers();
        createNumbers();
        createHands();
        createCenterDot();
        updateTime();
    }

    private void createClockFace() {
        // Outer rim with gradient
        Circle outerRim = new Circle(CENTER_X, CENTER_Y, CLOCK_RADIUS);
        outerRim.setFill(Color.web("#4a4a4a"));
        outerRim.setStroke(Color.web("#888888"));
        outerRim.setStrokeWidth(4.0);

        // Middle rim for depth
        Circle middleRim = new Circle(CENTER_X, CENTER_Y, CLOCK_RADIUS - 8);
        middleRim.setFill(Color.web("#3a3a3a"));
        middleRim.setStroke(Color.web("#666666"));
        middleRim.setStrokeWidth(2.0);

        // Inner face
        clockFace = new Circle(CENTER_X, CENTER_Y, CLOCK_RADIUS - 12);
        clockFace.setFill(Color.web("#2b2b2b"));
        clockFace.setStroke(Color.web("#cccccc"));
        clockFace.setStrokeWidth(1.5);

        getChildren().addAll(outerRim, middleRim, clockFace);
    }

    private void createHourMarkers() {
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90); // -90 to start at 12 o'clock

            // Major hour markers
            double outerX = CENTER_X + (CLOCK_RADIUS - 15) * Math.cos(angle);
            double outerY = CENTER_Y + (CLOCK_RADIUS - 15) * Math.sin(angle);
            double innerX = CENTER_X + (CLOCK_RADIUS - 25) * Math.cos(angle);
            double innerY = CENTER_Y + (CLOCK_RADIUS - 25) * Math.sin(angle);

            Line hourMarker = new Line(outerX, outerY, innerX, innerY);
            hourMarker.setStroke(Color.WHITE);
            hourMarker.setStrokeWidth(3.0);

            getChildren().add(hourMarker);
        }

        // Minor minute markers
        for (int i = 0; i < 60; i++) {
            if (i % 5 != 0) { // Skip positions where hour markers are
                double angle = Math.toRadians(i * 6 - 90); // 6 degrees per minute

                double outerX = CENTER_X + (CLOCK_RADIUS - 10) * Math.cos(angle);
                double outerY = CENTER_Y + (CLOCK_RADIUS - 10) * Math.sin(angle);
                double innerX = CENTER_X + (CLOCK_RADIUS - 15) * Math.cos(angle);
                double innerY = CENTER_Y + (CLOCK_RADIUS - 15) * Math.sin(angle);

                Line minuteMarker = new Line(outerX, outerY, innerX, innerY);
                minuteMarker.setStroke(Color.web("#cccccc"));
                minuteMarker.setStrokeWidth(1.0);

                getChildren().add(minuteMarker);
            }
        }
    }

    private void createNumbers() {
        for (int hour = 1; hour <= 12; hour++) {
            double angle = Math.toRadians(hour * 30 - 90); // -90 to start at 12 o'clock
            double x = CENTER_X + (CLOCK_RADIUS - 35) * Math.cos(angle);
            double y = CENTER_Y + (CLOCK_RADIUS - 35) * Math.sin(angle);

            Text number = new Text(String.valueOf(hour));
            number.setFill(Color.WHITE);
            number.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            number.setTextAlignment(TextAlignment.CENTER);

            // Center the text
            number.setX(x - number.getBoundsInLocal().getWidth() / 2);
            number.setY(y + number.getBoundsInLocal().getHeight() / 4);

            getChildren().add(number);
        }
    }

    private void createHands() {
        // Hour hand - shorter and thicker
        hourHand = new Line(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y - 45);
        hourHand.setStroke(Color.WHITE);
        hourHand.setStrokeWidth(5.0);
        hourHand.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        hourHand.getStyleClass().add("hour-hand");

        hourRotate = new Rotate(0, CENTER_X, CENTER_Y);
        hourHand.getTransforms().add(hourRotate);

        // Minute hand - longer and medium thickness
        minuteHand = new Line(CENTER_X, CENTER_Y, CENTER_X, CENTER_Y - 70);
        minuteHand.setStroke(Color.WHITE);
        minuteHand.setStrokeWidth(3.5);
        minuteHand.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        minuteHand.getStyleClass().add("minute-hand");

        minuteRotate = new Rotate(0, CENTER_X, CENTER_Y);
        minuteHand.getTransforms().add(minuteRotate);

        // Second hand - longest and thinnest with tail
        secondHand = new Line(CENTER_X, CENTER_Y + 20, CENTER_X, CENTER_Y - 80);
        secondHand.setStroke(Color.web("#ff4444"));
        secondHand.setStrokeWidth(2.0);
        secondHand.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        secondHand.getStyleClass().add("second-hand");

        secondRotate = new Rotate(0, CENTER_X, CENTER_Y);
        secondHand.getTransforms().add(secondRotate);

        getChildren().addAll(hourHand, minuteHand, secondHand);
    }

    private void createCenterDot() {
        // Larger center hub with depth
        Circle centerHub = new Circle(CENTER_X, CENTER_Y, 10);
        centerHub.setFill(Color.web("#555555"));
        centerHub.setStroke(Color.web("#888888"));
        centerHub.setStrokeWidth(2.0);

        centerDot = new Circle(CENTER_X, CENTER_Y, 6);
        centerDot.setFill(Color.web("#ff4444"));
        centerDot.setStroke(Color.WHITE);
        centerDot.setStrokeWidth(2.0);

        getChildren().addAll(centerHub, centerDot);
    }

    public void updateTime() {
        LocalTime time = LocalTime.now();

        // Calculate angles (in degrees)
        double secondAngle = time.getSecond() * 6; // 360/60 = 6 degrees per second
        double minuteAngle = time.getMinute() * 6 + time.getSecond() * 0.1; // Include seconds for smooth movement
        double hourAngle = (time.getHour() % 12) * 30 + time.getMinute() * 0.5; // 360/12 = 30 degrees per hour

        // Apply rotations
        secondRotate.setAngle(secondAngle);
        minuteRotate.setAngle(minuteAngle);
        hourRotate.setAngle(hourAngle);
    }

    public double getClockRadius() {
        return CLOCK_RADIUS;
    }

    public double getClockDiameter() {
        return CLOCK_RADIUS * 2;
    }
}
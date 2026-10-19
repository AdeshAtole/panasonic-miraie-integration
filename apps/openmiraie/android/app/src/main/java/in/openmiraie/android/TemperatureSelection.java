package in.openmiraie.android;

/** Local interaction only. Ending a gesture consumes its value at most once. */
final class TemperatureSelection {
    final double min, max, step;
    private final double observed;
    private double raw, selected, angle;
    private boolean active;

    TemperatureSelection(double min, double max, double step, double observed) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step)
                || !Double.isFinite(observed) || max <= min || step <= 0 || step > max-min)
            throw new IllegalArgumentException("Invalid temperature range");
        this.min=min; this.step=step;
        this.max=min+Math.floor((max-min)/step+1e-8)*step;
        this.observed=observed; raw=selected=snap(observed);
    }
    double value() { return selected; }
    double fraction() { return (selected-min)/(max-min); }
    private double snap(double value) {
        return Math.max(min, Math.min(max, min+Math.round((value-min)/step)*step));
    }
    void begin(double degrees) { active=true; angle=degrees; raw=selected; }
    void drag(double degrees) {
        if (!active || !Double.isFinite(degrees)) return;
        double delta=(degrees-angle+540)%360-180;
        raw=Math.max(min,Math.min(max,raw+delta/270*(max-min)));
        selected=snap(raw); angle=degrees;
    }
    void adjust(double value) {
        if (!Double.isFinite(value)) return;
        active=true; raw=selected=snap(value);
    }
    Double finish() {
        if (!active) return null;
        active=false;
        return Math.abs(selected-observed)<1e-7 ? null : selected;
    }
    void cancel() { active=false; raw=selected=snap(observed); }
}

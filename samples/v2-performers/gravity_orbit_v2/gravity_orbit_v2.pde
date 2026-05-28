// gravity_orbit_v2
//
// Stand-alone Processing performer for processing-server v2 OSC mode.
//
// Required Processing libraries:
// - oscP5
//
// Listens for the current v2 walker-trigger contract:
// - /walker/signal/x   float [-1..1]
// - /walker/signal/y   float [-1..1]
// - /test/ping         string "ping", int count
// - /event/ping        int count
//
// Also listens for the general v2 orchestra-input-v1 contract:
// - /input/position    float x, float y
// - /input/trigger     string kind, int count
//
// Preferred for this sketch: orchestra-session-v1
// - /session/join      string sessionId, string name, string instrumentId
// - /session/leave     string sessionId
// - /session/position  string sessionId, float x, float y
// - /session/trigger   string sessionId, string kind, int count
// - /session/pitch     string sessionId, string note, int midi, float frequency, float level
//
// Run processing-server with:
//   .\run.ps1 -Properties "-Doutput.mode=osc -Dosc.debug.logging=true"

import oscP5.*;
import java.util.ArrayList;
import java.util.HashMap;

final int LISTEN_PORT = 12002;
final float PAIRWISE_ATTRACTION = 0.010;
final float PAIRWISE_SOFTENING = 0.0025;
final float MIN_PAIR_SEPARATION = 0.010;
final float MAX_PAIRWISE_OFFSET = 0.055;
final float MAX_SPEED = 0.026;
final int PITCH_DISPLAY_HOLD_MS = 1100;
final int PITCH_MIN_MIDI = 24;   // C1
final int PITCH_MAX_MIDI = 108;  // C8
final int PITCH_DEFAULT_MIN_MIDI = 48; // C3
final int PITCH_DEFAULT_MAX_MIDI = 72;  // C5
final int PITCH_DEFAULT_CENTER_MIDI = 60; // C4
final float PITCH_VERTICAL_SPAN = 0.17;
final int pitchRangeMinMidi = PITCH_DEFAULT_MIN_MIDI;
final int pitchRangeMaxMidi = PITCH_DEFAULT_MAX_MIDI;
final boolean DEBUG_LOGGING = false;
final int DEBUG_MESSAGE_LIMIT = 12;

OscP5 osc;

float walkerX = 0;
float walkerY = 0;
boolean haveWalkerX = false;
boolean haveWalkerY = false;

float anchorX = 0.5;
float anchorY = 0.5;
float pulse = 0;
int pingCount = 0;
int nextBodyIndex = 0;
int nextOrbiterInstanceId = 1;
int debugMessageCount = 0;
int debugFrameCount = 0;
boolean physicsFrozen = false;

final Object stateLock = new Object();
HashMap<String, Orbiter> bodiesBySession = new HashMap<String, Orbiter>();

void setup() {
  size(1000, 720);
  colorMode(HSB, 360, 100, 100, 100);
  smooth(4);
  frameRate(60);
  surface.setTitle("gravity_orbit_v2 | OSC port " + LISTEN_PORT);

  osc = new OscP5(this, LISTEN_PORT);
  println("gravity_orbit_v2 listening on OSC port " + LISTEN_PORT);
}

void draw() {
  background(0, 0, 4);

  synchronized (stateLock) {
    debugFrameCount = frameCount;
    updateSharedAnchor();
    pulse = lerp(pulse, 0, 0.08);

    boolean showGravityField = bodiesBySession.size() > 1;
    if (showGravityField) {
      drawField();
    }
    updateBodies();
    drawConnections();
    if (showGravityField) {
      drawAnchor();
    }
    drawHud();
    drawDebugHud();
  }
}

void updateBodies() {
  ArrayList<Orbiter> bodies = activeBodies();

  float[] baseX = new float[bodies.size()];
  float[] baseY = new float[bodies.size()];
  float[] offsetX = new float[bodies.size()];
  float[] offsetY = new float[bodies.size()];

  for (int i = 0; i < bodies.size(); i++) {
    Orbiter body = bodies.get(i);
    body.updatePitchState();
    baseX[i] = body.targetX;
    baseY[i] = body.effectiveTargetY();
    body.preX = baseX[i];
    body.preY = baseY[i];
  }

  if (!physicsFrozen) {
    for (int i = 0; i < bodies.size(); i++) {
      for (int j = i + 1; j < bodies.size(); j++) {
        float dx = baseX[j] - baseX[i];
        float dy = baseY[j] - baseY[i];
        float distSq = max(0.0004, dx * dx + dy * dy);
        float dist = sqrt(distSq);
        float nx = dx / dist;
        float ny = dy / dist;
        float minDist = bodies.get(i).radiusNormalized() + bodies.get(j).radiusNormalized() + MIN_PAIR_SEPARATION;

        if (dist < minDist) {
          float overlap = minDist - dist;
          float separation = overlap * 0.42;
          offsetX[i] -= nx * separation;
          offsetY[i] -= ny * separation;
          offsetX[j] += nx * separation;
          offsetY[j] += ny * separation;
        } else {
          float gravity = PAIRWISE_ATTRACTION / (distSq + PAIRWISE_SOFTENING);
          float pull = gravity * (0.85 + pulse * 0.3);
          offsetX[i] += nx * pull;
          offsetY[i] += ny * pull;
          offsetX[j] -= nx * pull;
          offsetY[j] -= ny * pull;
        }
      }
    }
  }

  for (int index = 0; index < bodies.size(); index++) {
    Orbiter body = bodies.get(index);
    float proposedX = baseX[index] + offsetX[index];
    float proposedY = baseY[index] + offsetY[index];
    float maxOffset = MAX_PAIRWISE_OFFSET;
    float dx = proposedX - baseX[index];
    float dy = proposedY - baseY[index];
    float offsetMagSq = dx * dx + dy * dy;
    if (offsetMagSq > maxOffset * maxOffset) {
      float scale = maxOffset / sqrt(offsetMagSq);
      proposedX = baseX[index] + dx * scale;
      proposedY = baseY[index] + dy * scale;
      dx *= scale;
      dy *= scale;
    }

    body.x = constrain(proposedX, body.radiusNormalized(), 1 - body.radiusNormalized());
    body.y = constrain(proposedY, body.radiusNormalized(), 1 - body.radiusNormalized());
    body.vx = body.x - baseX[index];
    body.vy = body.y - baseY[index];
    body.energy = lerp(body.energy, pulse, 0.08);
    body.postX = body.x;
    body.postY = body.y;
    body.lastUpdateFrame = frameCount;
    body.draw();
  }
}

ArrayList<Orbiter> activeBodies() {
  return new ArrayList<Orbiter>(bodiesBySession.values());
}

void updateSharedAnchor() {
  ArrayList<Orbiter> bodies = activeBodies();
  if (!bodies.isEmpty()) {
    float sx = 0;
    float sy = 0;
    for (Orbiter body : bodies) {
      sx += body.targetX;
      sy += body.effectiveTargetY();
    }
    anchorX = lerp(anchorX, sx / bodies.size(), 0.15);
    anchorY = lerp(anchorY, sy / bodies.size(), 0.15);
    return;
  }

  if (haveWalkerX && haveWalkerY) {
    float targetX = map(walkerX, -1, 1, 0.08, 0.92);
    float targetY = map(walkerY, -1, 1, 0.92, 0.08);
    anchorX = lerp(anchorX, targetX, 0.22);
    anchorY = lerp(anchorY, targetY, 0.22);
  }
}

void pushApart(Orbiter a, Orbiter b) {
  float dx = b.x - a.x;
  float dy = b.y - a.y;
  float distSq = dx * dx + dy * dy;
  if (distSq < 0.000001) {
    dx = random(-0.01, 0.01);
    dy = random(-0.01, 0.01);
    distSq = dx * dx + dy * dy;
  }

  float minDist = a.radiusNormalized() + b.radiusNormalized();
  float dist = sqrt(distSq);
  if (dist >= minDist) {
    return;
  }

  float nx = dx / dist;
  float ny = dy / dist;
  float overlap = (minDist - dist) * 0.45;
  a.x -= nx * overlap;
  a.y -= ny * overlap;
  b.x += nx * overlap;
  b.y += ny * overlap;
}

void limitVelocity(Orbiter body) {
  float speed = sqrt(body.vx * body.vx + body.vy * body.vy);
  if (speed > MAX_SPEED) {
    float scale = MAX_SPEED / speed;
    body.vx *= scale;
    body.vy *= scale;
  }
}

void bounce(Orbiter body) {
  float radius = body.radiusNormalized();
  if (body.x < radius) {
    body.x = radius;
    body.vx *= -0.45;
  } else if (body.x > 1 - radius) {
    body.x = 1 - radius;
    body.vx *= -0.45;
  }

  if (body.y < radius) {
    body.y = radius;
    body.vy *= -0.45;
  } else if (body.y > 1 - radius) {
    body.y = 1 - radius;
    body.vy *= -0.45;
  }
}

void drawField() {
  noFill();
  strokeWeight(1);
  for (int i = 0; i < 5; i++) {
    float r = (90 + i * 70) * (1 + pulse * 0.45);
    stroke(205, 55, 70, 24 - i * 3);
    ellipse(anchorX * width, anchorY * height, r, r);
  }
}

void drawConnections() {
  ArrayList<Orbiter> bodies = activeBodies();
  strokeWeight(1.1);
  for (int i = 0; i < bodies.size(); i++) {
    for (int j = i + 1; j < bodies.size(); j++) {
      Orbiter a = bodies.get(i);
      Orbiter b = bodies.get(j);
      float dx = b.x - a.x;
      float dy = b.y - a.y;
      float dist = sqrt(dx * dx + dy * dy);
      if (dist < 0.34) {
        float alpha = map(dist, 0.04, 0.34, 48, 4);
        stroke(190, 30, 80, alpha);
        line(a.x * width, a.y * height, b.x * width, b.y * height);
      }
    }
  }
}

void drawAnchor() {
  float x = anchorX * width;
  float y = anchorY * height;
  noStroke();
  fill(48, 90, 100, 14 + pulse * 35);
  ellipse(x, y, 90 + pulse * 80, 90 + pulse * 80);
  fill(48, 80, 100, 90);
  ellipse(x, y, 14 + pulse * 18, 14 + pulse * 18);
}

void drawHud() {
  fill(0, 0, 92, 72);
  textSize(13);
  textAlign(LEFT, TOP);
  text("gravity_orbit_v2  |  port " + LISTEN_PORT
    + "  |  collaborators=" + bodiesBySession.size()
    + "  |  pings=" + pingCount
    + "  |  field=" + (bodiesBySession.size() > 1 ? "on" : "hidden"),
    18, 16);
}

void drawDebugHud() {
  if (!DEBUG_LOGGING) {
    return;
  }

  fill(0, 0, 10, 70);
  noStroke();
  rect(16, 56, 420, min(220, 24 + bodiesBySession.size() * 18), 8);
  fill(0, 0, 96, 95);
  textAlign(LEFT, TOP);
  textSize(11);
  int y = 68;
  text("debug | sessions=" + bodiesBySession.size() + " | pings=" + pingCount
    + " | anchor=(" + nf(anchorX, 0, 2) + "," + nf(anchorY, 0, 2) + ")"
    + " | field=" + (bodiesBySession.size() > 1 ? "visible" : "hidden")
    + " | frame=" + debugFrameCount
    + " | physics=" + (physicsFrozen ? "frozen" : "live"),
    26, y);
  y += 18;
  int shown = 0;
  for (Orbiter body : bodiesBySession.values()) {
    if (shown >= 8) {
      break;
    }
    text(shortSessionId(body.sessionId)
      + " #"+ body.instanceId
      + " " + body.labelForDebug()
      + " " + body.debugState()
      + " tgtY=" + nf(body.effectiveTargetY(), 0, 3)
      + " pre=(" + nf(body.preX, 0, 3) + "," + nf(body.preY, 0, 3) + ")"
      + " post=(" + nf(body.postX, 0, 3) + "," + nf(body.postY, 0, 3) + ")"
      + " frame=" + body.lastUpdateFrame,
      26,
      y);
    y += 18;
    shown++;
  }
}

void oscEvent(OscMessage msg) {
  synchronized (stateLock) {
    if (msg.checkAddrPattern("/session/join") && msg.checkTypetag("sss")) {
      String sessionId = msg.get(0).stringValue();
      String name = msg.get(1).stringValue();
      String instrumentId = msg.get(2).stringValue();
      getOrCreateBody(sessionId, name, instrumentId);
      logDebug("join " + shortSessionId(sessionId) + " name=" + name + " instrument=" + instrumentId);
      return;
    }

    if (msg.checkAddrPattern("/session/leave") && msg.checkTypetag("s")) {
      String sessionId = msg.get(0).stringValue();
      bodiesBySession.remove(sessionId);
      logDebug("leave " + shortSessionId(sessionId));
      return;
    }

    if (msg.checkAddrPattern("/session/position") && msg.checkTypetag("sff")) {
      String sessionId = msg.get(0).stringValue();
      float x = constrain(msg.get(1).floatValue(), -1, 1);
      float y = constrain(msg.get(2).floatValue(), -1, 1);
      receiveSessionPosition(sessionId, x, y);
      logDebug("position " + shortSessionId(sessionId) + " osc=(" + nf(x, 0, 2) + "," + nf(y, 0, 2) + ")");
      return;
    }

    if (msg.checkAddrPattern("/session/trigger") && msg.checkTypetag("ssi")) {
      String sessionId = msg.get(0).stringValue();
      String kind = msg.get(1).stringValue();
      receiveSessionTrigger(sessionId, kind);
      logDebug("trigger " + shortSessionId(sessionId) + " kind=" + kind);
      return;
    }

    if (msg.checkAddrPattern("/session/pitch") && msg.checkTypetag("ssiff")) {
      String sessionId = msg.get(0).stringValue();
      String note = msg.get(1).stringValue();
      int midi = msg.get(2).intValue();
      float frequency = msg.get(3).floatValue();
      float level = msg.get(4).floatValue();
      receiveSessionPitch(sessionId, note, midi, frequency, level);
      logDebug("pitch " + shortSessionId(sessionId) + " " + note + " midi=" + midi + " hz=" + nf(frequency, 0, 1) + " level=" + nf(level, 0, 3));
      return;
    }

    if (msg.checkAddrPattern("/input/position") && msg.checkTypetag("ff")) {
      walkerX = constrain(msg.get(0).floatValue(), -1, 1);
      walkerY = constrain(msg.get(1).floatValue(), -1, 1);
      haveWalkerX = true;
      haveWalkerY = true;
      logDebug("input position aggregate=(" + nf(walkerX, 0, 2) + "," + nf(walkerY, 0, 2) + ")");
      return;
    }

    if (msg.checkAddrPattern("/input/trigger")) {
      receivePing();
      logDebug("input trigger");
      return;
    }

    if (msg.checkAddrPattern("/input/pitch") && msg.checkTypetag("siff")) {
      String note = msg.get(0).stringValue();
      int midi = msg.get(1).intValue();
      float frequency = msg.get(2).floatValue();
      float level = msg.get(3).floatValue();
      for (Orbiter body : activeBodies()) {
        applyPitch(body, note, midi, frequency, level);
      }
      logDebug("input pitch " + note + " midi=" + midi + " hz=" + nf(frequency, 0, 1) + " level=" + nf(level, 0, 3));
      return;
    }

    if (msg.checkAddrPattern("/walker/signal/x") && msg.checkTypetag("f")) {
      walkerX = constrain(msg.get(0).floatValue(), -1, 1);
      haveWalkerX = true;
      logDebug("walker x=" + nf(walkerX, 0, 2));
      return;
    }

    if (msg.checkAddrPattern("/walker/signal/y") && msg.checkTypetag("f")) {
      walkerY = constrain(msg.get(0).floatValue(), -1, 1);
      haveWalkerY = true;
      logDebug("walker y=" + nf(walkerY, 0, 2));
      return;
    }

    if (msg.checkAddrPattern("/test/ping") || msg.checkAddrPattern("/event/ping")) {
      receivePing();
      logDebug("legacy ping");
    }
  }
}

void receivePing() {
  pingCount++;
  pulse = 1.0;
  for (Orbiter body : activeBodies()) {
    float dx = body.x - anchorX;
    float dy = body.y - anchorY;
    float mag = max(0.001, sqrt(dx * dx + dy * dy));
    body.vx += (dx / mag) * random(0.004, 0.014);
    body.vy += (dy / mag) * random(0.004, 0.014);
    body.energy = 1.0;
    body.hueVelocity += random(-9, 9);
  }
}

void receiveSessionPosition(String sessionId, float oscX, float oscY) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  body.targetX = map(oscX, -1, 1, 0.08, 0.92);
  body.targetY = map(oscY, -1, 1, 0.92, 0.08);
  walkerX = oscX;
  walkerY = oscY;
  haveWalkerX = true;
  haveWalkerY = true;
}

void receiveSessionTrigger(String sessionId, String kind) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  pingCount++;
  pulse = 1.0;
  body.energy = 1.0;
  body.hueVelocity += random(-12, 12);

  float dx = body.x - body.targetX;
  float dy = body.y - body.targetY;
  float mag = max(0.001, sqrt(dx * dx + dy * dy));
  float strength = "shake".equals(kind) ? 0.02 : 0.012;
  body.vx += (dx / mag) * random(strength * 0.45, strength);
  body.vy += (dy / mag) * random(strength * 0.45, strength);
}

void receiveSessionPitch(String sessionId, String note, int midi, float frequency, float level) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  body.applyPitch(note, midi, frequency, level);
}

void applyPitch(Orbiter body, String note, int midi, float frequency, float level) {
  body.applyPitch(note, midi, frequency, level);
}

Orbiter getOrCreateBody(String sessionId, String name, String instrumentId) {
  if (sessionId == null || sessionId.length() == 0) {
    sessionId = "unknown";
  }
  Orbiter body = bodiesBySession.get(sessionId);
  if (body != null) {
    if (name != null && name.trim().length() > 0) {
      body.label = name.trim();
    }
    if (instrumentId != null && instrumentId.trim().length() > 0) {
      body.instrumentId = instrumentId.trim();
    }
    return body;
  }

  int index = nextBodyIndex++;
  float angle = TWO_PI * index / max(1, nextBodyIndex);
  float radius = 0.16 + 0.035 * (index % 4);
  String label = (name == null || name.trim().length() == 0) ? shortSessionId(sessionId) : name.trim();
  body = new Orbiter(
    nextOrbiterInstanceId++,
    sessionId,
    label,
    instrumentId == null ? "" : instrumentId,
    0.5 + cos(angle) * radius,
    0.5 + sin(angle) * radius,
    28 + index * 47 % 360,
    index
  );
  bodiesBySession.put(sessionId, body);
  logDebug("created body #" + body.instanceId + " " + shortSessionId(sessionId)
    + " label=" + body.label + " instrument=" + body.instrumentId
    + " pos=(" + nf(body.x, 0, 2) + "," + nf(body.y, 0, 2) + ")"
    + " target=(" + nf(body.targetX, 0, 2) + "," + nf(body.targetY, 0, 2) + ")");
  return body;
}

String shortSessionId(String sessionId) {
  return sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8);
}

void mousePressed() {
  synchronized (stateLock) {
    walkerX = map(mouseX, 0, width, -1, 1);
    walkerY = map(mouseY, height, 0, -1, 1);
    haveWalkerX = true;
    haveWalkerY = true;
    receiveSessionPosition("local", walkerX, walkerY);
    receiveSessionTrigger("local", "mouse");
  }
}

void keyPressed() {
  if (key == 'f' || key == 'F') {
    synchronized (stateLock) {
      physicsFrozen = !physicsFrozen;
      logDebug("physics " + (physicsFrozen ? "frozen" : "live"));
    }
  }
}

void logDebug(String message) {
  if (!DEBUG_LOGGING) {
    return;
  }
  debugMessageCount++;
  if (debugMessageCount <= DEBUG_MESSAGE_LIMIT) {
    println("[gravity-orbit] " + message);
  } else if (debugMessageCount == DEBUG_MESSAGE_LIMIT + 1) {
    println("[gravity-orbit] debug message limit reached; suppressing further logs");
  }
}

class Orbiter {
  int instanceId;
  String sessionId;
  String label;
  String instrumentId;
  String lastNote = "";
  float lastFrequency = 0;
  float lastPitchLevel = 0;
  int lastPitchMidi = 0;
  int lastPitchMillis = 0;
  float pitchOffsetY = 0;
  float x;
  float y;
  float targetX;
  float targetY;
  float preX;
  float preY;
  float postX;
  float postY;
  float vx;
  float vy;
  float hueBase;
  float hueCurrent;
  float hueVelocity;
  float energy;
  float phaseOffset;
  float orbitDirection;
  int lastUpdateFrame;

  Orbiter(int instanceId, String sessionId, String label, String instrumentId, float x, float y, float hueBase, int index) {
    this.instanceId = instanceId;
    this.sessionId = sessionId;
    this.label = label;
    this.instrumentId = instrumentId;
    this.x = x;
    this.y = y;
    this.targetX = x;
    this.targetY = y;
    this.hueBase = hueBase;
    this.hueCurrent = hueBase;
    this.phaseOffset = index % 5;
    this.orbitDirection = (index % 2 == 0) ? 1 : -1;
  }

  float radiusPixels() {
    return 28 + phaseOffset * 6 + energy * 26;
  }

  float radiusNormalized() {
    return (radiusPixels() * 0.5 + 5) / min(width, height);
  }

  float pitchOffsetForMidi(int midi) {
    int clampedMidi = constrain(midi, pitchRangeMinMidi, pitchRangeMaxMidi);
    return map(clampedMidi, pitchRangeMinMidi, pitchRangeMaxMidi, PITCH_VERTICAL_SPAN, -PITCH_VERTICAL_SPAN);
  }

  float effectiveTargetY() {
    return constrain(targetY + pitchOffsetY, radiusNormalized(), 1 - radiusNormalized());
  }

  boolean isPitchFresh() {
    return lastPitchMillis > 0 && millis() - lastPitchMillis <= PITCH_DISPLAY_HOLD_MS;
  }

  String labelForDebug() {
    return label;
  }

  String debugState() {
    return "inst=" + instanceId
      + " x=" + nf(x, 0, 3)
      + " y=" + nf(y, 0, 3)
      + " tx=" + nf(targetX, 0, 3)
      + " ty=" + nf(targetY, 0, 3)
      + " pY=" + nf(pitchOffsetY, 0, 3)
      + " note=" + debugNote();
  }

  String debugNote() {
    return isPitchFresh() ? lastNote : "-";
  }

  String pitchDisplayLabel() {
    if (isPitchFresh()) {
      return lastNote.length() == 0 ? label : lastNote;
    }
    return label;
  }

  void applyPitch(String note, int midi, float frequency, float level) {
    lastNote = note == null ? "" : note.trim();
    lastFrequency = frequency;
    lastPitchLevel = level;
    lastPitchMidi = midi;
    lastPitchMillis = millis();
    pitchOffsetY = pitchOffsetForMidi(midi);
    hueBase = (midi % 12) * 30;
    energy = max(energy, constrain(level * 8, 0.1, 1.0));
  }

  void updatePitchState() {
    if (!isPitchFresh()) {
      pitchOffsetY = lerp(pitchOffsetY, 0, 0.08);
      if (abs(pitchOffsetY) < 0.0005) {
        pitchOffsetY = 0;
      }
    }
  }

  void draw() {
    hueVelocity = lerp(hueVelocity, 0, 0.07);
    hueCurrent = (hueBase + hueVelocity + 360) % 360;
    float speed = constrain(sqrt(vx * vx + vy * vy) / MAX_SPEED, 0, 1);
    float r = radiusPixels();
    float px = x * width;
    float py = y * height;
    float targetDisplayY = effectiveTargetY() * height;

    stroke(hueCurrent, 45, 95, 70);
    strokeWeight(2);
    fill(hueCurrent, 72 + speed * 20, 92 + energy * 8, 88);
    ellipse(px, py, r * 2.0, r * 2.0);

    stroke(hueCurrent, 45, 95, 44);
    strokeWeight(1);
    line(px, py, targetX * width, targetDisplayY);
    fill(0, 0, 95, 72);
    textAlign(CENTER, CENTER);
    textSize(12);
    String labelToDraw = pitchDisplayLabel();
    text(labelToDraw, px, py);
  }
}

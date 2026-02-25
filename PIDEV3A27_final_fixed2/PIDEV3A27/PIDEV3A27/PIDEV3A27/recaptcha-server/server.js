import express from "express";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

// Put your Google reCAPTCHA secret key in an environment variable.
// PowerShell:  $env:RECAPTCHA_SECRET="..."
// CMD:         set RECAPTCHA_SECRET=...
const RECAPTCHA_SECRET = process.env.RECAPTCHA_SECRET;

if (!RECAPTCHA_SECRET) {
  console.warn("[WARN] RECAPTCHA_SECRET is not set. The server will reject verifications.");
}

// Simple in-memory session store: state -> { verified: boolean, createdAt: number }
const sessions = new Map();

function newState() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

// Create a new CAPTCHA session and return a unique URL containing the state.
app.get("/captcha/start", (req, res) => {
  const state = newState();
  sessions.set(state, { verified: false, createdAt: Date.now() });
  res.json({ state, url: `http://localhost:8085/captcha.html?state=${state}` });
});

// Polling endpoint used by JavaFX.
app.get("/captcha/status", (req, res) => {
  const { state } = req.query;
  const s = sessions.get(state);
  if (!s) return res.status(404).json({ verified: false });
  res.json({ verified: !!s.verified });
});

// Verify token with Google and mark the session as verified.
app.post("/captcha/verify", async (req, res) => {
  const { token, state } = req.body || {};
  if (!token || !state) return res.status(400).json({ valid: false, reason: "missing token/state" });
  if (!RECAPTCHA_SECRET) return res.status(500).json({ valid: false, reason: "RECAPTCHA_SECRET not set" });

  try {
    const form = new URLSearchParams();
    form.append("secret", RECAPTCHA_SECRET);
    form.append("response", token);

    const r = await fetch("https://www.google.com/recaptcha/api/siteverify", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: form.toString(),
    });

    const data = await r.json();
    const ok = !!data.success;

    const sess = sessions.get(state);
    if (ok && sess) sess.verified = true;

    res.json({ valid: ok, raw: data });
  } catch (e) {
    res.status(500).json({ valid: false, reason: String(e) });
  }
});

// Optional cleanup (remove sessions older than 10 minutes)
setInterval(() => {
  const now = Date.now();
  for (const [state, s] of sessions.entries()) {
    if (now - s.createdAt > 10 * 60 * 1000) sessions.delete(state);
  }
}, 60 * 1000);

const PORT = 8085;
app.listen(PORT, () => {
  console.log(`reCAPTCHA server running on http://localhost:${PORT}`);
  console.log(`Open http://localhost:${PORT}/captcha.html (for manual test)`);
});

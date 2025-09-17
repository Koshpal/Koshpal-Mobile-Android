import express from "express";
import mongoose from "mongoose";

const app = express();
app.use(express.json());

// MongoDB connection
mongoose.connect("mongodb+srv://tushars7740:sexysankalp@cluster0.oyptvfw.mongodb.net/koshpal")
  .then(() => console.log("✅ MongoDB connected"))
  .catch((err) => console.error("❌ MongoDB connection error:", err));

// Schema + Model
const userSchema = new mongoose.Schema({
  mobile: { type: String, required: true, unique: true },
  createdAt: { type: Date, default: Date.now }
});
const User = mongoose.model("User", userSchema);

// API: Create user after OTP verified in app
app.post("/create-user", async (req, res) => {
  try {
    const { mobile } = req.body;

    if (!mobile) {
      return res.status(400).json({ message: "Mobile number required" });
    }

    // Check if user already exists
    let existingUser = await User.findOne({ mobile });
    if (existingUser) {
      return res.status(200).json({ message: "User already exists", user: existingUser });
    }

    // Create new user
    const newUser = new User({ mobile });
    await newUser.save();

    res.status(201).json({ message: "User created successfully", user: newUser });
  } catch (err) {
    res.status(500).json({ message: "Server error", error: err.message });
  }
});

// Start server
app.listen(5000, () => console.log("🚀 Server running on http://localhost:5000"));

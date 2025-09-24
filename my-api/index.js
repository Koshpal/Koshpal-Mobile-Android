import express from "express";
import mongoose from "mongoose";

const app = express();
app.use(express.json());

// MongoDB connection
mongoose.connect("mongodb+srv://tushars7740:sexysankalp@cluster0.oyptvfw.mongodb.net/koshpal")
  .then(() => console.log("✅ MongoDB connected"))
  .catch((err) => console.error("❌ MongoDB connection error:", err));

// User Schema for mobile users
const userSchema = new mongoose.Schema({
  mobile: { type: String, required: true, unique: true },
  createdAt: { type: Date, default: Date.now }
});
const User = mongoose.model("User", userSchema);

// EmailUser Schema for email users (matching your existing collection)
const emailUserSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  createdAt: { type: Date, default: Date.now }
}, { collection: 'emailusers' });

const EmailUser = mongoose.model("EmailUser", emailUserSchema);

// Onboarding Schema for storing user responses
const onboardingSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  ageGroup: { type: String, required: true },
  monthlySalary: { type: String, required: true },
  financialSituation: { type: String, required: true },
  expenseTracking: { type: String, required: true },
  moneyWorryLevel: { type: Number, required: true, min: 1, max: 5 },
  highestExpenseCategory: { type: String, required: true },
  primaryFinancialGoal: { type: String, required: true },
  goalTimeframe: { type: String, required: true },
  financialStressArea: { type: String, required: true },
  completedAt: { type: Date, default: Date.now }
});
const OnboardingData = mongoose.model("OnboardingData", onboardingSchema);

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

// API: Create employee user with email
app.post("/create-email-user", async (req, res) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({ message: "Email address required" });
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: "Invalid email format" });
    }

    // Check if email user already exists
    let existingEmailUser = await EmailUser.findOne({ email });
    if (existingEmailUser) {
      return res.status(200).json({ 
        message: "Employee already exists", 
        user: existingEmailUser,
        token: "dummy-jwt-token"
      });
    }

    // Create new email user
    const newEmailUser = new EmailUser({ email });
    await newEmailUser.save();

    res.status(201).json({ 
      message: "Employee created successfully", 
      user: newEmailUser,
      token: "dummy-jwt-token"
    });
  } catch (err) {
    res.status(500).json({ message: "Server error", error: err.message });
  }
});

// API: Submit onboarding data
app.post("/submit-onboarding", async (req, res) => {
  try {
    const {
      email,
      ageGroup,
      monthlySalary,
      financialSituation,
      expenseTracking,
      moneyWorryLevel,
      highestExpenseCategory,
      primaryFinancialGoal,
      goalTimeframe,
      financialStressArea
    } = req.body;

    console.log("📧 Received onboarding request for email:", email);
    console.log("📋 Full request body:", JSON.stringify(req.body, null, 2));

    // Validate required fields
    if (!email || !ageGroup || !monthlySalary || !financialSituation || 
        !expenseTracking || !moneyWorryLevel || !highestExpenseCategory || 
        !primaryFinancialGoal || !goalTimeframe || !financialStressArea) {
      console.log("❌ Missing required fields");
      return res.status(400).json({ message: "All onboarding fields are required" });
    }

    // Debug: List all collections
    const collections = await mongoose.connection.db.listCollections().toArray();
    console.log("📂 Available collections:", collections.map(c => c.name));

    // Debug: Count documents in emailusers collection
    const emailUserCount = await EmailUser.countDocuments();
    console.log("📊 Total documents in emailusers collection:", emailUserCount);

    // Debug: Find all email users
    const allEmailUsers = await EmailUser.find({}, { email: 1 });
    console.log("📧 All emails in collection:", allEmailUsers.map(u => u.email));

    // Validate email exists in emailusers collection
    console.log("🔍 Looking for email user:", email);
    const emailUser = await EmailUser.findOne({ email: email });
    console.log("📋 Email user found:", emailUser ? "YES" : "NO");
    console.log("👤 Email user details:", emailUser);
    
    if (!emailUser) {
      console.log("❌ Email user not found in database");
      return res.status(404).json({ message: "Employee not found. Please login first." });
    }

    console.log("✅ Email user verified, proceeding with onboarding...");

    // Check if onboarding data already exists for this email
    let existingOnboarding = await OnboardingData.findOne({ email });
    
    if (existingOnboarding) {
      console.log("🔄 Updating existing onboarding data");
      // Update existing onboarding data
      existingOnboarding.ageGroup = ageGroup;
      existingOnboarding.monthlySalary = monthlySalary;
      existingOnboarding.financialSituation = financialSituation;
      existingOnboarding.expenseTracking = expenseTracking;
      existingOnboarding.moneyWorryLevel = moneyWorryLevel;
      existingOnboarding.highestExpenseCategory = highestExpenseCategory;
      existingOnboarding.primaryFinancialGoal = primaryFinancialGoal;
      existingOnboarding.goalTimeframe = goalTimeframe;
      existingOnboarding.financialStressArea = financialStressArea;
      existingOnboarding.completedAt = new Date();
      
      await existingOnboarding.save();
      
      return res.status(200).json({
        message: "Onboarding data updated successfully",
        success: true,
        onboardingId: existingOnboarding._id
      });
    } else {
      console.log("➕ Creating new onboarding data");
      // Create new onboarding data
      const newOnboarding = new OnboardingData({
        email,
        ageGroup,
        monthlySalary,
        financialSituation,
        expenseTracking,
        moneyWorryLevel,
        highestExpenseCategory,
        primaryFinancialGoal,
        goalTimeframe,
        financialStressArea
      });
      
      await newOnboarding.save();
      console.log("✅ Onboarding data saved successfully");
      
      return res.status(201).json({
        message: "Onboarding completed successfully! Your personalized financial journey begins now.",
        success: true,
        onboardingId: newOnboarding._id
      });
    }
  } catch (err) {
    console.error("❌ Onboarding submission error:", err);
    res.status(500).json({ message: "Server error", error: err.message });
  }
});

// Start server
app.listen(5000, () => console.log("🚀 Server running on http://localhost:5000"));

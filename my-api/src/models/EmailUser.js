// models/EmailUser.js
import mongoose from "mongoose";

const emailUserSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true },
  createdAt: { type: Date, default: Date.now }
});

const EmailUser = mongoose.model("EmailUser", emailUserSchema);

export default EmailUser;

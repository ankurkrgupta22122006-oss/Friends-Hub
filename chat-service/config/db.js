const mongoose = require('mongoose');

const connectDB = async () => {
    try {
        const connString = process.env.MONGODB_URI || 'mongodb://localhost:27017/friendshub_chat';
        console.log(`Connecting to MongoDB at: ${connString.includes('@') ? connString.split('@')[1] : connString}`);
        
        const conn = await mongoose.connect(connString);
        console.log(`[MongoDB Connected] Host: ${conn.connection.host}`);
    } catch (error) {
        console.error(`[MongoDB Connection Error]: ${error.message}`);
        console.warn('Fallback: Running in local memory mode for development if DB is unreachable.');
    }
};

module.exports = connectDB;

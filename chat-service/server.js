const express = require('express');
const http = require('http');
const cors = require('cors');
const dotenv = require('dotenv');
const connectDB = require('./config/db');

dotenv.config({ path: '../.env' });

const app = express();
const server = http.createServer(app);

// Connect to MongoDB Atlas / Local MongoDB
connectDB();

// Middleware
app.use(cors({ origin: '*' }));
app.use(express.json());

// Routes
app.use('/api/chat', require('./routes/chat.routes'));
app.use('/api/analytics', require('./routes/analytics.routes'));

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'UP',
        service: 'FriendsHub Node.js + MongoDB Microservice',
        database: 'MongoDB Atlas / Local',
        port: process.env.CHAT_SERVICE_PORT || 5000
    });
});

const PORT = process.env.CHAT_SERVICE_PORT || 5000;
server.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`⚡ FriendsHub Node.js + Express + MongoDB Service`);
    console.log(`🚀 Running on: http://localhost:${PORT}`);
    console.log(`====================================================`);
});

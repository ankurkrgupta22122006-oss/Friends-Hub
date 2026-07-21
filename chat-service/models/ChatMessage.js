const mongoose = require('mongoose');

const ChatMessageSchema = new mongoose.Schema({
    senderId: {
        type: String,
        required: true,
        index: true
    },
    senderName: {
        type: String,
        default: 'Anonymous'
    },
    receiverId: {
        type: String,
        required: true,
        index: true
    },
    message: {
        type: String,
        required: true,
        trim: true
    },
    readStatus: {
        type: Boolean,
        default: false
    },
    mediaUrl: {
        type: String,
        default: null
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('ChatMessage', ChatMessageSchema);

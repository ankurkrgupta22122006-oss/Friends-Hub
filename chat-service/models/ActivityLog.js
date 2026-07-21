const mongoose = require('mongoose');

const ActivityLogSchema = new mongoose.Schema({
    eventType: {
        type: String,
        required: true,
        enum: ['USER_LOGIN', 'USER_SIGNUP', 'POST_CREATED', 'MESSAGE_SENT', 'ADMIN_ACTION'],
        index: true
    },
    description: {
        type: String,
        required: true
    },
    userId: {
        type: String,
        default: 'SYSTEM'
    },
    username: {
        type: String,
        default: 'System User'
    },
    ipAddress: {
        type: String,
        default: '127.0.0.1'
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('ActivityLog', ActivityLogSchema);

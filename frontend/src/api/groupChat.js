import api from './axios';

// Group CRUD
export const createGroup = (name, groupImageUrl, memberIds, groupKeys) =>
    api.post('/chat/groups', { name, groupImageUrl, memberIds, groupKeys });

export const getUserGroups = () => api.get('/chat/groups');

export const getGroupMessages = (groupId) => api.get(`/chat/groups/${groupId}/messages`);

export const getGroupMembers = (groupId) => api.get(`/chat/groups/${groupId}/members`);

// Members
export const addGroupMember = (groupId, userId, groupKeys) =>
    api.post(`/chat/groups/${groupId}/members/add`, { userId, groupKeys });

export const removeGroupMember = (groupId, userId) =>
    api.post(`/chat/groups/${groupId}/members/remove`, { userId });

// Messaging (REST fallback, though WS is preferred)
export const sendGroupMessageRest = (groupId, content, imageUrl, iv) => 
    api.post(`/chat/groups/${groupId}/messages/send`, { content, imageUrl, iv });

import api from '../api/axios';

// Local helpers for base64 / ArrayBuffer conversion
function arrayBufferToBase64(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}

function base64ToArrayBuffer(base64) {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

// Helper for IndexedDB storage
function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('e2ee-keys', 1);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('identity')) {
                db.createObjectStore('identity');
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

function getKey(db, storeName, key) {
    return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, 'readonly');
        const req = tx.objectStore(storeName).get(key);
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
    });
}

function putKey(db, storeName, value, key) {
    return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, 'readwrite');
        const req = tx.objectStore(storeName).put(value, key);
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
    });
}

let identityPromise = null;

export async function getOrCreateIdentity() {
    if (identityPromise) {
        return identityPromise;
    }

    identityPromise = (async () => {
        const db = await openDB();
        let keyPair = await getKey(db, 'identity', 'keyPair');

        if (keyPair) {
            const rawPub = await crypto.subtle.exportKey('raw', keyPair.publicKey);
            const publicKeyB64 = arrayBufferToBase64(rawPub);
            return { keyPair, publicKeyB64 };
        }

        keyPair = await crypto.subtle.generateKey(
            { name: 'ECDH', namedCurve: 'P-256' },
            true,
            ['deriveKey']
        );

        await putKey(db, 'identity', keyPair, 'keyPair');
        const rawPub = await crypto.subtle.exportKey('raw', keyPair.publicKey);
        const publicKeyB64 = arrayBufferToBase64(rawPub);

        await api.put('/users/me/public-key', { publicKey: publicKeyB64 });
        return { keyPair, publicKeyB64 };
    })();

    try {
        return await identityPromise;
    } catch (err) {
        identityPromise = null;
        throw err;
    }
}

const publicKeyCache = new Map();
const inFlightPublicKeyRequests = new Map();

export async function getPublicKeyForUser(userId) {
    if (publicKeyCache.has(userId)) {
        return publicKeyCache.get(userId);
    }
    if (inFlightPublicKeyRequests.has(userId)) {
        return inFlightPublicKeyRequests.get(userId);
    }
    const requestPromise = (async () => {
        try {
            const res = await api.get(`/users/${userId}/public-key`);
            const pubKey = res.data ? res.data.publicKey : null;
            publicKeyCache.set(userId, pubKey);
            return pubKey;
        } finally {
            inFlightPublicKeyRequests.delete(userId);
        }
    })();
    inFlightPublicKeyRequests.set(userId, requestPromise);
    return requestPromise;
}

async function importPeerPublicKey(publicKeyB64) {
    return crypto.subtle.importKey(
        'raw',
        base64ToArrayBuffer(publicKeyB64),
        { name: 'ECDH', namedCurve: 'P-256' },
        false,
        []
    );
}

async function deriveSharedKey(myPrivateKey, peerPublicKey) {
    return crypto.subtle.deriveKey(
        { name: 'ECDH', public: peerPublicKey },
        myPrivateKey,
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt', 'decrypt']
    );
}

export async function encryptMessage(plaintext, receiverPublicKeyB64) {
    if (receiverPublicKeyB64 === null || receiverPublicKeyB64 === undefined) {
        throw new Error("Recipient has no public key");
    }
    const { keyPair } = await getOrCreateIdentity();
    const peerKey = await importPeerPublicKey(receiverPublicKeyB64);
    const sharedKey = await deriveSharedKey(keyPair.privateKey, peerKey);
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encryptedBuffer = await crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        sharedKey,
        new TextEncoder().encode(plaintext)
    );
    return {
        ciphertext: arrayBufferToBase64(encryptedBuffer),
        iv: arrayBufferToBase64(iv.buffer)
    };
}

export async function decryptMessage(ciphertextB64, ivB64, senderPublicKeyB64) {
    if (!ciphertextB64 || !ivB64 || !senderPublicKeyB64) {
        return "[Unable to decrypt this message]";
    }
    try {
        const { keyPair } = await getOrCreateIdentity();
        const peerKey = await importPeerPublicKey(senderPublicKeyB64);
        const sharedKey = await deriveSharedKey(keyPair.privateKey, peerKey);
        const decryptedBuffer = await crypto.subtle.decrypt(
            { name: 'AES-GCM', iv: base64ToArrayBuffer(ivB64) },
            sharedKey,
            base64ToArrayBuffer(ciphertextB64)
        );
        return new TextDecoder().decode(decryptedBuffer);
    } catch (err) {
        console.error("Decryption error:", err);
        return "[Unable to decrypt this message]";
    }
}

// ─── Group Chat E2EE Utilities ───────────────────────────────

export async function generateGroupKey() {
    const rawKey = crypto.getRandomValues(new Uint8Array(32)); // 256-bit AES key
    return arrayBufferToBase64(rawKey.buffer);
}

async function importGroupCryptoKey(groupKeyB64) {
    return crypto.subtle.importKey(
        'raw',
        base64ToArrayBuffer(groupKeyB64),
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt', 'decrypt']
    );
}

export async function encryptGroupMessage(plaintext, groupKeyB64) {
    if (!groupKeyB64 || !plaintext) {
        return { ciphertext: plaintext, iv: null };
    }
    try {
        const cryptoKey = await importGroupCryptoKey(groupKeyB64);
        const iv = crypto.getRandomValues(new Uint8Array(12));
        const encryptedBuffer = await crypto.subtle.encrypt(
            { name: 'AES-GCM', iv },
            cryptoKey,
            new TextEncoder().encode(plaintext)
        );
        return {
            ciphertext: arrayBufferToBase64(encryptedBuffer),
            iv: arrayBufferToBase64(iv.buffer)
        };
    } catch (err) {
        console.error("Group encryption error:", err);
        throw err;
    }
}

export async function decryptGroupMessage(ciphertextB64, ivB64, groupKeyB64) {
    if (!ciphertextB64) return "";
    if (!ivB64 || !groupKeyB64) return ciphertextB64; // Fallback for unencrypted legacy messages
    try {
        const cryptoKey = await importGroupCryptoKey(groupKeyB64);
        const decryptedBuffer = await crypto.subtle.decrypt(
            { name: 'AES-GCM', iv: base64ToArrayBuffer(ivB64) },
            cryptoKey,
            base64ToArrayBuffer(ciphertextB64)
        );
        return new TextDecoder().decode(decryptedBuffer);
    } catch (err) {
        console.error("Group decryption error:", err);
        return "[Unable to decrypt this message]";
    }
}


/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

function safeStorageGet(key) {
  try {
    return window.localStorage.getItem(key);
  } catch (error) {
    console.warn(`Unable to read localStorage key "${key}"`, error);
    return null;
  }
}

function safeStorageSet(key, value) {
  try {
    window.localStorage.setItem(key, value);
  } catch (error) {
    console.warn(`Unable to write localStorage key "${key}"`, error);
  }
}

function safeStorageRemove(key) {
  try {
    window.localStorage.removeItem(key);
  } catch (error) {
    console.warn(`Unable to remove localStorage key "${key}"`, error);
  }
}

function isJwtLike(token) {
  return typeof token === 'string' && token.split('.').length === 3;
}

function readStoredUser() {
  const token = safeStorageGet('token');
  const username = safeStorageGet('username');

  if (!token && !username) {
    return null;
  }

  if (!isJwtLike(token) || !username) {
    safeStorageRemove('token');
    safeStorageRemove('username');
    return null;
  }

  return { username, token };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const loading = false;

  const login = (token, username) => {
    safeStorageSet('token', token);
    safeStorageSet('username', username);
    setUser({ username, token });
  };

  const logout = () => {
    safeStorageRemove('token');
    safeStorageRemove('username');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
